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
  const [form, setForm] = useState<Crew>(EMPTY);
  const [openMembers, setOpenMembers] = useState<string | null>(null);

  // Foreman candidates are limited to the selected project's employees.
  const { data: foremanPage } = useQuery({
    queryKey: ["employeesByProject", form.projectId ?? "all"],
    queryFn: () => employeeApi.list(0, 500, undefined, undefined, form.projectId),
  });
  const empList = foremanPage?.content ?? [];

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
            <TextField select fullWidth size="small" label="Project" value={form.projectId ?? ""} onChange={(e) => setForm({ ...form, projectId: e.target.value || undefined, foremanEmployeeId: undefined })}>
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
          {openMembers === c.id && c.id && <CrewMembersPanel crewId={c.id} projectId={c.projectId} />}
        </Paper>
      ))}
      {crews.length === 0 && <Typography variant="body2" color="text.secondary">No crews yet. Add one above.</Typography>}
    </Box>
  );
}

function CrewMembersPanel({ crewId, projectId }: { crewId: string; projectId?: string }) {
  const qc = useQueryClient();
  const { data: members = [] } = useQuery({ queryKey: ["crewMembers", crewId], queryFn: () => crewApi.members(crewId) });
  const { data: shifts = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });
  // Only employees of the crew's project can be added.
  const { data: empPage } = useQuery({
    queryKey: ["employeesByProject", projectId ?? "all"],
    queryFn: () => employeeApi.list(0, 500, undefined, undefined, projectId),
  });
  const empList = empPage?.content ?? [];

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
      qc.invalidateQueries({ queryKey: ["crewByEmp"] });
      setMsg(`Added ${r.created} member(s).`);
      setSelected(new Set());
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => crewApi.removeMember(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["crewMembers", crewId] });
      qc.invalidateQueries({ queryKey: ["crews"] });
      qc.invalidateQueries({ queryKey: ["crewByEmp"] });
    },
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

      <CrewTradesPanel crewId={crewId} />
    </Box>
  );
}

function CrewTradesPanel({ crewId }: { crewId: string }) {
  const qc = useQueryClient();
  const { data: trades = [] } = useQuery({ queryKey: ["crewTrades", crewId], queryFn: () => crewApi.trades(crewId) });
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [planned, setPlanned] = useState(1);

  const add = useMutation({
    mutationFn: () => crewApi.addTrade(crewId, { tradeCode: code, tradeName: name, plannedCount: planned }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["crewTrades", crewId] }); setCode(""); setName(""); setPlanned(1); },
  });
  const remove = useMutation({
    mutationFn: (id: string) => crewApi.removeTrade(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["crewTrades", crewId] }),
  });

  return (
    <Box sx={{ mt: 2 }}>
      <Typography variant="subtitle2" gutterBottom>Trades (required vs assigned)</Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Trade code</TableCell>
            <TableCell>Title</TableCell>
            <TableCell align="right">Planned</TableCell>
            <TableCell align="right">Assigned</TableCell>
            <TableCell align="right" />
          </TableRow>
        </TableHead>
        <TableBody>
          {trades.map((t) => {
            const ok = (t.assignedCount ?? 0) >= t.plannedCount;
            return (
              <TableRow key={t.id} sx={{ bgcolor: ok ? "#e8f5e9" : "#ffebee" }}>
                <TableCell>{t.tradeCode}</TableCell>
                <TableCell>{t.tradeName ?? "—"}</TableCell>
                <TableCell align="right">{t.plannedCount}</TableCell>
                <TableCell align="right" sx={{ color: ok ? "success.main" : "error.main", fontWeight: 600 }}>{t.assignedCount ?? 0}</TableCell>
                <TableCell align="right"><IconButton size="small" color="error" onClick={() => t.id && remove.mutate(t.id)}><DeleteIcon fontSize="small" /></IconButton></TableCell>
              </TableRow>
            );
          })}
          {trades.length === 0 && <TableRow><TableCell colSpan={5}><Typography variant="body2" color="text.secondary">No trades defined.</Typography></TableCell></TableRow>}
        </TableBody>
      </Table>
      <Stack direction="row" spacing={1} mt={1} alignItems="center">
        <TextField size="small" label="Trade code" value={code} onChange={(e) => setCode(e.target.value)} sx={{ width: 140 }} />
        <TextField size="small" label="Title" value={name} onChange={(e) => setName(e.target.value)} sx={{ flex: 1 }} />
        <TextField size="small" type="number" label="Planned" value={planned} onChange={(e) => setPlanned(Number(e.target.value))} sx={{ width: 100 }} />
        <Button variant="contained" size="small" disabled={!code || add.isPending} onClick={() => add.mutate()}>Add</Button>
      </Stack>
    </Box>
  );
}
