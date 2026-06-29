import { useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
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
import { projectApi, shiftApi } from "../api/resources";
import type { Shift, ShiftDay } from "../api/types";

const DAYS = ["SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI"];

const defaultWeek = (): ShiftDay[] =>
  DAYS.map((d) =>
    d === "FRI"
      ? { dayOfWeek: d, normalHours: 0, declaredOt: 0, weeklyOff: true }
      : { dayOfWeek: d, normalHours: 8, declaredOt: 2, weeklyOff: false },
  );

const EMPTY: Shift = {
  code: "",
  name: "",
  startTime: "08:00",
  endTime: "17:00",
  breakMinutes: 60,
  standardHours: 8,
  crossesMidnight: false,
  days: defaultWeek(),
};

export default function ShiftsPage() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const [form, setForm] = useState<Shift>(EMPTY);

  const save = useMutation({
    mutationFn: (s: Shift) => (s.id ? shiftApi.update(s.id, s) : shiftApi.create(s)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["shifts"] });
      setForm(EMPTY);
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => shiftApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["shifts"] }),
  });

  const edit = (s: Shift) => setForm({ ...s, days: s.days?.length ? s.days : defaultWeek() });

  const week = form.days ?? defaultWeek();
  const setDay = (idx: number, patch: Partial<ShiftDay>) =>
    setForm({ ...form, days: week.map((d, i) => (i === idx ? { ...d, ...patch } : d)) });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Shifts</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit shift" : "Add shift"}</Typography>
        <Grid container spacing={1.5}>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} /></Grid>
          <Grid item xs={6} sm={3}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Project" value={form.projectId ?? ""} onChange={(e) => setForm({ ...form, projectId: e.target.value || undefined })}>
              <MenuItem value="">(all projects)</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" type="time" label="Start" InputLabelProps={{ shrink: true }} value={form.startTime ?? ""} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" type="time" label="End" InputLabelProps={{ shrink: true }} value={form.endTime ?? ""} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></Grid>
          <Grid item xs={6} sm={3}><TextField fullWidth size="small" type="number" label="Break (min)" value={form.breakMinutes} onChange={(e) => setForm({ ...form, breakMinutes: Number(e.target.value) })} /></Grid>
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Status" value={form.status ?? "ACTIVE"} onChange={(e) => setForm({ ...form, status: e.target.value })}>
              <MenuItem value="ACTIVE">ACTIVE</MenuItem>
              <MenuItem value="INACTIVE">INACTIVE</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControlLabel control={<Checkbox checked={form.crossesMidnight} onChange={(e) => setForm({ ...form, crossesMidnight: e.target.checked })} />} label="Crosses midnight" />
          </Grid>

          <Grid item xs={12}>
            <Typography variant="subtitle2" sx={{ mt: 1 }}>Sample week (per-day normal hours + declared overtime)</Typography>
            <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
              For a weekly-off day you can still set Normal hrs (e.g. 8) — these are the paid weekend hours for MONTHLY staff (daily-paid get 0).
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Day</TableCell>
                  <TableCell align="right">Normal hrs</TableCell>
                  <TableCell align="right">Declared OT</TableCell>
                  <TableCell align="center">Weekly off</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {week.map((d, idx) => (
                  <TableRow key={d.dayOfWeek}>
                    <TableCell>{d.dayOfWeek}</TableCell>
                    <TableCell align="right">
                      <TextField type="number" size="small" value={d.normalHours ?? 0} onChange={(e) => setDay(idx, { normalHours: Number(e.target.value) })} sx={{ width: 90 }} />
                    </TableCell>
                    <TableCell align="right">
                      <TextField type="number" size="small" value={d.declaredOt ?? 0} onChange={(e) => setDay(idx, { declaredOt: Number(e.target.value) })} sx={{ width: 90 }} disabled={d.weeklyOff} />
                    </TableCell>
                    <TableCell align="center">
                      <Checkbox size="small" checked={d.weeklyOff} onChange={(e) => setDay(idx, { weeklyOff: e.target.checked })} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
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

      {data.map((s) => (
        <Paper key={s.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>{s.code} — {s.name}</Typography>
              <Typography variant="caption" color="text.secondary">
                {s.startTime}–{s.endTime} · break {s.breakMinutes}m · off: {s.weeklyOff || "none"}{s.crossesMidnight ? " · overnight" : ""}
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => edit(s)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => s.id && del.mutate(s.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No shifts yet. Add one above.</Typography>}
    </Box>
  );
}
