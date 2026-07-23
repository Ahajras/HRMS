import { useState } from "react";
import type { ReactNode } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  Grid,
  IconButton,
  InputAdornment,
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
import AssignmentTurnedInIcon from "@mui/icons-material/AssignmentTurnedIn";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import EventNoteIcon from "@mui/icons-material/EventNote";
import GroupsIcon from "@mui/icons-material/Groups";
import SearchIcon from "@mui/icons-material/Search";
import { employeeApi, employeeShiftApi, projectApi, shiftApi } from "../api/resources";
import type { EmployeeShift } from "../api/types";

const today = () => new Date().toISOString().slice(0, 10);
const EMPTY: EmployeeShift = { employeeId: "", shiftId: "", effectiveFrom: today() };

export default function RosterPage() {
  const qc = useQueryClient();
  const [form, setForm] = useState<EmployeeShift>(EMPTY);
  const [projectId, setProjectId] = useState("");
  const [bulkShift, setBulkShift] = useState("");
  const [bulkDate, setBulkDate] = useState(today());
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [bulkMsg, setBulkMsg] = useState<string | null>(null);
  const [q, setQ] = useState("");

  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: employees } = useQuery({
    queryKey: ["employeesByProject", projectId],
    queryFn: () => employeeApi.list(0, 500, undefined, undefined, projectId, { assignedOnly: true }),
    enabled: !!projectId,
  });
  const { data: shifts = [] } = useQuery({
    queryKey: ["shifts", projectId],
    queryFn: () => shiftApi.list(projectId),
    enabled: !!projectId,
  });
  const { data: roster = [] } = useQuery({ queryKey: ["roster"], queryFn: () => employeeShiftApi.list() });

  const save = useMutation({
    mutationFn: (r: EmployeeShift) => (r.id ? employeeShiftApi.update(r.id, r) : employeeShiftApi.create(r)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["roster"] });
      setForm(EMPTY);
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => employeeShiftApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["roster"] }),
  });
  const bulk = useMutation({
    mutationFn: () => employeeShiftApi.bulkAssign(bulkShift, bulkDate, Array.from(selected)),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["roster"] });
      setBulkMsg(`Assigned ${r.created} employee(s).`);
      setSelected(new Set());
    },
  });

  const match = (s: string) => {
    const t = q.trim().toLowerCase();
    return !t || s.toLowerCase().includes(t);
  };
  const empList = (employees?.content ?? []).filter((e) => match(`${e.employeeNumber ?? ""} ${e.firstName ?? ""} ${e.lastName ?? ""}`));
  const filteredRoster = roster.filter((r) =>
    (!projectId || empList.some((e) => e.id === r.employeeId)) &&
    match(`${r.employeeNumber ?? ""} ${r.employeeName ?? ""} ${r.shiftCode ?? ""}`),
  );
  const assignedEmployeeIds = new Set(filteredRoster.map((r) => r.employeeId));
  const notRosteredCount = empList.filter((e) => e.id && !assignedEmployeeIds.has(e.id)).length;

  const toggle = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  const allSelected = empList.length > 0 && empList.every((e) => e.id && selected.has(e.id));
  const toggleAll = () => setSelected(allSelected ? new Set() : new Set(empList.map((e) => e.id!).filter(Boolean)));
  const resetProject = (id: string) => {
    setProjectId(id);
    setSelected(new Set());
    setBulkShift("");
    setForm(EMPTY);
    setQ("");
  };

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5">Shift Roster</Typography>
          <Typography variant="body2" color="text.secondary">
            Assign employees to shifts with effective dates. Timesheet generation reads this roster.
          </Typography>
        </Box>
        <Chip icon={<EventNoteIcon />} label="Pick project, choose shift/date, then assign employees." variant="outlined" />
      </Stack>

      <Grid container spacing={1.5} mb={2}>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<GroupsIcon color="primary" />} title="1. Pick project" text="The employee list and shift list are filtered by project." />
        </Grid>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<EventNoteIcon color="primary" />} title="2. Set shift and date" text="Effective date decides when the employee starts using the selected shift." />
        </Grid>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<AssignmentTurnedInIcon color="primary" />} title="3. Generate timesheets" text="After roster is ready, timesheet generation will pick the correct shift." />
        </Grid>
      </Grid>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.25} alignItems="center">
          <Grid item xs={12} md={4}>
            <TextField select fullWidth size="small" label="Project" value={projectId} onChange={(e) => resetProject(e.target.value)}>
              <MenuItem value="">Pick project</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={8}>
            <TextField
              fullWidth
              size="small"
              label="Search employee or shift"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
            />
          </Grid>
        </Grid>
        {!projectId && (
          <Alert severity="info" sx={{ mt: 1.5 }}>
            Pick a project first. The page will then load that project's employees and matching shifts.
          </Alert>
        )}
        {projectId && shifts.length === 0 && (
          <Alert severity="warning" sx={{ mt: 1.5 }}>
            No shifts found for this project. Create the shift first in Shifts, then come back to assign it.
          </Alert>
        )}
      </Paper>

      {projectId && (
        <Grid container spacing={1.5} mb={2}>
          <Grid item xs={6} md={3}><Metric label="Employees" value={empList.length} /></Grid>
          <Grid item xs={6} md={3}><Metric label="Rostered" value={filteredRoster.length} /></Grid>
          <Grid item xs={6} md={3}><Metric label="Not rostered" value={notRosteredCount} /></Grid>
          <Grid item xs={6} md={3}><Metric label="Selected" value={selected.size} /></Grid>
        </Grid>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} lg={7}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, height: "100%" }}>
            <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1} mb={1.5}>
              <Box>
                <Typography variant="subtitle2">Bulk assign</Typography>
                <Typography variant="body2" color="text.secondary">
                  Best for assigning one shift to many employees at once.
                </Typography>
              </Box>
              <Button variant="contained" disabled={!projectId || !bulkShift || selected.size === 0 || bulk.isPending} onClick={() => bulk.mutate()}>
                Assign {selected.size} selected
              </Button>
            </Stack>

            <Grid container spacing={1.25} alignItems="center" mb={1}>
              <Grid item xs={12} md={7}>
                <TextField select fullWidth size="small" label="Shift" value={bulkShift} disabled={!projectId} onChange={(e) => setBulkShift(e.target.value)}>
                  {shifts.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} - {s.name}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} md={5}>
                <TextField fullWidth size="small" type="date" label="Effective from" InputLabelProps={{ shrink: true }} value={bulkDate} onChange={(e) => setBulkDate(e.target.value)} />
              </Grid>
            </Grid>

            {bulkMsg && <Alert severity="success" sx={{ mb: 1 }} onClose={() => setBulkMsg(null)}>{bulkMsg}</Alert>}
            {bulk.isError && <Alert severity="error" sx={{ mb: 1 }}>{(bulk.error as any)?.response?.data?.message ?? "Bulk assignment failed."}</Alert>}

            <Box sx={{ maxHeight: 340, overflow: "auto", border: 1, borderColor: "divider", borderRadius: 1 }}>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell padding="checkbox"><Checkbox size="small" checked={allSelected} indeterminate={selected.size > 0 && !allSelected} onChange={toggleAll} /></TableCell>
                    <TableCell>Emp #</TableCell>
                    <TableCell>Name</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {empList.map((e) => (
                    <TableRow key={e.id} hover onClick={() => e.id && toggle(e.id)} sx={{ cursor: "pointer" }}>
                      <TableCell padding="checkbox"><Checkbox size="small" checked={!!e.id && selected.has(e.id)} /></TableCell>
                      <TableCell>{e.employeeNumber}</TableCell>
                      <TableCell>{e.firstName} {e.lastName}</TableCell>
                    </TableRow>
                  ))}
                  {projectId && empList.length === 0 && (
                    <TableRow><TableCell colSpan={3}><EmptyState text="No employees assigned to this project." /></TableCell></TableRow>
                  )}
                  {!projectId && (
                    <TableRow><TableCell colSpan={3}><EmptyState text="Pick a project to load employees." /></TableCell></TableRow>
                  )}
                </TableBody>
              </Table>
            </Box>
          </Paper>
        </Grid>

        <Grid item xs={12} lg={5}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, height: "100%" }}>
            <Typography variant="subtitle2">{form.id ? "Edit single assignment" : "Single assignment"}</Typography>
            <Typography variant="body2" color="text.secondary" mb={1.5}>
              Use this for one employee or date corrections.
            </Typography>
            <Stack spacing={1.25}>
              <TextField select fullWidth size="small" label="Employee" value={form.employeeId} disabled={!projectId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
                {(employees?.content ?? []).map((emp) => (
                  <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} - {emp.firstName} {emp.lastName}</MenuItem>
                ))}
              </TextField>
              <TextField select fullWidth size="small" label="Shift" value={form.shiftId} disabled={!projectId} onChange={(e) => setForm({ ...form, shiftId: e.target.value })}>
                {shifts.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} - {s.name}</MenuItem>)}
              </TextField>
              <Grid container spacing={1.25}>
                <Grid item xs={6}>
                  <TextField fullWidth size="small" type="date" label="From" InputLabelProps={{ shrink: true }} value={form.effectiveFrom} onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })} />
                </Grid>
                <Grid item xs={6}>
                  <TextField fullWidth size="small" type="date" label="To" InputLabelProps={{ shrink: true }} value={form.effectiveTo ?? ""} onChange={(e) => setForm({ ...form, effectiveTo: e.target.value || undefined })} />
                </Grid>
              </Grid>
              <Stack direction="row" spacing={1}>
                <Button startIcon={<AddIcon />} variant="contained" disabled={!projectId || !form.employeeId || !form.shiftId || save.isPending} onClick={() => save.mutate(form)}>
                  {form.id ? "Save" : "Add"}
                </Button>
                {form.id && <Button onClick={() => setForm(EMPTY)}>Cancel</Button>}
              </Stack>
              {save.isError && <Alert severity="error">{(save.error as any)?.response?.data?.message ?? "Save failed."}</Alert>}
            </Stack>
          </Paper>
        </Grid>
      </Grid>

      <Paper variant="outlined" sx={{ borderRadius: 2, mt: 2 }}>
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1} p={1.5}>
          <Box>
            <Typography variant="subtitle2">Current roster</Typography>
            <Typography variant="body2" color="text.secondary">Existing shift assignments for the selected project/filter.</Typography>
          </Box>
          <Chip label={`${filteredRoster.length} assignment(s)`} />
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Employee</TableCell>
              <TableCell>Shift</TableCell>
              <TableCell>From</TableCell>
              <TableCell>To</TableCell>
              <TableCell align="right" />
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredRoster.map((r) => (
              <TableRow key={r.id} hover>
                <TableCell>{r.employeeNumber} - {r.employeeName}</TableCell>
                <TableCell>{r.shiftCode}</TableCell>
                <TableCell>{r.effectiveFrom}</TableCell>
                <TableCell>{r.effectiveTo ?? "-"}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setForm(r)}>Edit</Button>
                  <IconButton size="small" color="error" onClick={() => r.id && del.mutate(r.id)}><DeleteIcon fontSize="small" /></IconButton>
                </TableCell>
              </TableRow>
            ))}
            {filteredRoster.length === 0 && (
              <TableRow><TableCell colSpan={5}><EmptyState text={projectId ? "No roster assignments match this project/filter." : "Pick a project to review roster assignments."} /></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}

function GuideCard({ icon, title, text }: { icon: ReactNode; title: string; text: string }) {
  return (
    <Card variant="outlined" sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Stack direction="row" spacing={1} alignItems="center" mb={1}>
          {icon}
          <Typography variant="subtitle2">{title}</Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary">{text}</Typography>
      </CardContent>
    </Card>
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
