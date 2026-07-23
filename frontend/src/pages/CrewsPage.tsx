import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Divider,
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
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import GroupsIcon from "@mui/icons-material/Groups";
import SearchIcon from "@mui/icons-material/Search";
import { crewApi, employeeApi, lookupApi, projectApi, shiftApi, timekeeperApi } from "../api/resources";
import type { Crew } from "../api/types";

const today = () => new Date().toISOString().slice(0, 10);
const EMPTY: Crew = { code: "", name: "", status: "ACTIVE" };

export default function CrewsPage() {
  const qc = useQueryClient();
  const { data: crews = [] } = useQuery({ queryKey: ["crews"], queryFn: crewApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: timekeeperRows = [] } = useQuery({ queryKey: ["timekeeperProjects"], queryFn: timekeeperApi.list });
  const [form, setForm] = useState<Crew>(EMPTY);
  const [openMembers, setOpenMembers] = useState<string | null>(null);
  const [projectFilter, setProjectFilter] = useState("");
  const [q, setQ] = useState("");

  const { data: employeePage } = useQuery({
    queryKey: ["employeesByProject", form.projectId ?? "all"],
    queryFn: () => employeeApi.list(0, 500, undefined, undefined, form.projectId),
  });
  const empList = employeePage?.content ?? [];
  const timekeeperOptions = timekeeperRows
    .filter((row) => !form.projectId || row.projectId === form.projectId)
    .filter((row, index, rows) => rows.findIndex((r) => r.employeeId === row.employeeId) === index);

  const filteredCrews = crews
    .filter((c) => !projectFilter || c.projectId === projectFilter)
    .filter((c) => {
      const term = q.trim().toLowerCase();
      return !term || `${c.code} ${c.name} ${c.projectCode ?? ""} ${c.foremanName ?? ""} ${c.supervisorName ?? ""} ${c.timekeeperName ?? ""}`.toLowerCase().includes(term);
    });

  const projectLabel = (id?: string) => {
    const p = projects.find((project) => project.id === id);
    return p ? `${p.code} - ${p.name}` : "No project";
  };
  const employeeLabel = (emp: { employeeNumber?: string; firstName?: string; lastName?: string; employeeName?: string }) =>
    `${emp.employeeNumber ?? ""} - ${emp.employeeName ?? `${emp.firstName ?? ""} ${emp.lastName ?? ""}`.trim()}`.trim();
  const previewCode = () => {
    if (form.id) return form.code || "-";
    const prefix = (form.code || "").trim().toUpperCase().replace(/\s+/g, "-").replace(/-$/, "");
    if (!prefix) return "PREFIX-001";
    if (/-\d{3,}$/.test(prefix)) return prefix;
    const next = Math.max(
      0,
      ...crews
        .filter((c) => c.projectId === form.projectId && c.code?.startsWith(`${prefix}-`))
        .map((c) => Number(c.code?.slice(prefix.length + 1)) || 0),
    ) + 1;
    return `${prefix}-${String(next).padStart(3, "0")}`;
  };

  const save = useMutation({
    mutationFn: (c: Crew) => (c.id ? crewApi.update(c.id, c) : crewApi.create(c)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["crews"] });
      setForm(EMPTY);
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => crewApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["crews"] }),
  });

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", md: "center" }} spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5" fontWeight={800}>Crews</Typography>
          <Typography variant="body2" color="text.secondary">
            Create field teams by project. Enter a prefix like ENG or IT and the system generates the next crew code.
          </Typography>
        </Box>
        <Chip icon={<GroupsIcon />} label={`${crews.length} crew(s)`} color="primary" variant="outlined" />
      </Stack>

      <Grid container spacing={2} alignItems="flex-start">
        <Grid item xs={12} lg={7}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} mb={2}>
              <TextField select fullWidth size="small" label="Project" value={projectFilter} onChange={(e) => setProjectFilter(e.target.value)}>
                <MenuItem value="">All projects</MenuItem>
                {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
              </TextField>
              <TextField
                fullWidth
                size="small"
                placeholder="Search crew code, name, foreman, supervisor..."
                value={q}
                onChange={(e) => setQ(e.target.value)}
                InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
              />
            </Stack>

            <Stack spacing={1.5}>
              {filteredCrews.map((c) => (
                <Paper key={c.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
                  <Stack direction={{ xs: "column", sm: "row" }} alignItems={{ xs: "stretch", sm: "center" }} justifyContent="space-between" spacing={1.5}>
                    <Box sx={{ minWidth: 0 }}>
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                        <Typography fontWeight={800}>{c.code}</Typography>
                        <Typography color="text.secondary">-</Typography>
                        <Typography fontWeight={700}>{c.name}</Typography>
                        <Chip size="small" label={c.status ?? "ACTIVE"} color={(c.status ?? "ACTIVE") === "ACTIVE" ? "success" : "default"} />
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {projectLabel(c.projectId)} - {c.memberCount ?? 0} member(s)
                      </Typography>
                      <Stack direction="row" spacing={1} mt={1} flexWrap="wrap" useFlexGap>
                        <Chip size="small" variant="outlined" label={`Foreman: ${c.foremanName ?? "-"}`} />
                        <Chip size="small" variant="outlined" label={`Supervisor: ${c.supervisorName ?? "-"}`} />
                        <Chip size="small" variant="outlined" label={`Timekeeper: ${c.timekeeperName ?? "-"}`} />
                      </Stack>
                    </Box>
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button size="small" onClick={() => setOpenMembers(openMembers === c.id ? null : (c.id ?? null))}>
                        {openMembers === c.id ? "Hide" : "Members"}
                      </Button>
                      <Button size="small" onClick={() => setForm(c)}>Edit</Button>
                      <IconButton size="small" color="error" onClick={() => c.id && del.mutate(c.id)}><DeleteIcon /></IconButton>
                    </Stack>
                  </Stack>
                  {openMembers === c.id && c.id && <CrewMembersPanel crewId={c.id} projectId={c.projectId} />}
                </Paper>
              ))}
              {filteredCrews.length === 0 && (
                <Paper variant="outlined" sx={{ p: 3, borderRadius: 2, textAlign: "center", borderStyle: "dashed" }}>
                  <Typography variant="body2" color="text.secondary">No crews match the current filter.</Typography>
                </Paper>
              )}
            </Stack>
          </Paper>
        </Grid>

        <Grid item xs={12} lg={5}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, position: { lg: "sticky" }, top: 88 }}>
            <Typography variant="subtitle1" fontWeight={800} gutterBottom>{form.id ? "Edit crew" : "Create crew"}</Typography>
            <Alert severity="info" sx={{ mb: 2 }}>
              Code preview: <b>{previewCode()}</b>. The sequence is per project.
            </Alert>
            <Grid container spacing={1.5}>
              <Grid item xs={12} sm={5}>
                <TextField fullWidth size="small" label={form.id ? "Crew code" : "Code prefix"} value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
              </Grid>
              <Grid item xs={12} sm={7}>
                <TextField fullWidth size="small" label="Generated code" value={previewCode()} InputProps={{ readOnly: true }} />
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth size="small" label="Crew name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </Grid>
              <Grid item xs={12}>
                <TextField select fullWidth size="small" label="Project" value={form.projectId ?? ""} onChange={(e) => setForm({ ...form, projectId: e.target.value || undefined, foremanEmployeeId: undefined, supervisorEmployeeId: undefined, timekeeperEmployeeId: undefined })}>
                  <MenuItem value="">(none)</MenuItem>
                  {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                </TextField>
              </Grid>

              <Grid item xs={12}><Divider textAlign="left"><Typography variant="caption">Crew leads</Typography></Divider></Grid>
              <Grid item xs={12}>
                <TextField select fullWidth size="small" label="Foreman" value={form.foremanEmployeeId ?? ""} onChange={(e) => setForm({ ...form, foremanEmployeeId: e.target.value || undefined })}>
                  <MenuItem value="">(none)</MenuItem>
                  {empList.map((emp) => <MenuItem key={emp.id} value={emp.id}>{employeeLabel(emp)}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField select fullWidth size="small" label="Supervisor" value={form.supervisorEmployeeId ?? ""} onChange={(e) => setForm({ ...form, supervisorEmployeeId: e.target.value || undefined })}>
                  <MenuItem value="">(none)</MenuItem>
                  {empList.map((emp) => <MenuItem key={emp.id} value={emp.id}>{employeeLabel(emp)}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField select fullWidth size="small" label="Timekeeper" value={form.timekeeperEmployeeId ?? ""} onChange={(e) => setForm({ ...form, timekeeperEmployeeId: e.target.value || undefined })}>
                  <MenuItem value="">(none)</MenuItem>
                  {timekeeperOptions.map((tk) => <MenuItem key={tk.id ?? tk.employeeId} value={tk.employeeId}>{tk.employeeNumber} - {tk.employeeName}</MenuItem>)}
                </TextField>
              </Grid>

              <Grid item xs={12}>
                <TextField select fullWidth size="small" label="Parent crew (optional)" value={form.parentCrewId ?? ""} onChange={(e) => setForm({ ...form, parentCrewId: e.target.value || undefined })}>
                  <MenuItem value="">(none)</MenuItem>
                  {crews.filter((c) => c.id !== form.id && (!form.projectId || c.projectId === form.projectId)).map((c) => <MenuItem key={c.id} value={c.id}>{c.code} - {c.name}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <Stack direction="row" spacing={1}>
                  <Button startIcon={<AddIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                    {form.id ? "Update" : "Create"}
                  </Button>
                  {form.id && <Button onClick={() => setForm(EMPTY)}>Cancel</Button>}
                </Stack>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}

function CrewMembersPanel({ crewId, projectId }: { crewId: string; projectId?: string }) {
  const qc = useQueryClient();
  const { data: members = [] } = useQuery({ queryKey: ["crewMembers", crewId], queryFn: () => crewApi.members(crewId) });
  const { data: shifts = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });
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
    <Box sx={{ mt: 1.5, p: 1.5, bgcolor: "action.hover", borderRadius: 2 }}>
      <Typography variant="subtitle2" gutterBottom>Add members in bulk</Typography>
      <Grid container spacing={1.5} alignItems="center" mb={1}>
        <Grid item xs={12} sm={4}>
          <TextField select fullWidth size="small" label="Shift" value={shiftId} onChange={(e) => setShiftId(e.target.value)}>
            <MenuItem value="">(no shift)</MenuItem>
            {shifts.map((s) => <MenuItem key={s.id} value={s.id}>{s.code} - {s.name}</MenuItem>)}
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
      <Box sx={{ maxHeight: 220, overflow: "auto", border: 1, borderColor: "divider", borderRadius: 1, mb: 1.5, bgcolor: "background.paper" }}>
        <Table size="small" stickyHeader>
          <TableBody>
            {filtered.map((e) => (
              <TableRow key={e.id} hover onClick={() => e.id && toggle(e.id)} sx={{ cursor: "pointer" }}>
                <TableCell padding="checkbox"><Checkbox size="small" checked={!!e.id && selected.has(e.id)} /></TableCell>
                <TableCell>{e.employeeNumber} - {e.firstName} {e.lastName}</TableCell>
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
              <TableCell>{m.employeeNumber} - {m.employeeName}</TableCell>
              <TableCell>{m.shiftCode ?? "-"}</TableCell>
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
  const { data: jobTitles = [] } = useQuery({ queryKey: ["lookup", "JOB_TITLE"], queryFn: () => lookupApi.byCategory("JOB_TITLE") });
  const [jobTitleCode, setJobTitleCode] = useState("");
  const [planned, setPlanned] = useState(1);
  const selectedJobTitle = jobTitles.find((item) => item.code === jobTitleCode);

  const add = useMutation({
    mutationFn: () => crewApi.addTrade(crewId, {
      tradeCode: jobTitleCode,
      tradeName: selectedJobTitle?.label ?? jobTitleCode,
      plannedCount: planned,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["crewTrades", crewId] });
      setJobTitleCode("");
      setPlanned(1);
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => crewApi.removeTrade(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["crewTrades", crewId] }),
  });

  return (
    <Box sx={{ mt: 2 }}>
      <Typography variant="subtitle2" gutterBottom>Required manpower</Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Job title</TableCell>
            <TableCell>Code</TableCell>
            <TableCell align="right">Required</TableCell>
            <TableCell align="right">Assigned</TableCell>
            <TableCell align="right" />
          </TableRow>
        </TableHead>
        <TableBody>
          {trades.map((t) => {
            const ok = (t.assignedCount ?? 0) >= t.plannedCount;
            return (
              <TableRow key={t.id} sx={{ bgcolor: ok ? "#e8f5e9" : "#fff3e0" }}>
                <TableCell>{t.tradeName ?? "-"}</TableCell>
                <TableCell>{t.tradeCode}</TableCell>
                <TableCell align="right">{t.plannedCount}</TableCell>
                <TableCell align="right" sx={{ color: ok ? "success.main" : "warning.dark", fontWeight: 700 }}>{t.assignedCount ?? 0}</TableCell>
                <TableCell align="right"><IconButton size="small" color="error" onClick={() => t.id && remove.mutate(t.id)}><DeleteIcon fontSize="small" /></IconButton></TableCell>
              </TableRow>
            );
          })}
          {trades.length === 0 && <TableRow><TableCell colSpan={5}><Typography variant="body2" color="text.secondary">No manpower requirements defined.</Typography></TableCell></TableRow>}
        </TableBody>
      </Table>
      <Stack direction={{ xs: "column", sm: "row" }} spacing={1} mt={1} alignItems={{ xs: "stretch", sm: "center" }}>
        <TextField select size="small" label="Job title" value={jobTitleCode} onChange={(e) => setJobTitleCode(e.target.value)} sx={{ flex: 1 }}>
          <MenuItem value="">Select job title</MenuItem>
          {jobTitles.map((item) => (
            <MenuItem key={item.id ?? item.code} value={item.code}>{item.label}</MenuItem>
          ))}
        </TextField>
        <TextField size="small" label="Code" value={jobTitleCode || "-"} InputProps={{ readOnly: true }} sx={{ width: { sm: 140 } }} />
        <TextField size="small" type="number" label="Required" value={planned} onChange={(e) => setPlanned(Number(e.target.value))} sx={{ width: { sm: 110 } }} />
        <Button variant="contained" size="small" disabled={!jobTitleCode || planned < 1 || add.isPending} onClick={() => add.mutate()}>Add</Button>
      </Stack>
    </Box>
  );
}
