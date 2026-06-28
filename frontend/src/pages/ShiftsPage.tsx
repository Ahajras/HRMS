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
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import { shiftApi } from "../api/resources";
import type { Shift } from "../api/types";

const DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"];

const EMPTY: Shift = {
  code: "",
  name: "",
  startTime: "08:00",
  endTime: "17:00",
  breakMinutes: 60,
  standardHours: 8,
  crossesMidnight: false,
  weeklyOff: "FRI",
};

export default function ShiftsPage() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });
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

  const offSet = new Set((form.weeklyOff ?? "").split(",").map((d) => d.trim()).filter(Boolean));
  const toggleOff = (day: string) => {
    const next = new Set(offSet);
    next.has(day) ? next.delete(day) : next.add(day);
    setForm({ ...form, weeklyOff: DAYS.filter((d) => next.has(d)).join(",") });
  };

  return (
    <Box>
      <Typography variant="h5" mb={2}>Shifts</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit shift" : "Add shift"}</Typography>
        <Grid container spacing={1.5}>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" type="time" label="Start" InputLabelProps={{ shrink: true }} value={form.startTime ?? ""} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" type="time" label="End" InputLabelProps={{ shrink: true }} value={form.endTime ?? ""} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" type="number" label="Break (min)" value={form.breakMinutes} onChange={(e) => setForm({ ...form, breakMinutes: Number(e.target.value) })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" type="number" label="Std hrs" value={form.standardHours ?? 0} onChange={(e) => setForm({ ...form, standardHours: Number(e.target.value) })} /></Grid>
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
            <Typography variant="caption" color="text.secondary">Weekly off days</Typography>
            <Stack direction="row" spacing={0.5} flexWrap="wrap">
              {DAYS.map((d) => (
                <FormControlLabel key={d} control={<Checkbox size="small" checked={offSet.has(d)} onChange={() => toggleOff(d)} />} label={d} />
              ))}
            </Stack>
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
                {s.startTime}–{s.endTime} · break {s.breakMinutes}m · {s.standardHours}h/day · off: {s.weeklyOff || "none"}{s.crossesMidnight ? " · overnight" : ""}
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setForm(s)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => s.id && del.mutate(s.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No shifts yet. Add one above.</Typography>}
    </Box>
  );
}
