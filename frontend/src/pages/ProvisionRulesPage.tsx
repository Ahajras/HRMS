import { useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Grid,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/Delete";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { lookupApi, projectApi, provisionRuleApi } from "../api/resources";
import type { ProvisionRule } from "../api/types";

const TYPES = ["LEAVE", "EOS", "TICKET", "OTHER"];
const BASIS_MODES = [
  { code: "COMPONENT_FLAGS", label: "Pay component flags" },
  { code: "COMPONENT_CATEGORIES", label: "Component categories" },
  { code: "COMPONENT_CODES", label: "Component codes" },
  { code: "FIXED_AMOUNT", label: "Fixed amount" },
];
const VARIABLES = [
  "basis_amount",
  "fixed_amount",
  "divisor",
  "entitlement_days",
  "service_years",
  "service_months",
  "period_days",
  "month_days",
  "ticket_cycle_months",
  "ticket_amount",
];

const emptyRule = (): ProvisionRule => ({
  provisionType: "LEAVE",
  name: "Annual leave provision",
  payGroup: "ALL",
  basisMode: "COMPONENT_FLAGS",
  formulaExpression: "basis_amount / divisor * entitlement_days / 12",
  divisor: 365,
  fixedAmount: 0,
  entitlementDaysUnderFive: 21,
  entitlementDaysFiveOrMore: 28,
  ticketCycleMonths: 12,
  effectiveFrom: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
});

export default function ProvisionRulesPage() {
  const qc = useQueryClient();
  const [form, setForm] = useState<ProvisionRule>(emptyRule());
  const [variable, setVariable] = useState("basis_amount");

  const { data: rules = [] } = useQuery({ queryKey: ["provisionRules"], queryFn: provisionRuleApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: payGroups = [] } = useQuery({ queryKey: ["lookup", "PAY_STATUS"], queryFn: () => lookupApi.byCategory("PAY_STATUS") });

  const save = useMutation({
    mutationFn: provisionRuleApi.save,
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ["provisionRules"] });
      setForm(saved);
    },
  });
  const init = useMutation({
    mutationFn: provisionRuleApi.initializeDefaults,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["provisionRules"] }),
  });
  const remove = useMutation({
    mutationFn: provisionRuleApi.delete,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["provisionRules"] });
      setForm(emptyRule());
    },
  });

  const projectLabel = useMemo(() => new Map(projects.map((p) => [p.id, `${p.code} - ${p.name}`])), [projects]);
  const set = <K extends keyof ProvisionRule>(key: K, value: ProvisionRule[K]) => setForm((f) => ({ ...f, [key]: value }));
  const insertVariable = () => {
    const current = form.formulaExpression || "";
    set("formulaExpression", `${current}${current.endsWith(" ") || current.length === 0 ? "" : " "}${variable}`);
  };

  return (
    <Box>
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1} mb={2}>
        <Typography variant="h5">Provision Rules</Typography>
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" startIcon={<RestartAltIcon />} onClick={() => init.mutate()} disabled={init.isPending}>
            Initialize default templates
          </Button>
          <Button variant="outlined" startIcon={<AddIcon />} onClick={() => setForm(emptyRule())}>New rule</Button>
        </Stack>
      </Stack>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5}>
          <Grid item xs={12} md={3}>
            <TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => set("name", e.target.value)} />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField select fullWidth size="small" label="Type" value={form.provisionType} onChange={(e) => set("provisionType", e.target.value)}>
              {TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField select fullWidth size="small" label="Project" value={form.projectId ?? ""} onChange={(e) => set("projectId", e.target.value || undefined)}>
              <MenuItem value="">All projects</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <TextField select fullWidth size="small" label="Pay group" value={form.payGroup} onChange={(e) => set("payGroup", e.target.value)}>
              <MenuItem value="ALL">All</MenuItem>
              {payGroups.map((g) => <MenuItem key={g.code} value={g.code}>{g.label || g.code}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <TextField select fullWidth size="small" label="Basis mode" value={form.basisMode} onChange={(e) => set("basisMode", e.target.value)}>
              {BASIS_MODES.map((m) => <MenuItem key={m.code} value={m.code}>{m.label}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              size="small"
              label="Basis categories"
              value={form.basisCategories ?? ""}
              onChange={(e) => set("basisCategories", e.target.value)}
              helperText="Comma-separated, example: SALARY,ALLOWANCE"
              disabled={form.basisMode !== "COMPONENT_CATEGORIES"}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              size="small"
              label="Basis component codes"
              value={form.basisComponentCodes ?? ""}
              onChange={(e) => set("basisComponentCodes", e.target.value)}
              helperText="Comma-separated, example: 01,02,43"
              disabled={form.basisMode !== "COMPONENT_CODES"}
            />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="number" label="Divisor" value={form.divisor} onChange={(e) => set("divisor", Number(e.target.value))} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="number" label="Fixed amount" value={form.fixedAmount} onChange={(e) => set("fixedAmount", Number(e.target.value))} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="number" label="Days < 5 years" value={form.entitlementDaysUnderFive} onChange={(e) => set("entitlementDaysUnderFive", Number(e.target.value))} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="number" label="Days >= 5 years" value={form.entitlementDaysFiveOrMore} onChange={(e) => set("entitlementDaysFiveOrMore", Number(e.target.value))} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="number" label="Ticket cycle months" value={form.ticketCycleMonths} onChange={(e) => set("ticketCycleMonths", Number(e.target.value))} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="date" label="Effective from" value={form.effectiveFrom} onChange={(e) => set("effectiveFrom", e.target.value)} InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={12}>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={1} mb={1}>
              <TextField select size="small" label="Variable" value={variable} onChange={(e) => setVariable(e.target.value)} sx={{ minWidth: 240 }}>
                {VARIABLES.map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}
              </TextField>
              <Button variant="outlined" onClick={insertVariable}>Insert variable</Button>
            </Stack>
            <TextField
              fullWidth
              multiline
              minRows={2}
              label="Formula"
              value={form.formulaExpression}
              onChange={(e) => set("formulaExpression", e.target.value)}
              helperText="Supports + - * / and parentheses. Use variables from the dropdown."
            />
          </Grid>
          <Grid item xs={12}>
            <TextField fullWidth size="small" label="Notes" value={form.notes ?? ""} onChange={(e) => set("notes", e.target.value)} />
          </Grid>
          <Grid item xs={12}>
            <Stack direction="row" spacing={1}>
              <Button variant="contained" startIcon={<SaveIcon />} disabled={save.isPending} onClick={() => save.mutate(form)}>
                Save rule
              </Button>
              {form.id && (
                <Button color="error" startIcon={<DeleteIcon />} disabled={remove.isPending} onClick={() => remove.mutate(form.id!)}>
                  Delete
                </Button>
              )}
            </Stack>
          </Grid>
        </Grid>
        {(save.isError || init.isError || remove.isError) && (
          <Alert severity="error" sx={{ mt: 1.5 }}>
            {((save.error || init.error || remove.error) as any)?.response?.data?.message ?? "Provision rule action failed."}
          </Alert>
        )}
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Project</TableCell>
              <TableCell>Pay group</TableCell>
              <TableCell>Basis</TableCell>
              <TableCell>Formula</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rules.map((rule) => (
              <TableRow key={rule.id} hover selected={rule.id === form.id} onClick={() => setForm(rule)} sx={{ cursor: "pointer" }}>
                <TableCell>{rule.name}</TableCell>
                <TableCell>{rule.provisionType}</TableCell>
                <TableCell>{projectLabel.get(rule.projectId) ?? "All"}</TableCell>
                <TableCell>{rule.payGroup}</TableCell>
                <TableCell>{rule.basisMode}</TableCell>
                <TableCell>{rule.formulaExpression}</TableCell>
                <TableCell>{rule.status}</TableCell>
              </TableRow>
            ))}
            {rules.length === 0 && (
              <TableRow>
                <TableCell colSpan={7}>
                  <Typography variant="body2" color="text.secondary">No provision rules yet. Initialize default templates or create a rule.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}
