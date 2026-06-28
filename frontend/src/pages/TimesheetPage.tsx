import { useEffect, useState } from "react";
import {
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
import { employeeApi, shiftApi, timeTypeApi, timesheetApi } from "../api/resources";
import type { Timesheet, TimesheetDay } from "../api/types";

const MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

const STATUS_COLOR: Record<string, "default" | "info" | "success" | "warning"> = {
  DRAFT: "default",
  SUBMITTED: "info",
  APPROVED: "success",
  LOCKED: "warning",
};

export default function TimesheetPage() {
  const qc = useQueryClient();
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
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
  const { data: list = [] } = useQuery({
    queryKey: ["timesheets", year, month],
    queryFn: () => timesheetApi.listByPeriod(year, month),
  });
  const { data: detail } = useQuery({
    queryKey: ["timesheet", selectedId],
    queryFn: () => timesheetApi.get(selectedId!),
    enabled: !!selectedId,
  });

  const generate = useMutation({
    mutationFn: () =>
      timesheetApi.generate({ employeeId: genEmployee, year, month, shiftId: genShift || undefined, overwrite }),
    onSuccess: (ts) => {
      qc.invalidateQueries({ queryKey: ["timesheets", year, month] });
      setSelectedId(ts.id ?? null);
    },
  });

  const lifecycle = useMutation({
    mutationFn: ({ id, action }: { id: string; action: "submit" | "approve" | "lock" | "reopen" }) =>
      timesheetApi[action](id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timesheets", year, month] });
      qc.invalidateQueries({ queryKey: ["timesheet", selectedId] });
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => timesheetApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timesheets", year, month] });
      setSelectedId(null);
    },
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Timesheets</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={6} sm={2}>
            <TextField select fullWidth size="small" label="Year" value={year} onChange={(e) => { setYear(Number(e.target.value)); setSelectedId(null); }}>
              {Array.from({ length: 7 }, (_, i) => now.getFullYear() - 3 + i).map((y) => (
                <MenuItem key={y} value={y}>{y}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={6} sm={3}>
            <TextField select fullWidth size="small" label="Month" value={month} onChange={(e) => { setMonth(Number(e.target.value)); setSelectedId(null); }}>
              {MONTHS.map((m, i) => <MenuItem key={m} value={i + 1}>{m}</MenuItem>)}
            </TextField>
          </Grid>
        </Grid>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>Generate timesheet</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Employee" value={genEmployee} onChange={(e) => setGenEmployee(e.target.value)}>
              {(employees?.content ?? []).map((emp) => (
                <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} — {emp.firstName} {emp.lastName}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Shift" value={genShift} onChange={(e) => setGenShift(e.target.value)}>
              <MenuItem value="">(default)</MenuItem>
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
            <Button variant="contained" disabled={!genEmployee || generate.isPending} onClick={() => generate.mutate()}>
              Generate
            </Button>
          </Grid>
        </Grid>
        {generate.isError && <Typography color="error" variant="body2" mt={1}>{(generate.error as any)?.response?.data?.message ?? "Generate failed."}</Typography>}
      </Paper>

      <Paper variant="outlined" sx={{ p: 0, borderRadius: 2, mb: 2 }}>
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
            {list.map((t) => (
              <TableRow key={t.id} hover selected={t.id === selectedId}>
                <TableCell>{t.employeeNumber} — {t.employeeName}</TableCell>
                <TableCell><Chip size="small" label={t.status} color={STATUS_COLOR[t.status ?? "DRAFT"] ?? "default"} /></TableCell>
                <TableCell align="right">{t.totalWorkedHours ?? 0}</TableCell>
                <TableCell align="right">{t.totalOtHours ?? 0}</TableCell>
                <TableCell align="right">{t.totalAbsenceDays ?? 0}</TableCell>
                <TableCell align="right"><Button size="small" onClick={() => setSelectedId(t.id ?? null)}>Open</Button></TableCell>
              </TableRow>
            ))}
            {list.length === 0 && (
              <TableRow><TableCell colSpan={6}><Typography variant="body2" color="text.secondary" p={1}>No timesheets for this period. Generate one above.</Typography></TableCell></TableRow>
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
  const editable = timesheet.status === "DRAFT";

  useEffect(() => setDays(timesheet.days), [timesheet]);

  const save = useMutation({
    mutationFn: () => timesheetApi.saveDays(timesheet.id!, days),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timesheet", timesheet.id] });
      qc.invalidateQueries({ queryKey: ["timesheets", timesheet.periodYear, timesheet.periodMonth] });
    },
  });

  const setDay = (idx: number, patch: Partial<TimesheetDay>) =>
    setDays((prev) => prev.map((d, i) => (i === idx ? { ...d, ...patch } : d)));

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
              <TableCell align="right">OT</TableCell>
              <TableCell>Remarks</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {days.map((d, idx) => (
              <TableRow key={d.id ?? d.workDate}>
                <TableCell>{d.workDate}</TableCell>
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
                <TableCell align="right">{d.otHours ?? 0}</TableCell>
                <TableCell>
                  {editable ? (
                    <TextField size="small" value={d.remarks ?? ""} onChange={(e) => setDay(idx, { remarks: e.target.value })} sx={{ minWidth: 140 }} />
                  ) : (d.remarks ?? "")}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>
    </Paper>
  );
}
