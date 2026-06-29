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
import { crewApi, employeeApi, projectApi, shiftApi } from "../api/resources";
import type { Crew } from "../api/types";

const today = () => new Date().toISOString().slice(0, 10);
const EMPTY: Crew = { code: "", name: "" };

export default function CrewsPage() {
  const qc = useQueryClient();
  const { data: crews = [] } = useQuery({ queryKey: ["crews"], queryFn: crewApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: employees } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500) });
  const [form, setForm] = useState<Crew>(EMPTY);
  const [openMembers, setOpenMembers] = useState<string | null>(null);

  const empList = employees?.content ?? [];

  const save = useMutation({
    mutationFn: (c: Crew) => (c.id ? crewApi.update(c.id, c) : crewApi.create(c)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crews"] }); setForm(EMPTY); },
  });
  const del = useMutation({
    mutationFn: (id: string) => crewApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["crews"] }),
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Crews</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit crew" : "Add crew"}</Typography>
        <Grid container spacing={1.5}>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} /></Grid>
          <Grid item xs={6} sm={3}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Project" value={form.projectId ?? ""} onChange={(e) => setForm({ ...form, projectId: e.target.value || undefined })}>
              <MenuItem value="">(none)</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Foreman" value={form.foremanEmployeeId ?? ""} onChange={(e) => setForm({ ...form, foremanEmployeeId: e.target.value || undefined })}>
              <MenuItem value="">(none)</MenuItem>
              {empList.map((emp) => <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} — {emp.firstName} {emp.lastName}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Parent crew (optional)" value={form.parentCrewId ?? ""} onChange={(e) => setForm({ ...form, parentCrewId: e.target.value || undefined })}>
              <MenuItem value="">(none)</MenuItem>
              {crews.filter((c) => c.id !== form.id).map((c) => <MenuItem key={c.id} value={c.id}>{c.code} — {c.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm(EMPTY)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      {crews.map((c) => (
        <Paper key={c.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>{c.code} — {c.name}</Typography>
              <Typography variant="caption" color="text.secondary">
                project: {c.projectCode ?? "—"} · foreman: {c.foremanName ?? "—"} · {c.memberCount ?? 0} member(s)
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setOpenMembers(openMembers === c.id ? null : (c.id ?? null))}>
                {openMembers === c.id ? "Hide members" : "Members"}
              </Button>
              <Button size="small" onClick={() => setForm(c)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => c.id && del.mutate(c.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
          {openMembers === c.id && c.id && <CrewMembersPanel crewId={c.id} empList={empList} />}
        </Paper>
      ))}
      {crews.length === 0 && <Typography variant="body2" color="text.secondary">No crews yet. Add one above.</Typography>}
    </Box>
  );
}

function CrewMembersPanel({ crewId, empList }: { crewId: string; empList: any[] }) {
  const qc = useQueryClient();
  const { data: members = [] } = useQuery({ queryKey: ["crewMembers", crewId], queryFn: () => crewApi.members(crewId) });
  const { data: shifts = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });

  const [shiftId, setShiftId] = useState("");
  const [date, setDate] = useState(today());
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [q, setQ] = useState("");
  const [msg, setMsg] = useState<string | null>(null);

  const filtered = empList.filter((e) => {
    const t = q.trim().toLowerCase();
    return !t || `${e.employeeNumber ?? ""} ${e.firstName ?? ""} ${e.lastName ?? ""}`.toLowerCase().includes(t);
  });
  const toggle = (id: string) => setSelected((p) => { const n = new Set(p); n.has(id) ? n.delete(id) : n.add(id); return n; });

  const add = useMutation({
    mutationFn: () => crewApi.bulkAddMembers(crewId, shiftId || undefined, date, Array.from(selected)),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["crewMembers", crewId] });
      qc.invalidateQueries({ queryKey: ["crews"] });
      setMsg(`Added ${r.created} member(s).`);
      setSelected(new Set());
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => crewApi.removeMember(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crewMembers", crewId] }); qc.invalidateQueries({ queryKey: ["crews"] }); },
  });

  return (
    <Box sx={{ mt: 1.5, p: 1.5, bgcolor: "action.hover", borderRadius: 1 }}>
      <Typography variant="subtitle2" gutterBottom>Add members (bulk) — each on the chosen shift</Typography>
      <Grid container spacing={1.5} alignItems="center" mb={1}>
        <Grid item xs={12} sm={4}>
          <TextField select fullWidth size="small" label="Shift" value={shiftId} onChange={(e) => setShiftId(e.target.value)}>
            <MenuItem value="">(no shift)</MenuItem>
            {shifts.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} — {s.name}</MenuItem>)}
          </TextField>
        </Grid>
        <Grid item xs={6} sm={3}>
          <TextField fullWidth size="small" type="date" label="From" InputLabelProps={{ shrink: true }} value={date} onChange={(e) => setDate(e.target.value)} />
        </Grid>
        <Grid item xs={6} sm={5}>
          <Button variant="contained" disabled={selected.size === 0 || add.isPending} onClick={() => add.mutate()}>
            Add {selected.size} selected
          </Button>
        </Grid>
      </Grid>
      {msg && <Alert severity="success" sx={{ mb: 1 }} onClose={() => setMsg(null)}>{msg}</Alert>}
      <TextField size="small" fullWidth placeholder="Search employee" value={q} onChange={(e) => setQ(e.target.value)} sx={{ mb: 1 }} />
      <Box sx={{ maxHeight: 200, overflow: "auto", border: 1, borderColor: "divider", borderRadius: 1, mb: 1.5, bgcolor: "background.paper" }}>
        <Table size="small" stickyHeader>
          <TableBody>
            {filtered.map((e) => (
              <TableRow key={e.id} hover onClick={() => e.id && toggle(e.id)} sx={{ cursor: "pointer" }}>
                <TableCell padding="checkbox"><Checkbox size="small" checked={!!e.id && selected.has(e.id)} /></TableCell>
                <TableCell>{e.employeeNumber} — {e.firstName} {e.lastName}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>

      <Typography variant="subtitle2" gutterBottom>Current members</Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Employee</TableCell>
            <TableCell>Shift</TableCell>
            <TableCell>From</TableCell>
            <TableCell align="right" />
          </TableRow>
        </TableHead>
        <TableBody>
          {members.map((m) => (
            <TableRow key={m.id}>
              <TableCell>{m.employeeNumber} — {m.employeeName}</TableCell>
              <TableCell>{m.shiftCode ?? "—"}</TableCell>
              <TableCell>{m.effectiveFrom}</TableCell>
              <TableCell align="right"><IconButton size="small" color="error" onClick={() => m.id && remove.mutate(m.id)}><DeleteIcon fontSize="small" /></IconButton></TableCell>
            </TableRow>
          ))}
          {members.length === 0 && <TableRow><TableCell colSpan={4}><Typography variant="body2" color="text.secondary">No members yet.</Typography></TableCell></TableRow>}
        </TableBody>
      </Table>
    </Box>
  );
}
