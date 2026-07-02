import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Checkbox,
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
import { employeeApi, employeeShiftApi, projectApi, shiftApi } from "../api/resources";
import type { EmployeeShift } from "../api/types";

const today = () => new Date().toISOString().slice(0, 10);
const EMPTY: EmployeeShift = { employeeId: "", shiftId: "", effectiveFrom: today() };

export default function RosterPage() {
  const qc = useQueryClient();
  const [form, setForm] = useState<EmployeeShift>(EMPTY);
  const [projectId, setProjectId] = useState("");

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
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["roster"] }); setForm(EMPTY); },
  });
  const del = useMutation({
    mutationFn: (id: string) => employeeShiftApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["roster"] }),
  });

  // --- bulk assign ---
  const [bulkShift, setBulkShift] = useState("");
  const [bulkDate, setBulkDate] = useState(today());
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [bulkMsg, setBulkMsg] = useState<string | null>(null);
  const [q, setQ] = useState("");

  const match = (s: string) => {
    const t = q.trim().toLowerCase();
    return !t || s.toLowerCase().includes(t);
  };
  const empList = (employees?.content ?? []).filter((e) =>
    match(`${e.employeeNumber ?? ""} ${e.firstName ?? ""} ${e.lastName ?? ""}`));
  const filteredRoster = roster.filter((r) =>
    (!projectId || empList.some((e) => e.id === r.employeeId)) &&
    match(`${r.employeeNumber ?? ""} ${r.employeeName ?? ""} ${r.shiftCode ?? ""}`));
  const toggle = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  const allSelected = empList.length > 0 && empList.every((e) => e.id && selected.has(e.id));
  const toggleAll = () =>
    setSelected(allSelected ? new Set() : new Set(empList.map((e) => e.id!).filter(Boolean)));

  const bulk = useMutation({
    mutationFn: () => employeeShiftApi.bulkAssign(bulkShift, bulkDate, Array.from(selected)),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["roster"] });
      setBulkMsg(`Assigned ${r.created} employee(s).`);
      setSelected(new Set());
    },
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Shift Roster</Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Assign each employee to a shift. Timesheet generation uses the shift that is in effect for the period.
      </Typography>

      <Stack direction="row" spacing={1.5} mb={2}>
        <TextField select size="small" label="Project" value={projectId} onChange={(e) => { setProjectId(e.target.value); setSelected(new Set()); setBulkShift(""); setForm(EMPTY); }} sx={{ minWidth: 240 }}>
          <MenuItem value="">Pick project</MenuItem>
          {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>)}
        </TextField>
        <TextField size="small" fullWidth placeholder="Search employee or shift" value={q} onChange={(e) => setQ(e.target.value)} />
      </Stack>

      {!projectId && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Pick a project first to load that project's employees and matching shifts.
        </Alert>
      )}

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>Bulk assign (fast)</Typography>
        <Grid container spacing={1.5} alignItems="center" mb={1}>
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Shift" value={bulkShift} disabled={!projectId} onChange={(e) => setBulkShift(e.target.value)}>
              {shifts.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} — {s.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} sm={3}>
            <TextField fullWidth size="small" type="date" label="From" InputLabelProps={{ shrink: true }} value={bulkDate} onChange={(e) => setBulkDate(e.target.value)} />
          </Grid>
          <Grid item xs={6} sm={5}>
            <Button variant="contained" disabled={!projectId || !bulkShift || selected.size === 0 || bulk.isPending} onClick={() => bulk.mutate()}>
              Assign {selected.size} selected
            </Button>
          </Grid>
        </Grid>
        {bulkMsg && <Alert severity="success" sx={{ mb: 1 }} onClose={() => setBulkMsg(null)}>{bulkMsg}</Alert>}
        <Box sx={{ maxHeight: 260, overflow: "auto", border: 1, borderColor: "divider", borderRadius: 1 }}>
          <Table size="small" stickyHeader>
            <TableHead>
              <TableRow>
                <TableCell padding="checkbox"><Checkbox size="small" checked={allSelected} onChange={toggleAll} /></TableCell>
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
                <TableRow><TableCell colSpan={3}><Typography variant="body2" color="text.secondary" p={1}>No employees assigned to this project.</Typography></TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </Box>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit assignment" : "Assign employee to shift"}</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Employee" value={form.employeeId} disabled={!projectId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
              {(employees?.content ?? []).map((emp) => (
                <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} — {emp.firstName} {emp.lastName}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Shift" value={form.shiftId} disabled={!projectId} onChange={(e) => setForm({ ...form, shiftId: e.target.value })}>
              {shifts.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} — {s.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" type="date" label="From" InputLabelProps={{ shrink: true }} value={form.effectiveFrom} onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })} />
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" type="date" label="To" InputLabelProps={{ shrink: true }} value={form.effectiveTo ?? ""} onChange={(e) => setForm({ ...form, effectiveTo: e.target.value || undefined })} />
          </Grid>
          <Grid item xs={12} sm={1}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!projectId || !form.employeeId || !form.shiftId || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Save" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm(EMPTY)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2 }}>
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
                <TableCell>{r.employeeNumber} — {r.employeeName}</TableCell>
                <TableCell>{r.shiftCode}</TableCell>
                <TableCell>{r.effectiveFrom}</TableCell>
                <TableCell>{r.effectiveTo ?? "—"}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setForm(r)}>Edit</Button>
                  <IconButton size="small" color="error" onClick={() => r.id && del.mutate(r.id)}><DeleteIcon fontSize="small" /></IconButton>
                </TableCell>
              </TableRow>
            ))}
            {filteredRoster.length === 0 && (
              <TableRow><TableCell colSpan={5}><Typography variant="body2" color="text.secondary" p={1}>No assignments yet.</Typography></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}
