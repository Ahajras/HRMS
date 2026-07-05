import { useEffect, useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  Chip,
  Divider,
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
  useMediaQuery,
  useTheme,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import LoginIcon from "@mui/icons-material/Login";
import LogoutIcon from "@mui/icons-material/Logout";
import EventBusyIcon from "@mui/icons-material/EventBusy";
import ScheduleIcon from "@mui/icons-material/Schedule";
import { employeeApi, projectApi, timekeeperApi } from "../api/resources";
import { useAuth } from "../auth/AuthContext";
import type { TimekeeperMarkRequest, TimekeeperProject } from "../api/types";

const today = () => new Date().toISOString().slice(0, 10);

export default function TimekeepersPage() {
  const qc = useQueryClient();
  const { hasAuthority } = useAuth();
  const theme = useTheme();
  const compact = useMediaQuery(theme.breakpoints.down("md"));
  const canManage = hasAuthority("employee.write") || hasAuthority("employee.read");
  const [employeeId, setEmployeeId] = useState("");
  const [projectId, setProjectId] = useState("");
  const [bulkProjectId, setBulkProjectId] = useState("");
  const [bulkTimekeeperId, setBulkTimekeeperId] = useState("");
  const [assignView, setAssignView] = useState<"unassigned" | "assigned">("unassigned");
  const [selectedEmployeeIds, setSelectedEmployeeIds] = useState<string[]>([]);
  const [selectedAssignedIds, setSelectedAssignedIds] = useState<string[]>([]);
  const [moveTimekeeperId, setMoveTimekeeperId] = useState("");
  const [consoleTk, setConsoleTk] = useState("");
  const [workDate, setWorkDate] = useState(today());
  const [inTimes, setInTimes] = useState<Record<string, string>>({});
  const [outTimes, setOutTimes] = useState<Record<string, string>>({});
  const { data: rows = [] } = useQuery({ queryKey: ["timekeeperProjects"], queryFn: timekeeperApi.list, enabled: canManage });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list, enabled: canManage });
  const { data: employees } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500), enabled: canManage });
  const { data: projectEmployees } = useQuery({
    queryKey: ["timekeeperProjectCandidates", bulkProjectId],
    queryFn: () => employeeApi.list(0, 1000, undefined, undefined, bulkProjectId, { activeOnly: true }),
    enabled: canManage && !!bulkProjectId,
  });
  const empList = employees?.content ?? [];
  const projectTimekeepers = rows.filter((r) => !bulkProjectId || r.projectId === bulkProjectId);
  const candidateEmployees = (projectEmployees?.content ?? []).filter((emp) => !emp.timekeeperEmployeeId);
  const assignedEmployees = (projectEmployees?.content ?? []).filter((emp) => !!emp.timekeeperEmployeeId);

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
  const bulkAssign = useMutation({
    mutationFn: (employeeIds: string[]) => employeeIds.length
      ? employeeApi.assignTimekeeperByEmployees(employeeIds, bulkTimekeeperId, bulkProjectId)
      : employeeApi.assignTimekeeperByProject(bulkProjectId, bulkTimekeeperId),
    onSuccess: () => {
      setSelectedEmployeeIds([]);
      qc.invalidateQueries({ queryKey: ["employeesAll"] });
      qc.invalidateQueries({ queryKey: ["timekeeperConsole"] });
      qc.invalidateQueries({ queryKey: ["timekeeperProjectCandidates"] });
    },
  });
  const moveAssign = useMutation({
    mutationFn: (employeeIds: string[]) => employeeApi.moveTimekeeperByEmployees(employeeIds, moveTimekeeperId, bulkProjectId),
    onSuccess: () => {
      setSelectedAssignedIds([]);
      qc.invalidateQueries({ queryKey: ["employeesAll"] });
      qc.invalidateQueries({ queryKey: ["timekeeperProjectCandidates"] });
      qc.invalidateQueries({ queryKey: ["timekeeperConsole"] });
    },
  });
  const clearAssign = useMutation({
    mutationFn: (employeeIds: string[]) => employeeApi.clearTimekeeperByEmployees(employeeIds, bulkProjectId),
    onSuccess: () => {
      setSelectedAssignedIds([]);
      qc.invalidateQueries({ queryKey: ["employeesAll"] });
      qc.invalidateQueries({ queryKey: ["timekeeperProjectCandidates"] });
      qc.invalidateQueries({ queryKey: ["timekeeperConsole"] });
    },
  });

  useEffect(() => {
    if (!bulkProjectId) {
      const nfe = projects.find((p) => p.code?.toUpperCase() === "NFE");
      if (nfe?.id) setBulkProjectId(nfe.id);
    }
    if (!bulkTimekeeperId) {
      const tk = projectTimekeepers.find((t) => t.employeeNumber === "77677") ?? projectTimekeepers[0];
      if (tk?.employeeId) setBulkTimekeeperId(tk.employeeId);
    }
  }, [bulkProjectId, bulkTimekeeperId, projects, projectTimekeepers]);

  useEffect(() => {
    setSelectedEmployeeIds([]);
    setSelectedAssignedIds([]);
    setMoveTimekeeperId("");
    setBulkTimekeeperId("");
  }, [bulkProjectId]);

  const allCandidateIds = candidateEmployees.map((emp) => emp.id!).filter(Boolean);
  const allSelected = allCandidateIds.length > 0 && allCandidateIds.every((id) => selectedEmployeeIds.includes(id));
  const toggleAllCandidates = () => setSelectedEmployeeIds(allSelected ? [] : allCandidateIds);
  const toggleCandidate = (id: string) => {
    setSelectedEmployeeIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  };
  const allAssignedIds = assignedEmployees.map((emp) => emp.id!).filter(Boolean);
  const allAssignedSelected = allAssignedIds.length > 0 && allAssignedIds.every((id) => selectedAssignedIds.includes(id));
  const toggleAllAssigned = () => setSelectedAssignedIds(allAssignedSelected ? [] : allAssignedIds);
  const toggleAssigned = (id: string) => {
    setSelectedAssignedIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  };

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

      {canManage && (
        <>
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

          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
            <Typography variant="subtitle2" gutterBottom>Bulk employee assignment</Typography>
            <Grid container spacing={1.5} alignItems="center">
              <Grid item xs={12} sm={5}>
                <TextField select fullWidth size="small" label="Project employees" value={bulkProjectId} onChange={(e) => setBulkProjectId(e.target.value)}>
                  {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={5}>
                <TextField select fullWidth size="small" label="Assign timekeeper" value={bulkTimekeeperId} onChange={(e) => setBulkTimekeeperId(e.target.value)}>
                  {projectTimekeepers.map((tk) => <MenuItem key={tk.id} value={tk.employeeId}>{tk.employeeNumber} - {tk.employeeName}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={2}>
                <Button fullWidth variant="contained" disabled={!bulkProjectId || !bulkTimekeeperId || candidateEmployees.length === 0 || bulkAssign.isPending} onClick={() => bulkAssign.mutate([])}>
                  Assign all
                </Button>
              </Grid>
            </Grid>
            {bulkProjectId && projectTimekeepers.length === 0 && (
              <Typography color="warning.main" variant="body2" mt={1}>
                No timekeepers are assigned to this project. Add project access first.
              </Typography>
            )}
            <Stack direction="row" spacing={1} mt={1.5}>
              <Button size="small" variant={assignView === "unassigned" ? "contained" : "outlined"} onClick={() => setAssignView("unassigned")}>
                Unassigned ({candidateEmployees.length})
              </Button>
              <Button size="small" variant={assignView === "assigned" ? "contained" : "outlined"} onClick={() => setAssignView("assigned")}>
                Assigned ({assignedEmployees.length})
              </Button>
            </Stack>
            {bulkProjectId && assignView === "unassigned" && (
              <Box mt={1.5}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                  <Typography variant="body2" color="text.secondary">
                    Unassigned employees: {candidateEmployees.length}
                  </Typography>
                  <Button size="small" variant="outlined" disabled={!bulkTimekeeperId || selectedEmployeeIds.length === 0 || bulkAssign.isPending} onClick={() => bulkAssign.mutate(selectedEmployeeIds)}>
                    Assign selected ({selectedEmployeeIds.length})
                  </Button>
                </Stack>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell padding="checkbox">
                        <Checkbox size="small" checked={allSelected} indeterminate={selectedEmployeeIds.length > 0 && !allSelected} onChange={toggleAllCandidates} />
                      </TableCell>
                      <TableCell>Employee</TableCell>
                      <TableCell>Job title</TableCell>
                      <TableCell>Pay</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {candidateEmployees.map((emp) => (
                      <TableRow key={emp.id} hover>
                        <TableCell padding="checkbox">
                          <Checkbox size="small" checked={!!emp.id && selectedEmployeeIds.includes(emp.id)} onChange={() => emp.id && toggleCandidate(emp.id)} />
                        </TableCell>
                        <TableCell>{emp.employeeNumber} - {emp.firstName} {emp.lastName}</TableCell>
                        <TableCell>{emp.jobTitle ?? "-"}</TableCell>
                        <TableCell>{emp.payStatus ?? "-"}</TableCell>
                      </TableRow>
                    ))}
                    {candidateEmployees.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4}><Typography variant="body2" color="text.secondary" p={1}>No unassigned active employees for this project.</Typography></TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </Box>
            )}
            {bulkProjectId && assignView === "assigned" && (
              <Box mt={1.5}>
                <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", md: "center" }} spacing={1} mb={1}>
                  <Typography variant="body2" color="text.secondary">
                    Assigned employees: {assignedEmployees.length}
                  </Typography>
                  <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                    <TextField select size="small" label="Move to timekeeper" value={moveTimekeeperId} onChange={(e) => setMoveTimekeeperId(e.target.value)} sx={{ minWidth: 240 }}>
                      {projectTimekeepers.map((tk) => <MenuItem key={tk.id} value={tk.employeeId}>{tk.employeeNumber} - {tk.employeeName}</MenuItem>)}
                    </TextField>
                    <Button size="small" variant="outlined" disabled={!moveTimekeeperId || selectedAssignedIds.length === 0 || moveAssign.isPending} onClick={() => moveAssign.mutate(selectedAssignedIds)}>
                      Move selected ({selectedAssignedIds.length})
                    </Button>
                    <Button size="small" color="error" variant="outlined" disabled={selectedAssignedIds.length === 0 || clearAssign.isPending} onClick={() => clearAssign.mutate(selectedAssignedIds)}>
                      Remove selected
                    </Button>
                  </Stack>
                </Stack>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell padding="checkbox">
                        <Checkbox size="small" checked={allAssignedSelected} indeterminate={selectedAssignedIds.length > 0 && !allAssignedSelected} onChange={toggleAllAssigned} />
                      </TableCell>
                      <TableCell>Employee</TableCell>
                      <TableCell>Current timekeeper</TableCell>
                      <TableCell>Job title</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {assignedEmployees.map((emp) => (
                      <TableRow key={emp.id} hover>
                        <TableCell padding="checkbox">
                          <Checkbox size="small" checked={!!emp.id && selectedAssignedIds.includes(emp.id)} onChange={() => emp.id && toggleAssigned(emp.id)} />
                        </TableCell>
                        <TableCell>{emp.employeeNumber} - {emp.firstName} {emp.lastName}</TableCell>
                        <TableCell>{emp.timekeeperName ?? "-"}</TableCell>
                        <TableCell>{emp.jobTitle ?? "-"}</TableCell>
                      </TableRow>
                    ))}
                    {assignedEmployees.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4}><Typography variant="body2" color="text.secondary" p={1}>No assigned employees for this project.</Typography></TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </Box>
            )}
            {bulkAssign.isSuccess && <Typography color="success.main" variant="body2" mt={1}>Updated {bulkAssign.data?.updated ?? 0} active employee(s).</Typography>}
            {bulkAssign.isError && <Typography color="error" variant="body2" mt={1}>{(bulkAssign.error as any)?.response?.data?.message ?? "Failed."}</Typography>}
            {moveAssign.isSuccess && <Typography color="success.main" variant="body2" mt={1}>Moved {moveAssign.data?.updated ?? 0} employee(s).</Typography>}
            {moveAssign.isError && <Typography color="error" variant="body2" mt={1}>{(moveAssign.error as any)?.response?.data?.message ?? "Move failed."}</Typography>}
            {clearAssign.isSuccess && <Typography color="success.main" variant="body2" mt={1}>Removed {clearAssign.data?.updated ?? 0} employee(s).</Typography>}
            {clearAssign.isError && <Typography color="error" variant="body2" mt={1}>{(clearAssign.error as any)?.response?.data?.message ?? "Remove failed."}</Typography>}
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
        </>
      )}

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} alignItems={{ xs: "stretch", md: "center" }} mb={2}>
          <Typography variant="subtitle2" sx={{ minWidth: 160 }}>Daily console</Typography>
          <TextField size="small" type="date" label="Date" InputLabelProps={{ shrink: true }} value={workDate} inputProps={{ max: today() }} onChange={(e) => setWorkDate(e.target.value)} />
          {canManage && (
            <TextField select size="small" label="Timekeeper" value={consoleTk} onChange={(e) => setConsoleTk(e.target.value)} sx={{ minWidth: 280 }}>
              <MenuItem value="">Current login</MenuItem>
              {empList.map((emp) => <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} - {emp.firstName} {emp.lastName}</MenuItem>)}
            </TextField>
          )}
        </Stack>
        {mark.isError && <Typography color="error" variant="body2" mb={1}>{(mark.error as any)?.response?.data?.message ?? "Failed to mark attendance."}</Typography>}
        {consoleRows.isError && <Typography color="error" variant="body2" mb={1}>{(consoleRows.error as any)?.response?.data?.message ?? "Failed to load console."}</Typography>}
        {compact && (
          <Stack spacing={1.25}>
            {(consoleRows.data ?? []).map((r) => (
              <Paper key={r.employeeId} variant="outlined" sx={{ p: 1.25, borderRadius: 1 }}>
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" gap={1}>
                    <Box>
                      <Typography variant="subtitle2">{r.employeeNumber} - {r.employeeName}</Typography>
                      <Typography variant="caption" color="text.secondary">{r.shiftCode ?? "-"} {r.plannedIn && r.plannedOut ? `${r.plannedIn} - ${r.plannedOut}` : ""}</Typography>
                    </Box>
                    <Chip size="small" label={r.timeTypeCode ?? "-"} color={r.editable ? "success" : "default"} />
                  </Stack>
                  {!r.editable && <Typography variant="caption" color="text.secondary">{r.blockedReason}</Typography>}
                  <Divider />
                  <Grid container spacing={1}>
                    <Grid item xs={6}><Typography variant="caption" color="text.secondary">Actual</Typography><Typography variant="body2">{r.actualIn ?? "--:--"} / {r.actualOut ?? "--:--"}</Typography></Grid>
                    <Grid item xs={6}><Typography variant="caption" color="text.secondary">Hours</Typography><Typography variant="body2">{r.workedHours ?? 0}h · OT {r.otHours ?? 0}h</Typography></Grid>
                    <Grid item xs={6}><TextField fullWidth size="small" type="time" label="In" InputLabelProps={{ shrink: true }} value={inTimes[r.employeeId] ?? ""} onChange={(e) => setInTimes({ ...inTimes, [r.employeeId]: e.target.value })} /></Grid>
                    <Grid item xs={6}><TextField fullWidth size="small" type="time" label="Out" InputLabelProps={{ shrink: true }} value={outTimes[r.employeeId] ?? ""} onChange={(e) => setOutTimes({ ...outTimes, [r.employeeId]: e.target.value })} /></Grid>
                  </Grid>
                  <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
                    <Button size="small" startIcon={<LoginIcon />} disabled={!r.editable || mark.isPending} onClick={() => submitMark(r.employeeId, "ATTEND")}>Attend</Button>
                    <Button size="small" startIcon={<ScheduleIcon />} disabled={!r.editable || mark.isPending || !inTimes[r.employeeId]} onClick={() => submitMark(r.employeeId, "LATE")}>Late</Button>
                    <Button size="small" startIcon={<LogoutIcon />} disabled={!r.editable || mark.isPending} onClick={() => submitMark(r.employeeId, "CHECK_OUT")}>Out</Button>
                    <Button size="small" disabled={!r.editable || mark.isPending || !outTimes[r.employeeId]} onClick={() => submitMark(r.employeeId, "OUT_CUSTOM")}>Custom out</Button>
                    <Button size="small" color="error" startIcon={<EventBusyIcon />} disabled={!r.editable || mark.isPending} onClick={() => submitMark(r.employeeId, "ABSENT")}>Absent</Button>
                  </Stack>
                </Stack>
              </Paper>
            ))}
            {(consoleRows.data ?? []).length === 0 && <Typography variant="body2" color="text.secondary" p={1}>No employees assigned to this timekeeper.</Typography>}
          </Stack>
        )}
        <Table size="small" sx={{ display: compact ? "none" : "table" }}>
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
