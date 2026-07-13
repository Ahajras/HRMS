import { Fragment, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControlLabel,
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
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { lookupApi, payrollRuleApi, projectApi } from "../api/resources";
import type { PayrollCategoryRule, PayrollRule } from "../api/types";

const BASIS = [
  { value: "FIXED_AMOUNT", label: "Fixed amount" },
  { value: "DAILY_RATE", label: "Daily rate x payable days" },
];

const QUANTITY_SOURCE = [
  { value: "ACTUAL_WORKED", label: "Actual worked" },
  { value: "PLANNED_SHIFT", label: "Planned shift" },
  { value: "PAYABLE_SCHEDULE", label: "Payable schedule" },
];

const CATEGORY_BASIS = [
  { value: "FULL_MONTH", label: "Full divisor" },
  { value: "ACTUAL_PAYABLE", label: "Actual payable" },
  { value: "FIXED_AMOUNT", label: "Fixed amount" },
];

const CATEGORY_DIVISOR_MODE = [
  { value: "INHERIT", label: "Use rule divisor" },
  { value: "FIXED", label: "Fixed value" },
  { value: "ACTUAL_MONTH", label: "By month days" },
];

const DEFAULT_CATEGORY_RULES: PayrollCategoryRule[] = [
  { category: "ALLOWANCE", basis: "ACTUAL_PAYABLE", divisorMode: "INHERIT", monthDivisor: null },
  { category: "SALARY", basis: "ACTUAL_PAYABLE", divisorMode: "INHERIT", monthDivisor: null },
];

const MONTHLY_CATEGORY_RULES: PayrollCategoryRule[] = [
  { category: "ALLOWANCE", basis: "FIXED_AMOUNT", divisorMode: "FIXED", monthDivisor: 30 },
  { category: "SALARY", basis: "FIXED_AMOUNT", divisorMode: "FIXED", monthDivisor: 30 },
];

function defaultRuleFor(projectId: string, payGroup: string): PayrollRule {
  if (payGroup === "DAILY") {
    return {
      payGroup,
      projectId,
      payItemBasis: "DAILY_RATE",
      quantitySource: "ACTUAL_WORKED",
      otMultiplier: 1.25,
      restDayOtMultiplier: 1.5,
      standardHoursPerDay: 8,
      weeklyRestPaid: false,
      monthDivisor: 30,
      divisorMode: "FIXED",
      dayZeroCutoffDay: null,
      status: "ACTIVE",
      categoryRules: DEFAULT_CATEGORY_RULES,
    };
  }
  if (payGroup === "MONTHLY") {
    return {
      payGroup,
      projectId,
      payItemBasis: "FIXED_AMOUNT",
      quantitySource: "PAYABLE_SCHEDULE",
      otMultiplier: 1.25,
      restDayOtMultiplier: 1.5,
      standardHoursPerDay: 8,
      weeklyRestPaid: true,
      monthDivisor: 30,
      divisorMode: "FIXED",
      dayZeroCutoffDay: 22,
      status: "ACTIVE",
      categoryRules: MONTHLY_CATEGORY_RULES,
    };
  }
  return {
    payGroup,
    projectId,
    payItemBasis: "FIXED_AMOUNT",
    quantitySource: "PAYABLE_SCHEDULE",
    otMultiplier: 1.25,
    restDayOtMultiplier: 1.5,
    standardHoursPerDay: 8,
    weeklyRestPaid: true,
    monthDivisor: 30,
    divisorMode: "FIXED",
    dayZeroCutoffDay: null,
    status: "ACTIVE",
    categoryRules: MONTHLY_CATEGORY_RULES,
  };
}

function formulaFor(rule: PayrollRule) {
  if (rule.payItemBasis === "DAILY_RATE") {
    return "Daily pay = quantity from the selected source x daily rate. Hourly rate = daily rate / employee shift hours. Overtime = hourly rate x OT hours x OT multiplier.";
  }
  return "Fixed pay = configured component amount using this rule's category/divisor settings. Time type rules use the selected quantity source for deductions or variable pay.";
}

function categoryRulesFor(rule: PayrollRule): PayrollCategoryRule[] {
  const byCategory = new Map<string, PayrollCategoryRule>();
  DEFAULT_CATEGORY_RULES.forEach((r) => byCategory.set(r.category, { ...r }));
  (rule.categoryRules ?? []).forEach((r) => {
    const category = (r.category || "").trim().toUpperCase();
    if (category) byCategory.set(category, { ...r, category });
  });
  return Array.from(byCategory.values()).sort((a, b) => a.category.localeCompare(b.category));
}

export default function PayrollRulesPage() {
  const qc = useQueryClient();
  const { data: rules = [] } = useQuery({ queryKey: ["payrollRules"], queryFn: payrollRuleApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: payGroups = [] } = useQuery({ queryKey: ["lookup", "PAY_STATUS"], queryFn: () => lookupApi.byCategory("PAY_STATUS") });
  const [drafts, setDrafts] = useState<Record<string, PayrollRule>>({});
  const [projectId, setProjectId] = useState<string>("");

  const createRule = useMutation({
    mutationFn: (payload: PayrollRule) => payrollRuleApi.create(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["payrollRules"] }),
  });

  const shownRules = projectId ? rules.filter((r) => r.projectId === projectId) : [];
  const payGroupLabel = (code: string) => payGroups.find((g) => g.code === code)?.label || code;
  const missingGroups = projectId
    ? payGroups.map((g) => g.code).filter((g) => !shownRules.some((r) => r.payGroup === g))
    : [];
  const cloneForProject = (payGroup: string) => {
    const base = rules.find((r) => !r.projectId && r.payGroup === payGroup);
    if (!base) {
      createRule.mutate(defaultRuleFor(projectId, payGroup));
      return;
    }
    const { id, ...rest } = base;
    createRule.mutate({ ...(rest as PayrollRule), projectId, payGroup });
  };

  const save = useMutation({
    mutationFn: (rule: PayrollRule) => payrollRuleApi.update(rule.id!, rule),
    onSuccess: () => {
      setDrafts({});
      qc.invalidateQueries({ queryKey: ["payrollRules"] });
    },
  });

  const value = (rule: PayrollRule) => drafts[rule.id ?? ""] ?? rule;
  const set = (rule: PayrollRule, patch: Partial<PayrollRule>) => {
    setDrafts((d) => ({ ...d, [rule.id ?? ""]: { ...value(rule), ...patch } }));
  };
  const setCategoryRule = (rule: PayrollRule, category: string, patch: Partial<PayrollCategoryRule>) => {
    const row = value(rule);
    const next = categoryRulesFor(row).map((r) => (
      r.category === category ? { ...r, ...patch, category } : r
    ));
    set(rule, { categoryRules: next });
  };

  return (
    <Box>
      <Typography variant="h5" mb={2}>Payroll Rules</Typography>
      <Stack direction="row" spacing={1.5} mb={2} alignItems="center">
        <TextField
          select
          size="small"
          label="Project"
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
          sx={{ minWidth: 260 }}
        >
          <MenuItem value="">Select project</MenuItem>
          {projects.map((p: { id?: string; code?: string; name?: string }) => (
            <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>
          ))}
        </TextField>
        {projectId && missingGroups.map((g) => (
          <Button key={g} variant="contained" disabled={createRule.isPending} onClick={() => cloneForProject(g)}>
            Add {payGroupLabel(g)} rule
          </Button>
        ))}
      </Stack>

      {!projectId ? (
        <Alert severity="info">Select a project to manage its payroll rules and calculation formulas.</Alert>
      ) : shownRules.length === 0 ? (
        <Alert severity="info">This project uses the default payroll structure. Click the button above to give it its own settings.</Alert>
      ) : (
        <Paper variant="outlined" sx={{ borderRadius: 2 }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Pay group</TableCell>
                <TableCell>Pay item basis</TableCell>
                <TableCell>Quantity source</TableCell>
                <TableCell>Weekly rest paid</TableCell>
                <TableCell align="right">OT multiplier</TableCell>
                <TableCell align="right">Rest day OT</TableCell>
                <TableCell align="right">Month divisor</TableCell>
                <TableCell>Divisor mode</TableCell>
                <TableCell align="right">Day Zero cutoff</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {shownRules.map((rule) => {
                const row = value(rule);
                return (
                  <Fragment key={rule.id}>
                    <TableRow>
                      <TableCell>{rule.payGroup}</TableCell>
                      <TableCell>
                        <TextField select size="small" fullWidth value={row.payItemBasis} onChange={(e) => set(rule, { payItemBasis: e.target.value })}>
                          {BASIS.map((b) => <MenuItem key={b.value} value={b.value}>{b.label}</MenuItem>)}
                        </TextField>
                      </TableCell>
                      <TableCell>
                        <TextField select size="small" fullWidth value={row.quantitySource ?? "PAYABLE_SCHEDULE"} onChange={(e) => set(rule, { quantitySource: e.target.value })}>
                          {QUANTITY_SOURCE.map((s) => <MenuItem key={s.value} value={s.value}>{s.label}</MenuItem>)}
                        </TextField>
                      </TableCell>
                      <TableCell>
                        <FormControlLabel
                          control={<Checkbox checked={!!row.weeklyRestPaid} onChange={(e) => set(rule, { weeklyRestPaid: e.target.checked })} />}
                          label="Paid"
                        />
                      </TableCell>
                      <TableCell align="right">
                        <TextField size="small" type="number" value={row.otMultiplier} onChange={(e) => set(rule, { otMultiplier: Number(e.target.value) })} inputProps={{ step: "0.01" }} />
                      </TableCell>
                      <TableCell align="right">
                        <TextField size="small" type="number" value={row.restDayOtMultiplier} onChange={(e) => set(rule, { restDayOtMultiplier: Number(e.target.value) })} inputProps={{ step: "0.01" }} />
                      </TableCell>
                      <TableCell align="right">
                        <TextField size="small" type="number" value={row.monthDivisor} onChange={(e) => set(rule, { monthDivisor: Number(e.target.value) })} inputProps={{ step: "1" }} />
                      </TableCell>
                      <TableCell>
                        <TextField select size="small" fullWidth value={row.divisorMode ?? "FIXED"} onChange={(e) => set(rule, { divisorMode: e.target.value })}>
                          <MenuItem value="FIXED">Fixed value</MenuItem>
                          <MenuItem value="ACTUAL_MONTH">By month days</MenuItem>
                        </TextField>
                      </TableCell>
                      <TableCell align="right">
                        <TextField
                          size="small"
                          type="number"
                          placeholder="None"
                          value={row.dayZeroCutoffDay ?? ""}
                          onChange={(e) => set(rule, { dayZeroCutoffDay: e.target.value === "" ? null : Number(e.target.value) })}
                          inputProps={{ step: "1", min: "1", max: "31" }}
                          sx={{ width: 90 }}
                        />
                      </TableCell>
                      <TableCell align="right">
                        <Button size="small" variant="contained" disabled={!drafts[rule.id ?? ""] || save.isPending} onClick={() => save.mutate(row)}>Save</Button>
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell colSpan={10} sx={{ bgcolor: "action.hover" }}>
                        <Typography variant="body2" color="text.secondary" mb={1}>
                          <b>{rule.payGroup} formula:</b> {formulaFor(row)} Shift hours come from the employee's assigned shift on the timesheet.
                        </Typography>
                        {row.dayZeroCutoffDay != null && (
                          <Typography variant="body2" color="text.secondary" mb={1}>
                            <b>Day Zero:</b> when this project is locked, any day after day {row.dayZeroCutoffDay} of the month is marked
                            as estimated. If something changes it later (e.g. an approved leave), the whole month is recalculated with the
                            correction and compared to what was actually paid — the difference lands on the employee's next payslip
                            instead of reopening this locked period.
                          </Typography>
                        )}
                        <Table size="small" sx={{ bgcolor: "background.paper" }}>
                          <TableHead>
                            <TableRow>
                              <TableCell>Component category</TableCell>
                              <TableCell>Pay basis</TableCell>
                              <TableCell>Category divisor mode</TableCell>
                              <TableCell align="right">Category divisor</TableCell>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {categoryRulesFor(row).map((categoryRule) => (
                              <TableRow key={categoryRule.category}>
                                <TableCell sx={{ fontWeight: 700 }}>{categoryRule.category}</TableCell>
                                <TableCell>
                                  <TextField
                                    select
                                    size="small"
                                    fullWidth
                                    value={categoryRule.basis ?? "ACTUAL_PAYABLE"}
                                    onChange={(e) => setCategoryRule(rule, categoryRule.category, { basis: e.target.value })}
                                  >
                                    {CATEGORY_BASIS.map((b) => <MenuItem key={b.value} value={b.value}>{b.label}</MenuItem>)}
                                  </TextField>
                                </TableCell>
                                <TableCell>
                                  <TextField
                                    select
                                    size="small"
                                    fullWidth
                                    value={categoryRule.divisorMode ?? "INHERIT"}
                                    onChange={(e) => setCategoryRule(rule, categoryRule.category, { divisorMode: e.target.value })}
                                  >
                                    {CATEGORY_DIVISOR_MODE.map((m) => <MenuItem key={m.value} value={m.value}>{m.label}</MenuItem>)}
                                  </TextField>
                                </TableCell>
                                <TableCell align="right">
                                  <TextField
                                    size="small"
                                    type="number"
                                    value={categoryRule.monthDivisor ?? ""}
                                    disabled={(categoryRule.divisorMode ?? "INHERIT") !== "FIXED"}
                                    onChange={(e) => setCategoryRule(rule, categoryRule.category, {
                                      monthDivisor: e.target.value === "" ? null : Number(e.target.value),
                                    })}
                                    inputProps={{ step: "1" }}
                                  />
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableCell>
                    </TableRow>
                  </Fragment>
                );
              })}
            </TableBody>
          </Table>
        </Paper>
      )}

      {save.isError && (
        <Alert severity="error" sx={{ mt: 2 }}>{(save.error as any)?.response?.data?.message ?? "Could not save payroll rule."}</Alert>
      )}
      <Stack mt={2}>
        <Typography variant="body2" color="text.secondary">
          Changes apply when you calculate or recalculate a payroll run.
        </Typography>
      </Stack>
    </Box>
  );
}
