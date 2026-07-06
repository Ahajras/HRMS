import { Fragment, useEffect, useState } from "react";
import {
  Alert,
  Autocomplete,
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
import PrintIcon from "@mui/icons-material/Print";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { costCodeApi, crewApi, periodApi, periodLockApi, projectApi, shiftApi, timeTypeApi, timesheetApi } from "../api/resources";
import type { Timesheet, TimesheetDay, TimesheetDayCost } from "../api/types";

const STATUS_COLOR: Record<string, "default" | "info" | "success" | "warning"> = {
  DRAFT: "default",
  SUBMITTED: "info",
  APPROVED: "success",
  LOCKED: "warning",
};

const PERIOD_COLOR: Record<string, "success" | "warning" | "error"> = {
  OPEN: "success",
  LOCKED: "warning",
  CLOSED: "error",
};

const WEEKDAY = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
function fmtDay(iso: string): string {
  const d = new Date(iso + "T00:00:00");
  return isNaN(d.getTime()) ? iso : `${iso} · ${WEEKDAY[d.getDay()]}`;
}

function fmtDuration(seconds?: number): string {
  const s = Math.max(0, Math.floor(seconds ?? 0));
  const m = Math.floor(s / 60);
  const rem = s % 60;
  return m ? `${m}m ${rem}s` : `${rem}s`;
}

function printTimesheet(
  timesheet: Timesheet,
  summary: any,
  days: TimesheetDay[],
  projects: { id?: string; code: string; name: string }[],
  costCodes: { id?: string; code: string; name: string; projectId: string }[],
  timeTypes: { id?: string; code: string; name: string }[],
) {
  const projectLabel = (id?: string) => projects.find((p) => p.id === id)?.code ?? "";
  const costLabel = (id?: string) => {
    const cc = costCodes.find((c) => c.id === id);
    return cc ? `${cc.code} - ${cc.name}` : "";
  };
  const timeTypeLabel = (day: TimesheetDay) => {
    if (day.timeTypeCode) return day.timeTypeCode;
    const type = timeTypes.find((t) => t.id === day.timeTypeId);
    return type ? `${type.code} - ${type.name}` : "";
  };
  const html = `
    <html>
      <head>
        <title>Timesheet ${escapeHtml(timesheet.employeeNumber ?? "")}</title>
        <style>
          body { font-family: Arial, sans-serif; margin: 24px; color: #111827; }
          h1,h2,h3,p { margin: 0; }
          .header { display:flex; justify-content:space-between; margin-bottom:18px; }
          .muted { color:#6b7280; font-size:12px; }
          .grid { display:grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin: 18px 0; }
          .card { border:1px solid #d1d5db; border-radius:8px; padding:10px; }
          table { width:100%; border-collapse: collapse; margin-top:10px; }
          th, td { border:1px solid #d1d5db; padding:7px; text-align:left; vertical-align:top; font-size:12px; }
          th { background:#f3f4f6; }
          .section { margin-top: 18px; }
          .right { text-align:right; }
          .chip { display:inline-block; border:1px solid #d1d5db; border-radius:999px; padding:4px 8px; margin:3px 6px 0 0; font-size:12px; }
        </style>
      </head>
      <body>
        <div class="header">
          <div>
            <h1>Timesheet</h1>
            <p>${escapeHtml(timesheet.employeeNumber ?? "")} - ${escapeHtml(timesheet.employeeName ?? "")}</p>
            <p class="muted">${timesheet.periodYear}/${String(timesheet.periodMonth).padStart(2, "0")} - ${escapeHtml(timesheet.status ?? "")}</p>
          </div>
        </div>
        ${summary ? `
          <div class="section">
            <h2>Summary</h2>
            <div class="grid">
              <div class="card"><div class="muted">Normal</div><strong>${summary.normalHours}h</strong></div>
              <div class="card"><div class="muted">Weekend</div><strong>${summary.restHours ?? 0}h</strong></div>
              <div class="card"><div class="muted">Holiday</div><strong>${summary.holidayHours ?? 0}h</strong></div>
              <div class="card"><div class="muted">Overtime</div><strong>${summary.overtimeHours}h</strong></div>
              <div class="card"><div class="muted">Worked days</div><strong>${summary.workedDays}</strong></div>
              <div class="card"><div class="muted">Absence</div><strong>${summary.absenceDays}d · ${summary.absenceHours}h</strong></div>
              <div class="card"><div class="muted">Leave</div><strong>${summary.leaveDays}d · ${summary.leaveHours}h</strong></div>
              <div class="card"><div class="muted">Payable hours</div><strong>${summary.workedHours}h</strong></div>
            </div>
            <div>
              ${(summary.lines ?? []).map((line: any) => `<span class="chip">${escapeHtml(line.category)}: ${line.days}d · ${line.hours}h${line.paid ? "" : " (unpaid)"}</span>`).join("")}
            </div>
          </div>
        ` : ""}
        <div class="section">
          <h2>Days</h2>
          <table>
            <thead>
              <tr>
                <th>Date</th><th>Type</th><th>In</th><th>Out</th><th class="right">Planned</th><th class="right">Worked</th><th class="right">Normal</th><th class="right">OT</th><th>Project</th><th>Cost code</th><th>Remarks</th>
              </tr>
            </thead>
            <tbody>
              ${days.map((d) => `
                <tr>
                  <td>${escapeHtml(fmtDay(d.workDate))}</td>
                  <td>${escapeHtml(timeTypeLabel(d))}</td>
                  <td>${escapeHtml(d.actualIn ?? "")}</td>
                  <td>${escapeHtml(d.actualOut ?? "")}</td>
                  <td class="right">${d.plannedHours ?? 0}</td>
                  <td class="right">${d.workedHours ?? 0}</td>
                  <td class="right">${d.normalHours ?? 0}</td>
                  <td class="right">${d.otHours ?? 0}</td>
                  <td>${escapeHtml(projectLabel(d.projectId))}</td>
                  <td>${escapeHtml(costLabel(d.costCodeId))}</td>
                  <td>${escapeHtml(d.remarks ?? "")}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      </body>
    </html>
  `;
  openPrintWindow(html);
}

function openPrintWindow(html: string) {
  const win = window.open("", "_blank", "width=1100,height=900");
  if (!win) return;
  win.document.open();
  win.document.write(html);
  win.document.close();
  win.focus();
  win.onload = () => win.print();
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export default function TimesheetPage() {
  const qc = useQueryClient();
  const [periodId, setPeriodId] = useState("");
  const [projectId, setProjectId] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [q, setQ] = useState("");
  const pageSize = 50;

  // Generate form
  const [genEmployee, setGenEmployee] = useState("");
  const [genShift, setGenShift] = useState("");
  const [overwrite, setOverwrite] = useState(false);

  const { data: employees = [] } = useQuery({
    queryKey: ["timesheetEligibleEmployees", periodId, projectId],
    queryFn: () => timesheetApi.eligibleEmployees(periodId),
    enabled: !!periodId && !!projectId,
  });
  const { data: shifts = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });
  const { data: timeTypes = [] } = useQuery({ queryKey: ["timeTypes"], queryFn: timeTypeApi.list });
  const { data: periods = [] } = useQuery({ queryKey: ["periods"], queryFn: () => periodApi.list() });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: allCrews = [] } = useQuery({ queryKey: ["crews"], queryFn: crewApi.list });
  const [genCrew, setGenCrew] = useState("");

  const period = periods.find((p) => p.id === periodId);
  const employeeOptions = employees.filter((e) => !projectId || e.projectId === projectId);
  const selectedEmployee = employeeOptions.find((e) => e.id === genEmployee);
  const availableShifts = shifts.filter((s) => !selectedEmployee?.projectId || !s.projectId || s.projectId === selectedEmployee.projectId);
  const { data: list } = useQuery({
    queryKey: ["timesheets", periodId, projectId, page, pageSize, q],
    queryFn: () => timesheetApi.listByPeriod(period!.periodYear, period!.periodMonth, projectId || undefined, page, pageSize, q),
    enabled: !!period && !!projectId,
  });
  const rows = list?.content ?? [];
  const { data: timesheetProjectSummary = [] } = useQuery({
    queryKey: ["timesheetProjectSummary", period?.periodYear, period?.periodMonth, projectId],
    queryFn: () => timesheetApi.projectSummary(period!.periodYear, period!.periodMonth, projectId || undefined),
    enabled: !!period,
  });
  const selectedSummary = projectId
    ? timesheetProjectSummary.find((r) => r.projectId === projectId)
    : undefined;
  const summaryTotals = timesheetProjectSummary.reduce((acc, row) => ({
    eligible: acc.eligible + row.eligible,
    generated: acc.generated + row.generated,
    missing: acc.missing + row.missing,
    draft: acc.draft + row.draft,
    submitted: acc.submitted + row.submitted,
    approved: acc.approved + row.approved,
    locked: acc.locked + row.locked,
  }), { eligible: 0, generated: 0, missing: 0, draft: 0, submitted: 0, approved: 0, locked: 0 });
  const summaryScope = selectedSummary ?? summaryTotals;
  const { data: detail } = useQuery({
    queryKey: ["timesheet", selectedId],
    queryFn: () => timesheetApi.get(selectedId!),
    enabled: !!selectedId,
  });

  const generate = useMutation({
    mutationFn: () =>
      timesheetApi.generate({ employeeId: genEmployee, periodId, shiftId: genShift || undefined, overwrite }),
    onSuccess: (ts) => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      qc.invalidateQueries({ queryKey: ["timesheetProjectSummary"] });
      setSelectedId(ts.id ?? null);
    },
  });

  const lifecycle = useMutation({
    mutationFn: ({ id, action }: { id: string; action: "submit" | "approve" | "lock" | "reopen" }) =>
      timesheetApi[action](id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      qc.invalidateQueries({ queryKey: ["timesheet", selectedId] });
      qc.invalidateQueries({ queryKey: ["timesheetProjectSummary"] });
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => timesheetApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      qc.invalidateQueries({ queryKey: ["timesheetProjectSummary"] });
      setSelectedId(null);
    },
  });

  const [bulkMsg, setBulkMsg] = useState<string | null>(null);
  const [bulkError, setBulkError] = useState<string | null>(null);
  const [bulkJobId, setBulkJobId] = useState<string | null>(null);

  const generateAll = useMutation({
    mutationFn: () => timesheetApi.startGenerateBulk(periodId, projectId || undefined),
    onSuccess: (r) => {
      setBulkError(null);
      setBulkJobId(r.id);
      setBulkMsg("Timesheet generation is running in the background...");
    },
  });
  const { data: bulkJob } = useQuery({
    queryKey: ["timesheetBulkJob", bulkJobId],
    queryFn: () => timesheetApi.getGenerateBulkJob(bulkJobId!),
    enabled: !!bulkJobId,
    refetchInterval: (query) => query.state.data?.status === "RUNNING" ? 2000 : false,
  });
  useEffect(() => {
    if (!bulkJob) return;
    if (bulkJob.status === "RUNNING") {
      setBulkMsg(bulkJob.message || `Generating... processed ${bulkJob.processed} / ${bulkJob.total} in ${fmtDuration(bulkJob.elapsedSeconds)}.`);
      return;
    }
    if (bulkJob.status === "COMPLETED") {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      qc.invalidateQueries({ queryKey: ["timesheetProjectSummary"] });
      setBulkMsg(`Generated ${bulkJob.created}, skipped ${bulkJob.skipped} in ${fmtDuration(bulkJob.durationSeconds ?? bulkJob.elapsedSeconds)}.`);
      setBulkError(null);
    } else {
      setBulkError(bulkJob.message || "Timesheet generation failed.");
    }
    setBulkJobId(null);
  }, [bulkJob, periodId, qc]);
  const generateCrew = useMutation({
    mutationFn: () => timesheetApi.generateByCrew(genCrew, periodId),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      qc.invalidateQueries({ queryKey: ["timesheetProjectSummary"] });
      const detail = r.messages && r.messages.length ? " — " + r.messages.join("  •  ") : "";
      setBulkMsg(`Crew: generated ${r.created}, skipped ${r.skipped}.${detail}`);
    },
  });
  const [submitJobId, setSubmitJobId] = useState<string | null>(null);
  const submitAll = useMutation({
    mutationFn: () => timesheetApi.startSubmitAll(period!.periodYear, period!.periodMonth, projectId || undefined),
    onSuccess: (r) => { setBulkError(null); setSubmitJobId(r.id); setBulkMsg("Submitting timesheets in the background..."); },
  });
  const { data: submitJob } = useQuery({
    queryKey: ["timesheetSubmitJob", submitJobId],
    queryFn: () => timesheetApi.getSubmitAllJob(submitJobId!),
    enabled: !!submitJobId,
    refetchInterval: (query) => query.state.data?.status === "RUNNING" ? 2000 : false,
  });
  useEffect(() => {
    if (!submitJob) return;
    if (submitJob.status === "RUNNING") { setBulkMsg(submitJob.message || `Submitting... ${submitJob.done} / ${submitJob.total}.`); return; }
    if (submitJob.status === "COMPLETED") {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      qc.invalidateQueries({ queryKey: ["timesheetProjectSummary"] });
      setBulkMsg(`Submitted ${submitJob.done} draft timesheet(s) in ${fmtDuration(submitJob.durationSeconds ?? submitJob.elapsedSeconds)}.`);
      setBulkError(null);
    } else {
      setBulkError(submitJob.message || "Submit failed.");
    }
    setSubmitJobId(null);
  }, [submitJob, periodId, qc]);

  const [approveJobId, setApproveJobId] = useState<string | null>(null);
  const approveAll = useMutation({
    mutationFn: () => timesheetApi.startApproveAll(period!.periodYear, period!.periodMonth, projectId || undefined),
    onSuccess: (r) => { setBulkError(null); setApproveJobId(r.id); setBulkMsg("Approving timesheets in the background..."); },
  });
  const { data: approveJob } = useQuery({
    queryKey: ["timesheetApproveJob", approveJobId],
    queryFn: () => timesheetApi.getApproveAllJob(approveJobId!),
    enabled: !!approveJobId,
    refetchInterval: (query) => query.state.data?.status === "RUNNING" ? 2000 : false,
  });
  useEffect(() => {
    if (!approveJob) return;
    if (approveJob.status === "RUNNING") { setBulkMsg(approveJob.message || `Approving... ${approveJob.done} / ${approveJob.total}.`); return; }
    if (approveJob.status === "COMPLETED") {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      qc.invalidateQueries({ queryKey: ["timesheetProjectSummary"] });
      setBulkMsg(`Approved ${approveJob.done} submitted timesheet(s) in ${fmtDuration(approveJob.durationSeconds ?? approveJob.elapsedSeconds)}.`);
      setBulkError(null);
    } else {
      setBulkError(approveJob.message || "Approve failed.");
    }
    setApproveJobId(null);
  }, [approveJob, periodId, qc]);

  const [lockJobId, setLockJobId] = useState<string | null>(null);
  const lockProject = useMutation({
    mutationFn: () => periodLockApi.startLock(periodId, projectId, "ALL"),
    onSuccess: (r) => { setBulkError(null); setLockJobId(r.id); setBulkMsg("Locking timesheets in the background..."); },
  });
  const { data: lockJob } = useQuery({
    queryKey: ["timesheetLockJob", lockJobId],
    queryFn: () => periodLockApi.getLockJob(lockJobId!),
    enabled: !!lockJobId,
    refetchInterval: (query) => query.state.data?.status === "RUNNING" ? 2000 : false,
  });
  useEffect(() => {
    if (!lockJob) return;
    if (lockJob.status === "RUNNING") { setBulkMsg(lockJob.message || `Locking... ${lockJob.done} / ${lockJob.total}.`); return; }
    if (lockJob.status === "COMPLETED") {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      setBulkMsg(`Project locked for payroll in ${fmtDuration(lockJob.durationSeconds ?? lockJob.elapsedSeconds)}.`);
      setBulkError(null);
    } else {
      setBulkError(lockJob.message || "Lock failed.");
    }
    setLockJobId(null);
  }, [lockJob, periodId, qc]);

  const periodEditable = period?.status !== "CLOSED";
  const isGeneratingBulk = generateAll.isPending || bulkJob?.status === "RUNNING";
  const isSubmittingBulk = submitAll.isPending || submitJob?.status === "RUNNING";
  const isApprovingBulk = approveAll.isPending || approveJob?.status === "RUNNING";
  const isLockingBulk = lockProject.isPending || lockJob?.status === "RUNNING";
  const filtered = rows;

  return (
    <Box>
      <Typography variant="h5" mb={2}>Timesheets</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={5}>
            <TextField select fullWidth size="small" label="Period" value={periodId} onChange={(e) => { setPeriodId(e.target.value); setSelectedId(null); setPage(0); }}>
              {periods.length === 0 && <MenuItem value="" disabled>No periods — create them in Payroll Calendar</MenuItem>}
              {periods.map((p) => (
                <MenuItem key={p.id} value={p.id}>{p.name} ({p.status})</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Project" value={projectId} onChange={(e) => { setProjectId(e.target.value); setSelectedId(null); setGenEmployee(""); setGenShift(""); setPage(0); }}>
              <MenuItem value="">All projects</MenuItem>
              {projects.map((p) => (
                <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>
              ))}
            </TextField>
          </Grid>
          {period && (
            <Grid item xs={12} sm={4}>
              <Chip size="small" label={period.status} color={PERIOD_COLOR[period.status ?? "OPEN"] ?? "default"} />
              <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                {period.startDate} → {period.endDate}
              </Typography>
            </Grid>
          )}
          {period && (
            <Grid item xs={12}>
              <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
                <Button size="small" variant="outlined" disabled={!periodEditable || isGeneratingBulk}
                  onClick={() => {
                    if (!projectId && !window.confirm("No project selected — this will generate timesheets for EVERY project company-wide. Continue?")) {
                      return;
                    }
                    generateAll.mutate();
                  }}>
                  {isGeneratingBulk ? "Generating..." : projectId ? `Generate all (roster) — this project only` : `Generate all (roster) — company-wide`}
                </Button>
                <TextField select size="small" label="Crew" value={genCrew} onChange={(e) => setGenCrew(e.target.value)} sx={{ minWidth: 180 }}>
                  <MenuItem value="">(pick a crew)</MenuItem>
                  {allCrews.map((c) => <MenuItem key={c.id} value={c.id}>{c.code} — {c.name}</MenuItem>)}
                </TextField>
                <Button size="small" variant="outlined" disabled={!periodEditable || !genCrew || generateCrew.isPending} onClick={() => generateCrew.mutate()}>
                  Generate for crew
                </Button>
                <Button size="small" variant="outlined" disabled={rows.every((t) => t.status !== "DRAFT") || isSubmittingBulk} onClick={() => submitAll.mutate()}>
                  Submit all drafts
                </Button>
                <Button size="small" variant="outlined" color="success" disabled={rows.every((t) => t.status !== "SUBMITTED") || isApprovingBulk} onClick={() => approveAll.mutate()}>
                  Approve all submitted
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  color="warning"
                  disabled={!projectId || !periodEditable || isLockingBulk}
                  onClick={() => lockProject.mutate()}
                >
                  Lock all approved
                </Button>
              </Stack>
              {bulkMsg && <Alert severity="success" sx={{ mt: 1 }} onClose={() => setBulkMsg(null)}>{bulkMsg}</Alert>}
              {bulkError && <Alert severity="error" sx={{ mt: 1 }} onClose={() => setBulkError(null)}>{bulkError}</Alert>}
              {generateAll.isError && (
                <Alert severity="error" sx={{ mt: 1 }}>
                  {(generateAll.error as any)?.response?.data?.message ?? "Could not start timesheet generation."}
                </Alert>
              )}
            </Grid>
          )}
        </Grid>
      </Paper>

      {period && (
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
          <Stack direction="row" spacing={1} flexWrap="wrap">
            {[
              { label: "Eligible", value: summaryScope.eligible },
              { label: "Generated", value: summaryScope.generated },
              { label: "Missing / skipped", value: summaryScope.missing },
              { label: "Draft", value: summaryScope.draft },
              { label: "Submitted", value: summaryScope.submitted },
              { label: "Approved", value: summaryScope.approved },
              { label: "Locked", value: summaryScope.locked },
            ].map((item) => (
              <Box key={item.label} sx={{ minWidth: 120, border: "1px solid", borderColor: "divider", borderRadius: 1, px: 1.25, py: 1 }}>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6">{item.value}</Typography>
              </Box>
            ))}
          </Stack>
          {!projectId && timesheetProjectSummary.length > 0 && (
            <Table size="small" sx={{ mt: 1.5 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Project</TableCell>
                  <TableCell align="right">Eligible</TableCell>
                  <TableCell align="right">Generated</TableCell>
                  <TableCell align="right">Missing</TableCell>
                  <TableCell align="right">Draft</TableCell>
                  <TableCell align="right">Approved</TableCell>
                  <TableCell align="right">Locked</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {timesheetProjectSummary.map((row) => (
                  <TableRow key={row.projectId} hover onClick={() => { setProjectId(row.projectId); setPage(0); }} sx={{ cursor: "pointer" }}>
                    <TableCell>{row.projectCode} — {row.projectName}</TableCell>
                    <TableCell align="right">{row.eligible}</TableCell>
                    <TableCell align="right">{row.generated}</TableCell>
                    <TableCell align="right">{row.missing}</TableCell>
                    <TableCell align="right">{row.draft}</TableCell>
                    <TableCell align="right">{row.approved}</TableCell>
                    <TableCell align="right">{row.locked}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>Generate timesheet</Typography>
        {!projectId && (
          <Alert severity="info" sx={{ mb: 1 }}>
            Pick a project to load employees for single-timesheet generation. Company-wide bulk generation does not need the employee list.
          </Alert>
        )}
        {!periodEditable && period && (
          <Typography variant="body2" color="warning.main" mb={1}>
            This period is CLOSED and cannot be edited.
          </Typography>
        )}
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={4}>
            <Autocomplete
              size="small"
              options={employeeOptions}
              getOptionLabel={(o) => `${o.employeeNumber} — ${o.firstName} ${o.lastName}`}
              isOptionEqualToValue={(o, v) => o.id === v.id}
              value={employeeOptions.find((e) => e.id === genEmployee) ?? null}
              onChange={(_, v) => { setGenEmployee(v?.id ?? ""); setGenShift(""); }}
              renderInput={(params) => <TextField {...params} label="Employee" />}
            />
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Shift" value={genShift} onChange={(e) => setGenShift(e.target.value)}>
              <MenuItem value="">(from roster)</MenuItem>
              {availableShifts.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} — {s.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField select fullWidth size="small" label="Overwrite" value={overwrite ? "yes" : "no"} onChange={(e) => setOverwrite(e.target.value === "yes")}>
              <MenuItem value="no">No</MenuItem>
              <MenuItem value="yes">Yes</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Button variant="contained" disabled={!genEmployee || !periodEditable || generate.isPending} onClick={() => generate.mutate()}>
              Generate
            </Button>
          </Grid>
        </Grid>
        {generate.isError && <Typography color="error" variant="body2" mt={1}>{(generate.error as any)?.response?.data?.message ?? "Generate failed."}</Typography>}
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, mb: 2 }}>
        <Box sx={{ p: 1.5 }}>
          {!projectId && (
            <Alert severity="info" sx={{ mb: 1 }}>
              Pick a project to view timesheets. This avoids loading very large company-wide lists.
            </Alert>
          )}
          <TextField size="small" fullWidth placeholder="Search employee (name or number)" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
          {projectId && (
            <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between" mt={1}>
              <Typography variant="body2" color="text.secondary">
                {list ? `${list.totalElements} timesheet(s)${q.trim() ? " found" : ""} — page ${list.totalPages === 0 ? 0 : list.page + 1} of ${list.totalPages}` : "Loading..."}
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" variant="outlined" disabled={!list || list.first} onClick={() => setPage((p) => Math.max(0, p - 1))}>
                  Previous
                </Button>
                <Button size="small" variant="outlined" disabled={!list || list.last} onClick={() => setPage((p) => p + 1)}>
                  Next
                </Button>
              </Stack>
            </Stack>
          )}
        </Box>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Employee</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Worked</TableCell>
              <TableCell align="right">OT</TableCell>
              <TableCell align="right">Absence</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {filtered.map((t) => (
              <TableRow key={t.id} hover selected={t.id === selectedId}>
                <TableCell>{t.employeeNumber} — {t.employeeName}</TableCell>
                <TableCell><Chip size="small" label={t.status} color={STATUS_COLOR[t.status ?? "DRAFT"] ?? "default"} /></TableCell>
                <TableCell align="right">{t.totalWorkedHours ?? 0}</TableCell>
                <TableCell align="right">{t.totalOtHours ?? 0}</TableCell>
                <TableCell align="right">{t.totalAbsenceDays ?? 0}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setSelectedId(t.id ?? null)}>Open</Button>
                  <Button size="small" color="error" onClick={() => t.id && del.mutate(t.id)}>Delete</Button>
                </TableCell>
              </TableRow>
            ))}
            {filtered.length === 0 && (
              <TableRow><TableCell colSpan={6}><Typography variant="body2" color="text.secondary" p={1}>No timesheets match. Generate above or clear the search.</Typography></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      {detail && (
        <TimesheetDetail
          key={detail.id}
          timesheet={detail}
          timeTypes={timeTypes}
          lifecycleError={(lifecycle.error as any)?.response?.data?.message}
          onLifecycle={(action) => detail.id && lifecycle.mutate({ id: detail.id, action })}
          onDelete={() => detail.id && del.mutate(detail.id)}
        />
      )}
    </Box>
  );
}

function SummaryStat({ label, value }: { label: string; value: string | number }) {
  return (
    <Box sx={{
      minWidth: 118,
      px: 1.25,
      py: 1,
      borderRadius: 1,
      bgcolor: "background.paper",
      border: "1px solid",
      borderColor: "divider",
    }}>
      <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>{label}</Typography>
      <Typography variant="body1" fontWeight={700}>{value}</Typography>
    </Box>
  );
}

function AllocationCard({
  project,
  costCode,
  hours,
}: {
  project: string;
  costCode: string;
  hours: number;
}) {
  return (
    <Box sx={{
      minWidth: 220,
      flex: "1 1 220px",
      px: 1.25,
      py: 1,
      borderRadius: 1,
      bgcolor: "background.paper",
      border: "1px solid",
      borderColor: "divider",
      borderLeft: "4px solid",
      borderLeftColor: "primary.main",
    }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" gap={1}>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
            {project}
          </Typography>
          <Typography variant="body2" fontWeight={600} noWrap title={costCode}>
            {costCode}
          </Typography>
        </Box>
        <Typography variant="h6" fontWeight={800} sx={{ lineHeight: 1 }}>
          {hours}h
        </Typography>
      </Stack>
    </Box>
  );
}

function TimesheetDetail({
  timesheet,
  timeTypes,
  lifecycleError,
  onLifecycle,
  onDelete,
}: {
  timesheet: Timesheet;
  timeTypes: { id?: string; code: string; name: string }[];
  lifecycleError?: string;
  onLifecycle: (action: "submit" | "approve" | "lock" | "reopen") => void;
  onDelete: () => void;
}) {
  const qc = useQueryClient();
  const [days, setDays] = useState<TimesheetDay[]>(timesheet.days);
  const [costOpen, setCostOpen] = useState<number | null>(null);
  const editable = timesheet.status === "DRAFT";

  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: costCodes = [] } = useQuery({ queryKey: ["costCodesAll"], queryFn: costCodeApi.list });
  const { data: summary } = useQuery({
    queryKey: ["timesheetSummary", timesheet.id],
    queryFn: () => timesheetApi.summary(timesheet.id!),
    enabled: !!timesheet.id,
  });

  useEffect(() => setDays(timesheet.days), [timesheet]);

  const save = useMutation({
    mutationFn: () => timesheetApi.saveDays(timesheet.id!, days),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timesheet", timesheet.id] });
      qc.invalidateQueries({ queryKey: ["timesheetSummary", timesheet.id] });
      qc.invalidateQueries({ queryKey: ["timesheets"] });
    },
  });

  const setDay = (idx: number, patch: Partial<TimesheetDay>) =>
    setDays((prev) => prev.map((d, i) => (i === idx ? { ...d, ...patch } : d)));

  const setCost = (idx: number, ci: number, patch: Partial<TimesheetDayCost>) =>
    setDays((prev) => prev.map((d, i) =>
      i === idx ? { ...d, costs: (d.costs ?? []).map((c, j) => (j === ci ? { ...c, ...patch } : c)) } : d));
  const addCost = (idx: number) =>
    setDays((prev) => prev.map((d, i) =>
      i === idx ? { ...d, costs: [...(d.costs ?? []), { projectId: d.projectId, costCodeId: d.costCodeId, hours: 0 }] } : d));
  const removeCost = (idx: number, ci: number) =>
    setDays((prev) => prev.map((d, i) =>
      i === idx ? { ...d, costs: (d.costs ?? []).filter((_, j) => j !== ci) } : d));
  const costLabel = (costCodeId?: string) => {
    const cc = costCodes.find((c) => c.id === costCodeId);
    return cc ? `${cc.code} ${cc.name}` : "No code";
  };
  const splitSummary = (d: TimesheetDay) => {
    const active = (d.costs ?? []).filter((c) => c.projectId || c.costCodeId || Number(c.hours));
    if (active.length === 0) return "Split";
    return active.map((c) => `${costLabel(c.costCodeId)} ${Number(c.hours) || 0}h`).join(" · ");
  };
  const costedHours = (d: TimesheetDay) => Number(d.normalHours ?? 0) + Number(d.otHours ?? 0);
  const dayEditable = (_d?: TimesheetDay) => editable;

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1.5} flexWrap="wrap" gap={1}>
        <Box>
          <Typography fontWeight={600}>{timesheet.employeeNumber} — {timesheet.employeeName}</Typography>
          <Typography variant="caption" color="text.secondary">
            {timesheet.periodYear}/{String(timesheet.periodMonth).padStart(2, "0")} · {timesheet.status}
            {" · worked "}{timesheet.totalWorkedHours ?? 0}h · OT {timesheet.totalOtHours ?? 0}h · absence {timesheet.totalAbsenceDays ?? 0}d
          </Typography>
        </Box>
        <Stack direction="row" spacing={1} flexWrap="wrap">
          <Button size="small" startIcon={<PrintIcon />} onClick={() => printTimesheet(timesheet, summary, days, projects, costCodes, timeTypes)}>
            Print
          </Button>
          {editable && <Button size="small" variant="contained" disabled={save.isPending} onClick={() => save.mutate()}>Save days</Button>}
          {timesheet.status === "DRAFT" && <Button size="small" onClick={() => onLifecycle("submit")}>Submit</Button>}
          {timesheet.status === "SUBMITTED" && <Button size="small" color="success" onClick={() => onLifecycle("approve")}>Approve</Button>}
          {timesheet.status === "APPROVED" && <Button size="small" color="warning" onClick={() => onLifecycle("lock")}>Lock</Button>}
          {(timesheet.status === "SUBMITTED" || timesheet.status === "APPROVED" || timesheet.status === "LOCKED") && <Button size="small" onClick={() => onLifecycle("reopen")}>Reopen</Button>}
          <Button size="small" color="error" onClick={onDelete}>Delete</Button>
        </Stack>
      </Stack>

      {lifecycleError && <Alert severity="error" sx={{ mb: 1 }}>{lifecycleError}</Alert>}

      {save.isError && (
        <Alert severity="error" sx={{ mb: 1 }}>
          {(save.error as any)?.response?.data?.message ?? "Save failed."}
        </Alert>
      )}

      {summary && (
        <Box sx={{
          mb: 1.5,
          p: 1.5,
          borderRadius: 1.5,
          bgcolor: "grey.50",
          border: "1px solid",
          borderColor: "divider",
        }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1.25} gap={1} flexWrap="wrap">
            <Box>
              <Typography variant="subtitle2" fontWeight={700}>Timesheet summary</Typography>
              <Typography variant="caption" color="text.secondary">
                Payroll hours and cost allocation for this period
              </Typography>
            </Box>
            <Chip size="small" color={summary.overtimeHours > 0 ? "warning" : "default"}
              label={`${summary.workedHours} payable hours`} />
          </Stack>

          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <SummaryStat label="Normal" value={`${summary.normalHours}h`} />
            <SummaryStat label="Weekend" value={`${summary.restHours ?? 0}h`} />
            <SummaryStat label="Holiday" value={`${summary.holidayHours ?? 0}h`} />
            <SummaryStat label="Overtime" value={`${summary.overtimeHours}h`} />
            <SummaryStat label="Worked days" value={summary.workedDays} />
            <SummaryStat label="Absence" value={`${summary.absenceDays}d · ${summary.absenceHours}h`} />
            <SummaryStat label="Leave" value={`${summary.leaveDays}d · ${summary.leaveHours}h`} />
            <SummaryStat label="Rest/Holiday" value={`${summary.restDays + summary.holidayDays}d`} />
          </Stack>
          {summary.lines.length > 0 && (
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
              {summary.lines.map((l) => (
                <Chip key={l.category} size="small" variant="outlined"
                  color={l.paid ? "default" : "warning"}
                  label={`${l.category}: ${l.days}d · ${l.hours}h${l.paid ? "" : " (unpaid)"}`} />
              ))}
            </Stack>
          )}
          {(summary.allocationLines?.length ?? 0) > 0 && (
            <Box sx={{ mt: 1.5 }}>
              <Typography variant="subtitle2" fontWeight={700} sx={{ display: "block", mb: 0.75 }}>
                Cost allocation
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {summary.allocationLines.map((l) => (
                  <AllocationCard
                    key={`${l.projectId}-${l.costCodeId}`}
                    project={l.projectCode ?? "Project"}
                    costCode={`${l.costCode ?? "Cost code"}${l.costCodeName ? ` — ${l.costCodeName}` : ""}`}
                    hours={l.hours}
                  />
                ))}
              </Stack>
            </Box>
          )}
        </Box>
      )}

      <Box sx={{ overflowX: "auto" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>In</TableCell>
              <TableCell>Out</TableCell>
              <TableCell align="right">Planned</TableCell>
              <TableCell align="right">Worked</TableCell>
              <TableCell align="right">Normal</TableCell>
              <TableCell align="right">OT</TableCell>
              <TableCell align="right">Decl</TableCell>
              <TableCell align="right">Undecl</TableCell>
              <TableCell align="right">Inelig OT</TableCell>
              <TableCell>Project</TableCell>
              <TableCell>Cost code</TableCell>
              <TableCell>Cost split</TableCell>
              <TableCell>Remarks</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {days.map((d, idx) => (
              <Fragment key={d.id ?? d.workDate}>
              <TableRow>
                <TableCell>{fmtDay(d.workDate)}</TableCell>
                <TableCell>
                  {dayEditable(d) ? (
                    <TextField select size="small" value={d.timeTypeId ?? ""} onChange={(e) => setDay(idx, { timeTypeId: e.target.value })} sx={{ minWidth: 220 }}>
                      {timeTypes.map((t) => <MenuItem key={t.id} value={t.id}>{t.code} - {t.name}</MenuItem>)}
                    </TextField>
                  ) : (d.timeTypeCode ?? "")}
                </TableCell>
                <TableCell>
                  {dayEditable(d) ? (
                    <TextField type="time" size="small" value={d.actualIn ?? ""} onChange={(e) => setDay(idx, { actualIn: e.target.value || null })} sx={{ width: 110 }} />
                  ) : (d.actualIn ?? "")}
                </TableCell>
                <TableCell>
                  {dayEditable(d) ? (
                    <TextField type="time" size="small" value={d.actualOut ?? ""} onChange={(e) => setDay(idx, { actualOut: e.target.value || null })} sx={{ width: 110 }} />
                  ) : (d.actualOut ?? "")}
                </TableCell>
                <TableCell align="right">{d.plannedHours ?? 0}</TableCell>
                <TableCell align="right">
                  {dayEditable(d) ? (
                    <TextField type="number" size="small" value={d.workedHours ?? 0} onChange={(e) => setDay(idx, { workedHours: Number(e.target.value) })} sx={{ width: 80 }} />
                  ) : (d.workedHours ?? 0)}
                </TableCell>
                <TableCell align="right">{d.normalHours ?? 0}</TableCell>
                <TableCell align="right">{d.otHours ?? 0}</TableCell>
                <TableCell align="right">{d.declaredOtHours ?? 0}</TableCell>
                <TableCell align="right">{d.undeclaredOtHours ?? 0}</TableCell>
                <TableCell align="right" sx={{ color: "text.disabled" }}>{d.ineligibleOtHours ?? 0}</TableCell>
                <TableCell>
                  {dayEditable(d) ? (
                    <TextField select size="small" value={d.projectId ?? ""}
                      onChange={(e) => setDay(idx, { projectId: e.target.value || undefined, costCodeId: undefined })}
                      sx={{ minWidth: 140 }}>
                      <MenuItem value="">(none)</MenuItem>
                      {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code}</MenuItem>)}
                    </TextField>
                  ) : (projects.find((p) => p.id === d.projectId)?.code ?? "")}
                </TableCell>
                <TableCell>
                  {dayEditable(d) ? (
                    <TextField select size="small" value={d.costCodeId ?? ""}
                      onChange={(e) => setDay(idx, { costCodeId: e.target.value || undefined })}
                      sx={{ minWidth: 160 }}>
                      <MenuItem value="">(none)</MenuItem>
                      {costCodes.filter((cc) => !d.projectId || cc.projectId === d.projectId).map((cc) => (
                        <MenuItem key={cc.id} value={cc.id}>{cc.code} — {cc.name}</MenuItem>
                      ))}
                    </TextField>
                  ) : (() => {
                    const cc = costCodes.find((c) => c.id === d.costCodeId);
                    return cc ? `${cc.code} — ${cc.name}` : "";
                  })()}
                </TableCell>
                <TableCell>
                  <Button size="small" disabled={!dayEditable(d) && (d.costs?.length ?? 0) === 0} onClick={() => setCostOpen(costOpen === idx ? null : idx)}>
                    {splitSummary(d)}
                  </Button>
                </TableCell>
                <TableCell>
                  {dayEditable(d) ? (
                    <TextField size="small" value={d.remarks ?? ""} onChange={(e) => setDay(idx, { remarks: e.target.value })} sx={{ minWidth: 140 }} />
                  ) : (d.remarks ?? "")}
                </TableCell>
              </TableRow>
              {costOpen === idx && (
                <TableRow>
                  <TableCell colSpan={15} sx={{ bgcolor: "action.hover" }}>
                    <Typography variant="caption" color="text.secondary">Split {d.workDate} hours across cost codes</Typography>
                    {(d.costs ?? []).map((c, ci) => (
                      <Stack key={ci} direction="row" spacing={1} alignItems="center" mt={0.5}>
                        <TextField select size="small" label="Project" value={c.projectId ?? ""} disabled={!dayEditable(d)}
                          onChange={(e) => setCost(idx, ci, { projectId: e.target.value, costCodeId: undefined })} sx={{ minWidth: 140 }}>
                          {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code}</MenuItem>)}
                        </TextField>
                        <TextField select size="small" label="Cost code" value={c.costCodeId ?? ""} disabled={!dayEditable(d)}
                          onChange={(e) => setCost(idx, ci, { costCodeId: e.target.value })} sx={{ minWidth: 160 }}>
                          {costCodes.filter((cc) => !c.projectId || cc.projectId === c.projectId).map((cc) => (
                            <MenuItem key={cc.id} value={cc.id}>{cc.code} — {cc.name}</MenuItem>
                          ))}
                        </TextField>
                        <TextField type="number" size="small" label="Hours" value={c.hours ?? 0} disabled={!dayEditable(d)}
                          onChange={(e) => setCost(idx, ci, { hours: Number(e.target.value) })} sx={{ width: 90 }} />
                        {dayEditable(d) && <Button size="small" color="error" onClick={() => removeCost(idx, ci)}>Remove</Button>}
                      </Stack>
                    ))}
                    {dayEditable(d) && <Button size="small" sx={{ mt: 0.5 }} onClick={() => addCost(idx)}>+ Add cost code</Button>}
                    {(d.costs?.length ?? 0) > 0 && (() => {
                      const sum = (d.costs ?? []).reduce((a, c) => a + (Number(c.hours) || 0), 0);
                      const target = costedHours(d);
                      const ok = Math.abs(sum - target) < 0.01;
                      return (
                        <Typography variant="caption" sx={{ ml: 1 }} color={ok ? "success.main" : "error.main"}>
                          Σ {sum} / costed {target} {ok ? "✓" : "✗ must match"}
                        </Typography>
                      );
                    })()}
                  </TableCell>
                </TableRow>
              )}
              </Fragment>
            ))}
          </TableBody>
        </Table>
      </Box>
    </Paper>
  );
}
