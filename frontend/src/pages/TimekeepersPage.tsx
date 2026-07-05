import { useState } from "react";
import {
  Box,
  Button,
  Chip,
  Grid,
  IconButton,
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
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import LoginIcon from "@mui/icons-material/Login";
import LogoutIcon from "@mui/icons-material/Logout";
import EventBusyIcon from "@mui/icons-material/EventBusy";
import ScheduleIcon from "@mui/icons-material/Schedule";
import { employeeApi, projectApi, timekeeperApi } from "../api/resources";
import type { TimekeeperMarkRequest, TimekeeperProject } from "../api/types";

const today = () => new Date().toISOString().slice(0, 10);

export default function TimekeepersPage() {
  const qc = useQueryClient();
  const { data: rows = [] } = useQuery({ queryKey: ["timekeeperProjects"], queryFn: timekeeperApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: employees } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500) });
  const empList = employees?.content ?? [];

  const [employeeId, setEmployeeId] = useState("");
  const [projectId, setProjectId] = useState("");
  const [consoleTk, setConsoleTk] = useState("");
  const [workDate, setWorkDate] = useState(today());
  const [inTimes, setInTimes] = useState<Record<string, string>>({});
  const [outTimes, setOutTimes] = useState<Record<string, string>>({});

  const consoleRows = useQuery({
    queryKey: ["timekeeperConsole", workDate, consoleTk],
    queryFn: () => timekeeperApi.console(workDate, consoleTk || undefined),
    enabled: !!workDate,
  });

  const save = useMutation({
    mutationFn: () => timekeeperApi.create({ employeeId, projectId } as TimekeeperProject),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["timekeeperProjects"] }); setEmployeeId(""); setProjectId(""); },
  });
  const del = useMutation({
    mutationFn: (id: string) => timekeeperApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["timekeeperProjects"] }),
  });
  const mark = useMutation({
    mutationFn: (d: TimekeeperMarkRequest) => timekeeperApi.mark(d),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["timekeeperConsole"] }),
  });

  const submitMark = (employeeId: string, action: TimekeeperMarkRequest["action"]) => {
    mark.mutate({
      employeeId,
      workDate,
      action,
      actualIn: action === "LATE" ? inTimes[employeeId] : undefined,
      actualOut: action === "OUT_CUSTOM" ? outTimes[employeeId] : undefined,
    });
  };

  return (
    <Box>
      <Typography variant="h5" mb={2}>Timekeepers</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>Project access</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={5}>
            <TextField select fullWidth size="small" label="Timekeeper (employee)" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
              {empList.map((emp) => <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} - {emp.firstName} {emp.lastName}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Project" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={3}>
            <Button startIcon={<AddIcon />} variant="contained" disabled={!employeeId || !projectId || save.isPending} onClick={() => save.mutate()}>
              Assign
            </Button>
          </Grid>
        </Grid>
        {save.isError && <Typography color="error" variant="body2" mt={1}>{(save.error as any)?.response?.data?.message ?? "Failed."}</Typography>}
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, mb: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Timekeeper</TableCell>
              <TableCell>Project</TableCell>
              <TableCell align="right" />
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.id} hover>
                <TableCell>{r.employeeNumber} - {r.employeeName}</TableCell>
                <TableCell>{r.projectCode}</TableCell>
                <TableCell align="right"><IconButton size="small" color="error" onClick={() => r.id && del.mutate(r.id)}><DeleteIcon fontSize="small" /></IconButton></TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && <TableRow><TableCell colSpan={3}><Typography variant="body2" color="text.secondary" p={1}>No project access assigned yet.</Typography></TableCell></TableRow>}
          </TableBody>
        </Table>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} alignItems={{ xs: "stretch", md: "center" }} mb={2}>
          <Typography variant="subtitle2" sx={{ minWidth: 160 }}>Daily console</Typography>
          <TextField size="small" type="date" label="Date" InputLabelProps={{ shrink: true }} value={workDate} inputProps={{ max: today() }} onChange={(e) => setWorkDate(e.target.value)} />
          <TextField select size="small" label="Timekeeper" value={consoleTk} onChange={(e) => setConsoleTk(e.target.value)} sx={{ minWidth: 280 }}>
            <MenuItem value="">Current login</MenuItem>
            {empList.map((emp) => <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} - {emp.firstName} {emp.lastName}</MenuItem>)}
          </TextField>
        </Stack>
        {mark.isError && <Typography color="error" variant="body2" mb={1}>{(mark.error as any)?.response?.data?.message ?? "Failed to mark attendance."}</Typography>}
        {consoleRows.isError && <Typography color="error" variant="body2" mb={1}>{(consoleRows.error as any)?.response?.data?.message ?? "Failed to load console."}</Typography>}
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Employee</TableCell>
              <TableCell>Shift</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Actual</TableCell>
              <TableCell>Hours</TableCell>
              <TableCell>Inputs</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(consoleRows.data ?? []).map((r) => (
              <TableRow key={r.employeeId} hover>
                <TableCell>{r.employeeNumber} - {r.employeeName}</TableCell>
                <TableCell>{r.shiftCode ?? "-"} {r.plannedIn && r.plannedOut ? `${r.plannedIn} - ${r.plannedOut}` : ""}</TableCell>
                <TableCell>
                  <Stack direction="row" spacing={0.5} alignItems="center">
                    <Chip size="small" label={r.timeTypeCode ?? "-"} color={r.editable ? "success" : "default"} />
                    {!r.editable && <Typography variant="caption" color="text.secondary">{r.blockedReason}</Typography>}
                  </Stack>
                </TableCell>
                <TableCell>{r.actualIn ?? "--:--"} / {r.actualOut ?? "--:--"}</TableCell>
                <TableCell>{r.workedHours ?? 0}h · OT {r.otHours ?? 0}h</TableCell>
                <TableCell>
                  <Stack direction="row" spacing={1}>
                    <TextField size="small" type="time" label="In" InputLabelProps={{ shrink: true }} value={inTimes[r.employeeId] ?? ""} onChange={(e) => setInTimes({ ...inTimes, [r.employeeId]: e.target.value })} sx={{ width: 110 }} />
                    <TextField size="small" type="time" label="Out" InputLabelProps={{ shrink: true }} value={outTimes[r.employeeId] ?? ""} onChange={(e) => setOutTimes({ ...outTimes, [r.employeeId]: e.target.value })} sx={{ width: 110 }} />
                  </Stack>
                </TableCell>
                <TableCell align="right">
                  <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                    <Button size="small" startIcon={<LoginIcon />} disabled={!r.editable || mark.isPending} onClick={() => submitMark(r.employeeId, "ATTEND")}>Attend</Button>
                    <Button size="small" startIcon={<ScheduleIcon />} disabled={!r.editable || mark.isPending || !inTimes[r.employeeId]} onClick={() => submitMark(r.employeeId, "LATE")}>Late</Button>
                    <Button size="small" startIcon={<LogoutIcon />} disabled={!r.editable || mark.isPending} onClick={() => submitMark(r.employeeId, "CHECK_OUT")}>Out</Button>
                    <Button size="small" disabled={!r.editable || mark.isPending || !outTimes[r.employeeId]} onClick={() => submitMark(r.employeeId, "OUT_CUSTOM")}>Custom out</Button>
                    <Button size="small" color="error" startIcon={<EventBusyIcon />} disabled={!r.editable || mark.isPending} onClick={() => submitMark(r.employeeId, "ABSENT")}>Absent</Button>
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
            {(consoleRows.data ?? []).length === 0 && <TableRow><TableCell colSpan={7}><Typography variant="body2" color="text.secondary" p={1}>No employees assigned to this timekeeper.</Typography></TableCell></TableRow>}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}
