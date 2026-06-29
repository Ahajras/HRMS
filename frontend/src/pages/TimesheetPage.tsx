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
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { costCodeApi, crewApi, employeeApi, periodApi, projectApi, shiftApi, timeTypeApi, timesheetApi } from "../api/resources";
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

export default function TimesheetPage() {
  const qc = useQueryClient();
  const [periodId, setPeriodId] = useState("");
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // Generate form
  const [genEmployee, setGenEmployee] = useState("");
  const [genShift, setGenShift] = useState("");
  const [overwrite, setOverwrite] = useState(false);

  const { data: employees } = useQuery({
    queryKey: ["employeesAll"],
    queryFn: () => employeeApi.list(0, 500),
  });
  const { data: shifts = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });
  const { data: timeTypes = [] } = useQuery({ queryKey: ["timeTypes"], queryFn: timeTypeApi.list });
  const { data: periods = [] } = useQuery({ queryKey: ["periods"], queryFn: () => periodApi.list() });
  const { data: allCrews = [] } = useQuery({ queryKey: ["crews"], queryFn: crewApi.list });
  const [genCrew, setGenCrew] = useState("");

  const period = periods.find((p) => p.id === periodId);
  const { data: list = [] } = useQuery({
    queryKey: ["timesheets", periodId],
    queryFn: () => timesheetApi.listByPeriod(period!.periodYear, period!.periodMonth),
    enabled: !!period,
  });
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
      setSelectedId(ts.id ?? null);
    },
  });

  const lifecycle = useMutation({
    mutationFn: ({ id, action }: { id: string; action: "submit" | "approve" | "lock" | "reopen" }) =>
      timesheetApi[action](id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      qc.invalidateQueries({ queryKey: ["timesheet", selectedId] });
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => timesheetApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      setSelectedId(null);
    },
  });

  const [q, setQ] = useState("");
  const [bulkMsg, setBulkMsg] = useState<string | null>(null);

  const generateAll = useMutation({
    mutationFn: () => timesheetApi.generateBulk(periodId),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      setBulkMsg(`Generated ${r.created}, skipped ${r.skipped} (already existed).`);
    },
  });
  const generateCrew = useMutation({
    mutationFn: () => timesheetApi.generateByCrew(genCrew, periodId),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      const detail = r.messages && r.messages.length ? " — " + r.messages.join("  •  ") : "";
      setBulkMsg(`Crew: generated ${r.created}, skipped ${r.skipped}.${detail}`);
    },
  });
  const submitAll = useMutation({
    mutationFn: () => timesheetApi.submitAll(period!.periodYear, period!.periodMonth),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["timesheets", periodId] });
      setBulkMsg(`Submitted ${r.submitted} draft timesheet(s).`);
    },
  });

  const periodOpen = period?.status === "OPEN";
  const filtered = list.filter((t) => {
    const s = q.trim().toLowerCase();
    if (!s) return true;
    return (t.employeeName ?? "").toLowerCase().includes(s) || (t.employeeNumber ?? "").toLowerCase().includes(s);
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Timesheets</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={5}>
            <TextField select fullWidth size="small" label="Period" value={periodId} onChange={(e) => { setPeriodId(e.target.value); setSelectedId(null); }}>
              {periods.length === 0 && <MenuItem value="" disabled>No periods — create them in Payroll Calendar</MenuItem>}
              {periods.map((p) => (
                <MenuItem key={p.id} value={p.id}>{p.name} ({p.status})</MenuItem>
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
                <Button size="small" variant="outlined" disabled={!periodOpen || generateAll.isPending} onClick={() => generateAll.mutate()}>
                  Generate all (roster)
                </Button>
                <TextField select size="small" label="Crew" value={genCrew} onChange={(e) => setGenCrew(e.target.value)} sx={{ minWidth: 180 }}>
                  <MenuItem value="">(pick a crew)</MenuItem>
                  {allCrews.map((c) => <MenuItem key={c.id} value={c.id}>{c.code} — {c.name}</MenuItem>)}
                </TextField>
                <Button size="small" variant="outlined" disabled={!periodOpen || !genCrew || generateCrew.isPending} onClick={() => generateCrew.mutate()}>
                  Generate for crew
                </Button>
                <Button size="small" variant="outlined" disabled={list.every((t) => t.status !== "DRAFT") || submitAll.isPending} onClick={() => submitAll.mutate()}>
                  Submit all drafts
                </Button>
              </Stack>
              {bulkMsg && <Alert severity="success" sx={{ mt: 1 }} onClose={() => setBulkMsg(null)}>{bulkMsg}</Alert>}
            </Grid>
          )}
        </Grid>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>Generate timesheet</Typography>
        {!periodOpen && period && (
          <Typography variant="body2" color="warning.main" mb={1}>
            This period is {period.status}. Reopen it in Payroll Calendar to add or edit timesheets.
          </Typography>
        )}
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={4}>
            <Autocomplete
              size="small"
              options={employees?.content ?? []}
              getOptionLabel={(o) => `${o.employeeNumber} — ${o.firstName} ${o.lastName}`}
              isOptionEqualToValue={(o, v) => o.id === v.id}
              value={(employees?.content ?? []).find((e) => e.id === genEmployee) ?? null}
              onChange={(_, v) => setGenEmployee(v?.id ?? "")}
              renderInput={(params) => <TextField {...params} label="Employee" />}
            />
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Shift" value={genShift} onChange={(e) => setGenShift(e.target.value)}>
              <MenuItem value="">(from roster)</MenuItem>
              {shifts.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} — {s.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField select fullWidth size="small" label="Overwrite" value={overwrite ? "yes" : "no"} onChange={(e) => setOverwrite(e.target.value === "yes")}>
              <MenuItem value="no">No</MenuItem>
              <MenuItem value="yes">Yes</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Button variant="contained" disabled={!genEmployee || !periodOpen || generate.isPending} onClick={() => generate.mutate()}>
              Generate
            </Button>
          </Grid>
        </Grid>
        {generate.isError && <Typography color="error" variant="body2" mt={1}>{(generate.error as any)?.response?.data?.message ?? "Generate failed."}</Typography>}
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, mb: 2 }}>
        <Box sx={{ p: 1.5 }}>
          <TextField size="small" fullWidth placeholder="Search employee (name or number)" value={q} onChange={(e) => setQ(e.target.value)} />
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
                  {t.status !== "LOCKED" && (
                    <Button size="small" color="error" onClick={() => t.id && del.mutate(t.id)}>Delete</Button>
                  )}
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
          onLifecycle={(action) => detail.id && lifecycle.mutate({ id: detail.id, action })}
          onDelete={() => detail.id && del.mutate(detail.id)}
        />
      )}
    </Box>
  );
}

function SummaryStat({ label, value }: { label: string; value: string | number }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>{label}</Typography>
      <Typography variant="body2" fontWeight={600}>{value}</Typography>
    </Box>
  );
}

function TimesheetDetail({
  timesheet,
  timeTypes,
  onLifecycle,
  onDelete,
}: {
  timesheet: Timesheet;
  timeTypes: { id?: string; code: string; name: string }[];
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
          {editable && <Button size="small" variant="contained" disabled={save.isPending} onClick={() => save.mutate()}>Save days</Button>}
          {timesheet.status === "DRAFT" && <Button size="small" onClick={() => onLifecycle("submit")}>Submit</Button>}
          {timesheet.status === "SUBMITTED" && <Button size="small" color="success" onClick={() => onLifecycle("approve")}>Approve</Button>}
          {timesheet.status === "APPROVED" && <Button size="small" color="warning" onClick={() => onLifecycle("lock")}>Lock</Button>}
          {(timesheet.status === "SUBMITTED" || timesheet.status === "APPROVED") && <Button size="small" onClick={() => onLifecycle("reopen")}>Reopen</Button>}
          {timesheet.status !== "LOCKED" && <Button size="small" color="error" onClick={onDelete}>Delete</Button>}
        </Stack>
      </Stack>

      {save.isError && (
        <Alert severity="error" sx={{ mb: 1 }}>
          {(save.error as any)?.response?.data?.message ?? "Save failed."}
        </Alert>
      )}

      {summary && (
        <Box sx={{ mb: 1.5, p: 1.5, bgcolor: "action.hover", borderRadius: 1 }}>
          <Stack direction="row" spacing={3} flexWrap="wrap" useFlexGap>
            <SummaryStat label="Normal" value={`${summary.normalHours}h`} />
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
                  {editable ? (
                    <TextField select size="small" value={d.timeTypeId ?? ""} onChange={(e) => setDay(idx, { timeTypeId: e.target.value })} sx={{ minWidth: 120 }}>
                      {timeTypes.map((t) => <MenuItem key={t.id} value={t.id}>{t.code}</MenuItem>)}
                    </TextField>
                  ) : (d.timeTypeCode ?? "")}
                </TableCell>
                <TableCell>
                  {editable ? (
                    <TextField type="time" size="small" value={d.actualIn ?? ""} onChange={(e) => setDay(idx, { actualIn: e.target.value || null })} sx={{ width: 110 }} />
                  ) : (d.actualIn ?? "")}
                </TableCell>
                <TableCell>
                  {editable ? (
                    <TextField type="time" size="small" value={d.actualOut ?? ""} onChange={(e) => setDay(idx, { actualOut: e.target.value || null })} sx={{ width: 110 }} />
                  ) : (d.actualOut ?? "")}
                </TableCell>
                <TableCell align="right">{d.plannedHours ?? 0}</TableCell>
                <TableCell align="right">
                  {editable ? (
                    <TextField type="number" size="small" value={d.workedHours ?? 0} onChange={(e) => setDay(idx, { workedHours: Number(e.target.value) })} sx={{ width: 80 }} />
                  ) : (d.workedHours ?? 0)}
                </TableCell>
                <TableCell align="right">{d.normalHours ?? 0}</TableCell>
                <TableCell align="right">{d.otHours ?? 0}</TableCell>
                <TableCell align="right">{d.declaredOtHours ?? 0}</TableCell>
                <TableCell align="right">{d.undeclaredOtHours ?? 0}</TableCell>
                <TableCell align="right" sx={{ color: "text.disabled" }}>{d.ineligibleOtHours ?? 0}</TableCell>
                <TableCell>
                  <Button size="small" onClick={() => setCostOpen(costOpen === idx ? null : idx)}>
                    {(d.costs?.length ?? 0) > 0 ? `${d.costs!.length} code(s)` : "split"}
                  </Button>
                </TableCell>
                <TableCell>
                  {editable ? (
                    <TextField size="small" value={d.remarks ?? ""} onChange={(e) => setDay(idx, { remarks: e.target.value })} sx={{ minWidth: 140 }} />
                  ) : (d.remarks ?? "")}
                </TableCell>
              </TableRow>
              {costOpen === idx && (
                <TableRow>
                  <TableCell colSpan={13} sx={{ bgcolor: "action.hover" }}>
                    <Typography variant="caption" color="text.secondary">Split {d.workDate} hours across cost codes</Typography>
                    {(d.costs ?? []).map((c, ci) => (
                      <Stack key={ci} direction="row" spacing={1} alignItems="center" mt={0.5}>
                        <TextField select size="small" label="Project" value={c.projectId ?? ""} disabled={!editable}
                          onChange={(e) => setCost(idx, ci, { projectId: e.target.value, costCodeId: undefined })} sx={{ minWidth: 140 }}>
                          {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code}</MenuItem>)}
                        </TextField>
                        <TextField select size="small" label="Cost code" value={c.costCodeId ?? ""} disabled={!editable}
                          onChange={(e) => setCost(idx, ci, { costCodeId: e.target.value })} sx={{ minWidth: 160 }}>
                          {costCodes.filter((cc) => !c.projectId || cc.projectId === c.projectId).map((cc) => (
                            <MenuItem key={cc.id} value={cc.id}>{cc.code} — {cc.name}</MenuItem>
                          ))}
                        </TextField>
                        <TextField type="number" size="small" label="Hours" value={c.hours ?? 0} disabled={!editable}
                          onChange={(e) => setCost(idx, ci, { hours: Number(e.target.value) })} sx={{ width: 90 }} />
                        {editable && <Button size="small" color="error" onClick={() => removeCost(idx, ci)}>Remove</Button>}
                      </Stack>
                    ))}
                    {editable && <Button size="small" sx={{ mt: 0.5 }} onClick={() => addCost(idx)}>+ Add cost code</Button>}
                    {(d.costs?.length ?? 0) > 0 && (() => {
                      const sum = (d.costs ?? []).reduce((a, c) => a + (Number(c.hours) || 0), 0);
                      const worked = Number(d.workedHours) || 0;
                      const ok = Math.abs(sum - worked) < 0.01;
                      return (
                        <Typography variant="caption" sx={{ ml: 1 }} color={ok ? "success.main" : "error.main"}>
                          Σ {sum} / worked {worked} {ok ? "✓" : "✗ must match"}
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
