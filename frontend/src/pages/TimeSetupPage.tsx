import { useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Grid,
  IconButton,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import { publicHolidayApi, timeTypeApi } from "../api/resources";
import type { PublicHoliday, TimeType } from "../api/types";

const EMPTY_TYPE: TimeType = {
  code: "",
  name: "",
  category: "",
  paid: true,
  countsAsWorked: true,
  affectsLeave: false,
  factor: 1,
  sortOrder: 0,
};

function TimeTypesSection() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["timeTypes"], queryFn: timeTypeApi.list });
  const [form, setForm] = useState<TimeType>(EMPTY_TYPE);

  const save = useMutation({
    mutationFn: (t: TimeType) => (t.id ? timeTypeApi.update(t.id, t) : timeTypeApi.create(t)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["timeTypes"] }); setForm(EMPTY_TYPE); },
  });
  const del = useMutation({
    mutationFn: (id: string) => timeTypeApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["timeTypes"] }),
  });

  return (
    <Box mb={4}>
      <Typography variant="h6" mb={1.5}>Time Types</Typography>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit time type" : "Add time type"}</Typography>
        <Grid container spacing={1.5}>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} /></Grid>
          <Grid item xs={6} sm={3}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" label="Category" value={form.category ?? ""} onChange={(e) => setForm({ ...form, category: e.target.value })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" type="number" label="Factor" value={form.factor ?? 1} onChange={(e) => setForm({ ...form, factor: Number(e.target.value) })} /></Grid>
          <Grid item xs={6} sm={2}><TextField fullWidth size="small" type="number" label="Sort" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: Number(e.target.value) })} /></Grid>
          <Grid item xs={12}>
            <Stack direction="row" spacing={2} flexWrap="wrap">
              <FormControlLabel control={<Checkbox checked={form.paid} onChange={(e) => setForm({ ...form, paid: e.target.checked })} />} label="Paid" />
              <FormControlLabel control={<Checkbox checked={form.countsAsWorked} onChange={(e) => setForm({ ...form, countsAsWorked: e.target.checked })} />} label="Counts as worked" />
              <FormControlLabel control={<Checkbox checked={form.affectsLeave} onChange={(e) => setForm({ ...form, affectsLeave: e.target.checked })} />} label="Affects leave" />
            </Stack>
          </Grid>
          <Grid item xs={12}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm(EMPTY_TYPE)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>
      {data.map((t) => (
        <Paper key={t.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>{t.code} — {t.name}</Typography>
              <Typography variant="caption" color="text.secondary">
                factor {t.factor} · {t.paid ? "paid" : "unpaid"} · {t.countsAsWorked ? "worked" : "not worked"}{t.affectsLeave ? " · affects leave" : ""}
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setForm(t)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => t.id && del.mutate(t.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No time types yet.</Typography>}
    </Box>
  );
}

function HolidaysSection() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["publicHolidays"], queryFn: publicHolidayApi.list });
  const [form, setForm] = useState<PublicHoliday>({ holidayDate: "", name: "" });

  const save = useMutation({
    mutationFn: (h: PublicHoliday) => (h.id ? publicHolidayApi.update(h.id, h) : publicHolidayApi.create(h)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["publicHolidays"] }); setForm({ holidayDate: "", name: "" }); },
  });
  const del = useMutation({
    mutationFn: (id: string) => publicHolidayApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["publicHolidays"] }),
  });

  return (
    <Box>
      <Typography variant="h6" mb={1.5}>Public Holidays</Typography>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={3}><TextField fullWidth size="small" type="date" label="Date" InputLabelProps={{ shrink: true }} value={form.holidayDate} onChange={(e) => setForm({ ...form, holidayDate: e.target.value })} /></Grid>
          <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={12} sm={3}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.holidayDate || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm({ holidayDate: "", name: "" })}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>
      {data.map((h) => (
        <Paper key={h.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography fontWeight={600}>{h.holidayDate} — {h.name}</Typography>
            <Box>
              <Button size="small" onClick={() => setForm(h)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => h.id && del.mutate(h.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No holidays yet.</Typography>}
    </Box>
  );
}

export default function TimeSetupPage() {
  return (
    <Box>
      <Typography variant="h5" mb={2}>Time Setup</Typography>
      <TimeTypesSection />
      <HolidaysSection />
    </Box>
  );
}
