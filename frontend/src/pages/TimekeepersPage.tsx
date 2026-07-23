import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  Divider,
  Grid,
  IconButton,
  InputAdornment,
  MenuItem,
  Paper,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import AssignmentIndIcon from "@mui/icons-material/AssignmentInd";
import ChecklistIcon from "@mui/icons-material/Checklist";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import EventBusyIcon from "@mui/icons-material/EventBusy";
import HelpOutlineIcon from "@mui/icons-material/HelpOutline";
import LoginIcon from "@mui/icons-material/Login";
import LogoutIcon from "@mui/icons-material/Logout";
import ManageAccountsIcon from "@mui/icons-material/ManageAccounts";
import SearchIcon from "@mui/icons-material/Search";
import ScheduleIcon from "@mui/icons-material/Schedule";
import { employeeApi, projectApi, timekeeperApi } from "../api/resources";
import { useAuth } from "../auth/AuthContext";
import type { Employee, TimekeeperDay, TimekeeperMarkRequest, TimekeeperProject } from "../api/types";

const today = () => new Date().toISOString().slice(0, 10);

const employeeLabel = (emp: Pick<Employee, "employeeNumber" | "firstName" | "lastName">) =>
  `${emp.employeeNumber ?? ""} - ${emp.firstName ?? ""} ${emp.lastName ?? ""}`.trim();

const matchesEmployeeSearch = (emp: Employee, term: string) => {
  const q = term.trim().toLowerCase();
  if (!q) return true;
  return `${emp.employeeNumber ?? ""} ${emp.firstName ?? ""} ${emp.lastName ?? ""} ${emp.jobTitle ?? ""} ${emp.payStatus ?? ""}`
    .toLowerCase()
    .includes(q);
};

const summaryNumber = (value: number | undefined) => value ?? 0;

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
  const [assignmentTab, setAssignmentTab] = useState<"unassigned" | "assigned">("unassigned");
  const [employeeSearch, setEmployeeSearch] = useState("");
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
  const consoleRows = useQuery({
    queryKey: ["timekeeperConsole", workDate, consoleTk],
    queryFn: () => timekeeperApi.console(workDate, consoleTk || undefined),
    enabled: !!workDate,
  });

  const empList = employees?.content ?? [];
  const projectTimekeepers = rows.filter((r) => !bulkProjectId || r.projectId === bulkProjectId);
  const projectEmployeeList = projectEmployees?.content ?? [];
  const candidateEmployees = useMemo(
    () => projectEmployeeList.filter((emp) => !emp.timekeeperEmployeeId && matchesEmployeeSearch(emp, employeeSearch)),
    [projectEmployeeList, employeeSearch],
  );
  const assignedEmployees = useMemo(
    () => projectEmployeeList.filter((emp) => !!emp.timekeeperEmployeeId && matchesEmployeeSearch(emp, employeeSearch)),
    [projectEmployeeList, employeeSearch],
  );

  const consoleSummary = useMemo(() => {
    const list = consoleRows.data ?? [];
    return {
      employees: list.length,
      editable: list.filter((r) => r.editable).length,
      blocked: list.filter((r) => !r.editable).length,
      attended: list.filter((r) => !!r.actualIn || !!r.actualOut).length,
    };
  }, [consoleRows.data]);

  const save = useMutation({
    mutationFn: () => timekeeperApi.create({ employeeId, projectId } as TimekeeperProject),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timekeeperProjects"] });
      setEmployeeId("");
      setProjectId("");
    },
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
    mutationFn: (employeeIds: string[]) =>
      employeeIds.length
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
    setEmployeeSearch("");
  }, [bulkProjectId]);

  const allCandidateIds = candidateEmployees.map((emp) => emp.id!).filter(Boolean);
  const allSelected = allCandidateIds.length > 0 && allCandidateIds.every((id) => selectedEmployeeIds.includes(id));
  const toggleAllCandidates = () => setSelectedEmployeeIds(allSelected ? [] : allCandidateIds);
  const toggleCandidate = (id: string) => {
    setSelectedEmployeeIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };
  const allAssignedIds = assignedEmployees.map((emp) => emp.id!).filter(Boolean);
  const allAssignedSelected = allAssignedIds.length > 0 && allAssignedIds.every((id) => selectedAssignedIds.includes(id));
  const toggleAllAssigned = () => setSelectedAssignedIds(allAssignedSelected ? [] : allAssignedIds);
  const toggleAssigned = (id: string) => {
    setSelectedAssignedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  const submitMark = (targetEmployeeId: string, action: TimekeeperMarkRequest["action"]) => {
    mark.mutate({
      employeeId: targetEmployeeId,
      workDate,
      action,
      actualIn: action === "LATE" ? inTimes[targetEmployeeId] : undefined,
      actualOut: action === "OUT_CUSTOM" ? outTimes[targetEmployeeId] : undefined,
    });
  };

  const message = (error: unknown, fallback: string) => (error as any)?.response?.data?.message ?? fallback;

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5">Timekeepers</Typography>
          <Typography variant="body2" color="text.secondary">
            Assign project timekeepers, distribute employees, then record daily attendance from one focused screen.
          </Typography>
        </Box>
        <Chip icon={<HelpOutlineIcon />} label="Pick a project first, then work from left to right." variant="outlined" />
      </Stack>

      <Grid container spacing={1.5} mb={2}>
        <Grid item xs={12} md={4}>
          <Card variant="outlined" sx={{ height: "100%", borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                <ChecklistIcon color="primary" />
                <Typography variant="subtitle2">Daily flow</Typography>
              </Stack>
              <Typography variant="body2" color="text.secondary">
                Select the date, review assigned employees, then mark attend, late, out, or absent.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card variant="outlined" sx={{ height: "100%", borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                <AssignmentIndIcon color="primary" />
                <Typography variant="subtitle2">Employee assignment</Typography>
              </Stack>
              <Typography variant="body2" color="text.secondary">
                Employees assigned to a timekeeper will stop appearing in the unassigned list for that project.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card variant="outlined" sx={{ height: "100%", borderRadius: 2 }}>
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" mb={1}>
                <ManageAccountsIcon color="primary" />
                <Typography variant="subtitle2">Project access</Typography>
              </Stack>
              <Typography variant="body2" color="text.secondary">
                Add timekeepers to projects first, so they can be selected when distributing employees.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
          <Box>
            <Typography variant="h6">Daily attendance console</Typography>
            <Typography variant="body2" color="text.secondary">
              This is the screen a timekeeper uses on tablet or mobile during the day.
            </Typography>
          </Box>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
            <TextField size="small" type="date" label="Date" InputLabelProps={{ shrink: true }} value={workDate} inputProps={{ max: today() }} onChange={(e) => setWorkDate(e.target.value)} />
            {canManage && (
              <TextField select size="small" label="View as timekeeper" value={consoleTk} onChange={(e) => setConsoleTk(e.target.value)} sx={{ minWidth: 260 }}>
                <MenuItem value="">Current login</MenuItem>
                {empList.map((emp) => (
                  <MenuItem key={emp.id} value={emp.id}>{employeeLabel(emp)}</MenuItem>
                ))}
              </TextField>
            )}
          </Stack>
        </Stack>

        <Grid container spacing={1} mb={2}>
          <Grid item xs={6} sm={3}><Metric label="Assigned" value={consoleSummary.employees} /></Grid>
          <Grid item xs={6} sm={3}><Metric label="Editable" value={consoleSummary.editable} /></Grid>
          <Grid item xs={6} sm={3}><Metric label="Marked today" value={consoleSummary.attended} /></Grid>
          <Grid item xs={6} sm={3}><Metric label="Blocked" value={consoleSummary.blocked} /></Grid>
        </Grid>

        {mark.isError && <Alert severity="error" sx={{ mb: 1 }}>{message(mark.error, "Failed to mark attendance.")}</Alert>}
        {consoleRows.isError && <Alert severity="error" sx={{ mb: 1 }}>{message(consoleRows.error, "Failed to load console.")}</Alert>}

        {compact ? (
          <Stack spacing={1.25}>
            {(consoleRows.data ?? []).map((row) => (
              <TimekeeperMobileCard
                key={row.employeeId}
                row={row}
                inTime={inTimes[row.employeeId] ?? ""}
                outTime={outTimes[row.employeeId] ?? ""}
                setInTime={(value) => setInTimes({ ...inTimes, [row.employeeId]: value })}
                setOutTime={(value) => setOutTimes({ ...outTimes, [row.employeeId]: value })}
                isSaving={mark.isPending}
                onMark={submitMark}
              />
            ))}
            {(consoleRows.data ?? []).length === 0 && <EmptyState text="No employees assigned to this timekeeper." />}
          </Stack>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Employee</TableCell>
                <TableCell>Shift</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actual</TableCell>
                <TableCell>Hours</TableCell>
                <TableCell>Manual time</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(consoleRows.data ?? []).map((row) => (
                <TimekeeperDesktopRow
                  key={row.employeeId}
                  row={row}
                  inTime={inTimes[row.employeeId] ?? ""}
                  outTime={outTimes[row.employeeId] ?? ""}
                  setInTime={(value) => setInTimes({ ...inTimes, [row.employeeId]: value })}
                  setOutTime={(value) => setOutTimes({ ...outTimes, [row.employeeId]: value })}
                  isSaving={mark.isPending}
                  onMark={submitMark}
                />
              ))}
              {(consoleRows.data ?? []).length === 0 && (
                <TableRow><TableCell colSpan={7}><EmptyState text="No employees assigned to this timekeeper." /></TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Paper>

      {canManage && (
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
          <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
            <Box>
              <Typography variant="h6">Setup and assignment</Typography>
              <Typography variant="body2" color="text.secondary">
                Use this area for HR/admin setup. Timekeepers only need the daily console above.
              </Typography>
            </Box>
            <Chip label={`${rows.length} project access record(s)`} color="primary" variant="outlined" />
          </Stack>

          <Grid container spacing={2}>
            <Grid item xs={12} lg={4}>
              <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, height: "100%" }}>
                <Typography variant="subtitle2" mb={1}>1. Add project access</Typography>
                <Stack spacing={1.25}>
                  <TextField select fullWidth size="small" label="Timekeeper employee" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
                    {empList.map((emp) => <MenuItem key={emp.id} value={emp.id}>{employeeLabel(emp)}</MenuItem>)}
                  </TextField>
                  <TextField select fullWidth size="small" label="Project" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
                    {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                  </TextField>
                  <Button startIcon={<AddIcon />} variant="contained" disabled={!employeeId || !projectId || save.isPending} onClick={() => save.mutate()}>
                    Assign access
                  </Button>
                  {save.isError && <Alert severity="error">{message(save.error, "Failed.")}</Alert>}
                </Stack>
                <Divider sx={{ my: 1.5 }} />
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
                        <TableCell align="right">
                          <IconButton size="small" color="error" onClick={() => r.id && del.mutate(r.id)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                    {rows.length === 0 && <TableRow><TableCell colSpan={3}><EmptyState text="No project access assigned yet." /></TableCell></TableRow>}
                  </TableBody>
                </Table>
              </Paper>
            </Grid>

            <Grid item xs={12} lg={8}>
              <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
                <Typography variant="subtitle2" mb={1}>2. Distribute employees</Typography>
                <Grid container spacing={1.25} alignItems="center">
                  <Grid item xs={12} md={4}>
                    <TextField select fullWidth size="small" label="Project" value={bulkProjectId} onChange={(e) => setBulkProjectId(e.target.value)}>
                      {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                    </TextField>
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <TextField select fullWidth size="small" label="Assign to timekeeper" value={bulkTimekeeperId} onChange={(e) => setBulkTimekeeperId(e.target.value)}>
                      {projectTimekeepers.map((tk) => <MenuItem key={tk.id} value={tk.employeeId}>{tk.employeeNumber} - {tk.employeeName}</MenuItem>)}
                    </TextField>
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <Button fullWidth variant="contained" disabled={!bulkProjectId || !bulkTimekeeperId || candidateEmployees.length === 0 || bulkAssign.isPending} onClick={() => bulkAssign.mutate([])}>
                      Assign all unassigned
                    </Button>
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Search employees in selected project"
                      value={employeeSearch}
                      onChange={(e) => setEmployeeSearch(e.target.value)}
                      InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
                    />
                  </Grid>
                </Grid>
                {bulkProjectId && projectTimekeepers.length === 0 && (
                  <Alert severity="warning" sx={{ mt: 1 }}>No timekeepers are assigned to this project. Add project access first.</Alert>
                )}

                <Tabs value={assignmentTab} onChange={(_, value) => setAssignmentTab(value)} sx={{ mt: 1.5 }}>
                  <Tab value="unassigned" label={`Unassigned (${candidateEmployees.length})`} />
                  <Tab value="assigned" label={`Assigned (${assignedEmployees.length})`} />
                </Tabs>

                {bulkProjectId && assignmentTab === "unassigned" && (
                  <Box mt={1}>
                    <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1} mb={1}>
                      <Typography variant="body2" color="text.secondary">Select employees or assign everyone still unassigned.</Typography>
                      <Button size="small" variant="outlined" disabled={!bulkTimekeeperId || selectedEmployeeIds.length === 0 || bulkAssign.isPending} onClick={() => bulkAssign.mutate(selectedEmployeeIds)}>
                        Assign selected ({selectedEmployeeIds.length})
                      </Button>
                    </Stack>
                    <EmployeeAssignmentTable
                      mode="unassigned"
                      rows={candidateEmployees}
                      selectedIds={selectedEmployeeIds}
                      allSelected={allSelected}
                      onToggleAll={toggleAllCandidates}
                      onToggle={toggleCandidate}
                    />
                  </Box>
                )}

                {bulkProjectId && assignmentTab === "assigned" && (
                  <Box mt={1}>
                    <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1} mb={1}>
                      <Typography variant="body2" color="text.secondary">Move employees to another timekeeper or remove the assignment.</Typography>
                      <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
                        <TextField select size="small" label="Move to" value={moveTimekeeperId} onChange={(e) => setMoveTimekeeperId(e.target.value)} sx={{ minWidth: 220 }}>
                          {projectTimekeepers.map((tk) => <MenuItem key={tk.id} value={tk.employeeId}>{tk.employeeNumber} - {tk.employeeName}</MenuItem>)}
                        </TextField>
                        <Button size="small" variant="outlined" disabled={!moveTimekeeperId || selectedAssignedIds.length === 0 || moveAssign.isPending} onClick={() => moveAssign.mutate(selectedAssignedIds)}>
                          Move ({selectedAssignedIds.length})
                        </Button>
                        <Button size="small" color="error" variant="outlined" disabled={selectedAssignedIds.length === 0 || clearAssign.isPending} onClick={() => clearAssign.mutate(selectedAssignedIds)}>
                          Remove
                        </Button>
                      </Stack>
                    </Stack>
                    <EmployeeAssignmentTable
                      mode="assigned"
                      rows={assignedEmployees}
                      selectedIds={selectedAssignedIds}
                      allSelected={allAssignedSelected}
                      onToggleAll={toggleAllAssigned}
                      onToggle={toggleAssigned}
                    />
                  </Box>
                )}

                {bulkAssign.isSuccess && <Alert severity="success" sx={{ mt: 1 }}>Updated {bulkAssign.data?.updated ?? 0} active employee(s).</Alert>}
                {bulkAssign.isError && <Alert severity="error" sx={{ mt: 1 }}>{message(bulkAssign.error, "Failed.")}</Alert>}
                {moveAssign.isSuccess && <Alert severity="success" sx={{ mt: 1 }}>Moved {moveAssign.data?.updated ?? 0} employee(s).</Alert>}
                {moveAssign.isError && <Alert severity="error" sx={{ mt: 1 }}>{message(moveAssign.error, "Move failed.")}</Alert>}
                {clearAssign.isSuccess && <Alert severity="success" sx={{ mt: 1 }}>Removed {clearAssign.data?.updated ?? 0} employee(s).</Alert>}
                {clearAssign.isError && <Alert severity="error" sx={{ mt: 1 }}>{message(clearAssign.error, "Remove failed.")}</Alert>}
              </Paper>
            </Grid>
          </Grid>
        </Paper>
      )}
    </Box>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 1.5 }}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="h6">{value}</Typography>
    </Paper>
  );
}

function EmptyState({ text }: { text: string }) {
  return <Typography variant="body2" color="text.secondary" p={1}>{text}</Typography>;
}

function EmployeeAssignmentTable({
  mode,
  rows,
  selectedIds,
  allSelected,
  onToggleAll,
  onToggle,
}: {
  mode: "assigned" | "unassigned";
  rows: Employee[];
  selectedIds: string[];
  allSelected: boolean;
  onToggleAll: () => void;
  onToggle: (id: string) => void;
}) {
  return (
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell padding="checkbox">
            <Checkbox size="small" checked={allSelected} indeterminate={selectedIds.length > 0 && !allSelected} onChange={onToggleAll} />
          </TableCell>
          <TableCell>Employee</TableCell>
          {mode === "assigned" && <TableCell>Current timekeeper</TableCell>}
          <TableCell>Job title</TableCell>
          <TableCell>Pay</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {rows.map((emp) => (
          <TableRow key={emp.id} hover>
            <TableCell padding="checkbox">
              <Checkbox size="small" checked={!!emp.id && selectedIds.includes(emp.id)} onChange={() => emp.id && onToggle(emp.id)} />
            </TableCell>
            <TableCell>{employeeLabel(emp)}</TableCell>
            {mode === "assigned" && <TableCell>{emp.timekeeperName ?? "-"}</TableCell>}
            <TableCell>{emp.jobTitle ?? "-"}</TableCell>
            <TableCell>{emp.payStatus ?? "-"}</TableCell>
          </TableRow>
        ))}
        {rows.length === 0 && (
          <TableRow>
            <TableCell colSpan={mode === "assigned" ? 5 : 4}><EmptyState text={mode === "assigned" ? "No assigned employees match the current filter." : "No unassigned active employees match the current filter."} /></TableCell>
          </TableRow>
        )}
      </TableBody>
    </Table>
  );
}

function TimekeeperMobileCard({
  row,
  inTime,
  outTime,
  setInTime,
  setOutTime,
  isSaving,
  onMark,
}: {
  row: TimekeeperDay;
  inTime: string;
  outTime: string;
  setInTime: (value: string) => void;
  setOutTime: (value: string) => void;
  isSaving: boolean;
  onMark: (employeeId: string, action: TimekeeperMarkRequest["action"]) => void;
}) {
  return (
    <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 1.5 }}>
      <Stack spacing={1}>
        <Stack direction="row" justifyContent="space-between" gap={1}>
          <Box>
            <Typography variant="subtitle2">{row.employeeNumber} - {row.employeeName}</Typography>
            <Typography variant="caption" color="text.secondary">{shiftText(row)}</Typography>
          </Box>
          <Chip size="small" label={row.timeTypeCode ?? "-"} color={row.editable ? "success" : "default"} />
        </Stack>
        {!row.editable && <Typography variant="caption" color="text.secondary">{row.blockedReason}</Typography>}
        <Grid container spacing={1}>
          <Grid item xs={6}><Typography variant="caption" color="text.secondary">Actual</Typography><Typography variant="body2">{row.actualIn ?? "--:--"} / {row.actualOut ?? "--:--"}</Typography></Grid>
          <Grid item xs={6}><Typography variant="caption" color="text.secondary">Hours</Typography><Typography variant="body2">{summaryNumber(row.workedHours)}h - OT {summaryNumber(row.otHours)}h</Typography></Grid>
          <Grid item xs={6}><TextField fullWidth size="small" type="time" label="Late in" InputLabelProps={{ shrink: true }} value={inTime} onChange={(e) => setInTime(e.target.value)} /></Grid>
          <Grid item xs={6}><TextField fullWidth size="small" type="time" label="Custom out" InputLabelProps={{ shrink: true }} value={outTime} onChange={(e) => setOutTime(e.target.value)} /></Grid>
        </Grid>
        <ActionButtons row={row} inTime={inTime} outTime={outTime} isSaving={isSaving} onMark={onMark} />
      </Stack>
    </Paper>
  );
}

function TimekeeperDesktopRow({
  row,
  inTime,
  outTime,
  setInTime,
  setOutTime,
  isSaving,
  onMark,
}: {
  row: TimekeeperDay;
  inTime: string;
  outTime: string;
  setInTime: (value: string) => void;
  setOutTime: (value: string) => void;
  isSaving: boolean;
  onMark: (employeeId: string, action: TimekeeperMarkRequest["action"]) => void;
}) {
  return (
    <TableRow hover>
      <TableCell>{row.employeeNumber} - {row.employeeName}</TableCell>
      <TableCell>{shiftText(row)}</TableCell>
      <TableCell>
        <Stack direction="row" spacing={0.5} alignItems="center">
          <Chip size="small" label={row.timeTypeCode ?? "-"} color={row.editable ? "success" : "default"} />
          {!row.editable && <Typography variant="caption" color="text.secondary">{row.blockedReason}</Typography>}
        </Stack>
      </TableCell>
      <TableCell>{row.actualIn ?? "--:--"} / {row.actualOut ?? "--:--"}</TableCell>
      <TableCell>{summaryNumber(row.workedHours)}h - OT {summaryNumber(row.otHours)}h</TableCell>
      <TableCell>
        <Stack direction="row" spacing={1}>
          <TextField size="small" type="time" label="Late in" InputLabelProps={{ shrink: true }} value={inTime} onChange={(e) => setInTime(e.target.value)} sx={{ width: 115 }} />
          <TextField size="small" type="time" label="Custom out" InputLabelProps={{ shrink: true }} value={outTime} onChange={(e) => setOutTime(e.target.value)} sx={{ width: 125 }} />
        </Stack>
      </TableCell>
      <TableCell align="right">
        <ActionButtons row={row} inTime={inTime} outTime={outTime} isSaving={isSaving} onMark={onMark} />
      </TableCell>
    </TableRow>
  );
}

function ActionButtons({
  row,
  inTime,
  outTime,
  isSaving,
  onMark,
}: {
  row: TimekeeperDay;
  inTime: string;
  outTime: string;
  isSaving: boolean;
  onMark: (employeeId: string, action: TimekeeperMarkRequest["action"]) => void;
}) {
  return (
    <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap justifyContent={{ xs: "flex-start", md: "flex-end" }}>
      <Button size="small" startIcon={<LoginIcon />} disabled={!row.editable || isSaving} onClick={() => onMark(row.employeeId, "ATTEND")}>Attend</Button>
      <Button size="small" startIcon={<ScheduleIcon />} disabled={!row.editable || isSaving || !inTime} onClick={() => onMark(row.employeeId, "LATE")}>Late</Button>
      <Button size="small" startIcon={<LogoutIcon />} disabled={!row.editable || isSaving} onClick={() => onMark(row.employeeId, "CHECK_OUT")}>Out</Button>
      <Button size="small" disabled={!row.editable || isSaving || !outTime} onClick={() => onMark(row.employeeId, "OUT_CUSTOM")}>Custom out</Button>
      <Button size="small" color="error" startIcon={<EventBusyIcon />} disabled={!row.editable || isSaving} onClick={() => onMark(row.employeeId, "ABSENT")}>Absent</Button>
    </Stack>
  );
}

function shiftText(row: TimekeeperDay) {
  const shift = row.shiftCode ?? "-";
  const hours = row.plannedIn && row.plannedOut ? `${row.plannedIn} - ${row.plannedOut}` : "";
  return `${shift} ${hours}`.trim();
}
