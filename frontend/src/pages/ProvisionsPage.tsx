import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
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
import CalculateIcon from "@mui/icons-material/Calculate";
import DeleteIcon from "@mui/icons-material/Delete";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { lookupApi, periodApi, projectApi, provisionApi, provisionRuleApi } from "../api/resources";
import type { ProvisionRun } from "../api/types";

const money = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fmtDuration = (seconds?: number | null) => {
  const s = Number(seconds ?? 0);
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  const r = s % 60;
  return `${m}m ${r}s`;
};
const PROVISION_TYPES = [
  { code: "LEAVE", label: "Leave provision" },
  { code: "EOS", label: "End of service" },
  { code: "TICKET", label: "Ticket provision" },
  { code: "OTHER", label: "Other provision" },
];

export default function ProvisionsPage() {
  const qc = useQueryClient();
  const [periodId, setPeriodId] = useState("");
  const [projectId, setProjectId] = useState("");
  const [payGroup, setPayGroup] = useState("ALL");
  const [provisionType, setProvisionType] = useState("LEAVE");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [calcJobId, setCalcJobId] = useState<string | null>(null);
  const [calcMsg, setCalcMsg] = useState<string | null>(null);
  const [calcError, setCalcError] = useState<string | null>(null);

  const { data: periods = [] } = useQuery({ queryKey: ["periods"], queryFn: () => periodApi.list() });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: payGroups = [] } = useQuery({ queryKey: ["lookup", "PAY_STATUS"], queryFn: () => lookupApi.byCategory("PAY_STATUS") });
  const { data: runs = [] } = useQuery({
    queryKey: ["provisions", periodId],
    queryFn: () => provisionApi.list(periodId || undefined),
  });
  const { data: detail } = useQuery({
    queryKey: ["provision", selectedId],
    queryFn: () => provisionApi.get(selectedId!),
    enabled: !!selectedId,
  });

  useEffect(() => {
    if (!periodId && periods.length > 0) setPeriodId(periods[0].id ?? "");
  }, [periodId, periods]);

  const calculate = useMutation({
    mutationFn: () => provisionApi.startCalculate({
      periodId,
      provisionType,
      payGroup,
      ...(projectId ? { projectId } : {}),
    }),
    onSuccess: (job) => {
      setCalcError(null);
      setCalcJobId(job.id);
      setCalcMsg("Provision calculation is running in the background...");
    },
  });
  const { data: calcJob } = useQuery({
    queryKey: ["provisionCalculateJob", calcJobId],
    queryFn: () => provisionApi.getCalculateJob(calcJobId!),
    enabled: !!calcJobId,
    refetchInterval: (q) => q.state.data?.status === "RUNNING" ? 2000 : false,
  });
  useEffect(() => {
    if (!calcJob) return;
    if (calcJob.status === "RUNNING") {
      setCalcMsg(calcJob.message || `Calculating provisions... ${calcJob.done} / ${calcJob.total} in ${fmtDuration(calcJob.elapsedSeconds)}.`);
      return;
    }
    if (calcJob.status === "COMPLETED") {
      qc.invalidateQueries({ queryKey: ["provisions"] });
      const runId = typeof calcJob.result?.runId === "string" ? calcJob.result.runId : null;
      if (runId) setSelectedId(runId);
      setCalcMsg(`Provision calculated for ${calcJob.done} employee(s) in ${fmtDuration(calcJob.durationSeconds ?? calcJob.elapsedSeconds)}.`);
      setCalcError(null);
    } else {
      setCalcError(calcJob.message || "Provision calculation failed.");
    }
    setCalcJobId(null);
  }, [calcJob, qc]);

  const initRules = useMutation({
    mutationFn: provisionRuleApi.initializeDefaults,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["provisionRules"] });
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => provisionApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["provisions"] });
      setSelectedId(null);
    },
  });

  const selected = useMemo(
    () => detail ?? runs.find((r) => r.id === selectedId),
    [detail, runs, selectedId]
  );

  return (
    <Box>
      <Typography variant="h5" mb={2}>Provisions</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} md={3}>
            <TextField select fullWidth size="small" label="Period" value={periodId} onChange={(e) => setPeriodId(e.target.value)}>
              <MenuItem value="">Select period</MenuItem>
              {periods.map((p) => <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField select fullWidth size="small" label="Project" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              <MenuItem value="">Whole company</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4} md={2}>
            <TextField select fullWidth size="small" label="Pay group" value={payGroup} onChange={(e) => setPayGroup(e.target.value)}>
              <MenuItem value="ALL">All</MenuItem>
              {payGroups.map((g) => <MenuItem key={g.code} value={g.code}>{g.label || g.code}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4} md={2}>
            <TextField select fullWidth size="small" label="Type" value={provisionType} onChange={(e) => setProvisionType(e.target.value)}>
              {PROVISION_TYPES.map((t) => <MenuItem key={t.code} value={t.code}>{t.label}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4} md={2}>
            <Button
              fullWidth
              variant="contained"
              startIcon={<CalculateIcon />}
              disabled={!periodId || calculate.isPending || calcJob?.status === "RUNNING"}
              onClick={() => calculate.mutate()}
            >
              {calculate.isPending || calcJob?.status === "RUNNING" ? "Calculating..." : "Calculate"}
            </Button>
          </Grid>
          <Grid item xs={12}>
            <Button
              variant="outlined"
              startIcon={<RestartAltIcon />}
              disabled={initRules.isPending}
              onClick={() => initRules.mutate()}
            >
              Initialize default templates
            </Button>
          </Grid>
        </Grid>
        <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1.25 }}>
          Provision is an accounting accrual preview. It does not change payroll net pay.
        </Typography>
        {calcMsg && (
          <Alert severity="info" sx={{ mt: 1.5 }} onClose={() => setCalcMsg(null)}>
            {calcMsg}
          </Alert>
        )}
        {(calculate.isError || initRules.isError || calcError) && (
          <Alert severity="error" sx={{ mt: 1.5 }}>
            {calcError ?? ((calculate.error || initRules.error) as any)?.response?.data?.message ?? "Provision action failed."}
          </Alert>
        )}
        {initRules.isSuccess && (
          <Alert severity="success" sx={{ mt: 1.5 }}>
            Default provision templates are ready. You can edit them from Payroll - Provision Rules.
          </Alert>
        )}
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, mb: 2, overflow: "auto" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Period</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Pay group</TableCell>
              <TableCell>Project</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Employees</TableCell>
              <TableCell align="right">Eligible amount</TableCell>
              <TableCell align="right">Provision amount</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {runs.map((run) => (
              <TableRow key={run.id} hover selected={run.id === selectedId}>
                <TableCell>{run.periodName}</TableCell>
                <TableCell>{run.provisionType}</TableCell>
                <TableCell>{run.payGroup}</TableCell>
                <TableCell>{projects.find((p) => p.id === run.projectId)?.code ?? "All"}</TableCell>
                <TableCell><Chip size="small" color="info" label={run.status ?? "CALCULATED"} /></TableCell>
                <TableCell align="right">{run.employeeCount}</TableCell>
                <TableCell align="right">{money(run.totalEligibleAmount)}</TableCell>
                <TableCell align="right">{money(run.totalProvisionAmount)}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setSelectedId(run.id ?? null)}>Open</Button>
                  {run.id && (
                    <Button size="small" color="error" startIcon={<DeleteIcon />} onClick={() => remove.mutate(run.id!)}>
                      Delete
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {runs.length === 0 && (
              <TableRow>
                <TableCell colSpan={9}>
                  <Typography variant="body2" color="text.secondary">No provision runs yet.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      {selected && <ProvisionDetail run={selected} projectName={(id) => projects.find((p) => p.id === id)?.code ?? ""} />}
    </Box>
  );
}

function ProvisionDetail({ run, projectName }: { run: ProvisionRun; projectName: (id?: string) => string }) {
  return (
    <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
      <Box sx={{ p: 2, borderBottom: "1px solid", borderColor: "divider" }}>
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" gap={1}>
          <Box>
            <Typography variant="subtitle1" fontWeight={800}>{run.provisionType} - {run.periodName}</Typography>
            <Typography variant="caption" color="text.secondary">{run.notes}</Typography>
          </Box>
          <Stack direction="row" spacing={3}>
            <Total label="Employees" value={run.employeeCount} plain />
            <Total label="Eligible" value={run.totalEligibleAmount} />
            <Total label="Provision" value={run.totalProvisionAmount} />
          </Stack>
        </Stack>
      </Box>
      <Box sx={{ overflow: "auto" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Employee</TableCell>
              <TableCell>Project</TableCell>
              <TableCell>Pay group</TableCell>
              <TableCell align="right">Eligible amount</TableCell>
              <TableCell align="right">Provision amount</TableCell>
              <TableCell>Formula</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {run.results.map((row) => (
              <TableRow key={row.id ?? row.employeeId} hover>
                <TableCell>
                  <Typography variant="body2" fontWeight={700}>{row.employeeNumber}</Typography>
                  <Typography variant="caption" color="text.secondary">{row.employeeName}</Typography>
                </TableCell>
                <TableCell>{projectName(row.projectId)}</TableCell>
                <TableCell>{row.payGroup}</TableCell>
                <TableCell align="right">{money(row.eligibleAmount)}</TableCell>
                <TableCell align="right">{money(row.provisionAmount)}</TableCell>
                <TableCell>{row.formulaNote}</TableCell>
                <TableCell>{row.status}{row.message ? ` - ${row.message}` : ""}</TableCell>
              </TableRow>
            ))}
            {run.results.length === 0 && (
              <TableRow>
                <TableCell colSpan={7}>
                  <Typography variant="body2" color="text.secondary">Open a run to load employee details.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Box>
    </Paper>
  );
}

function Total({ label, value, plain = false }: { label: string; value: number; plain?: boolean }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography fontWeight={800}>{plain ? value : money(value)}</Typography>
    </Box>
  );
}
