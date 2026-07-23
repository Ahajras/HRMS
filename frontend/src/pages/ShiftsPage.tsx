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
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import ScheduleIcon from "@mui/icons-material/Schedule";
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
  const [savedShiftName, setSavedShiftName] = useState("");

  const save = useMutation({
    mutationFn: (s: Shift) => (s.id ? shiftApi.update(s.id, s) : shiftApi.create(s)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["shifts"] });
      setSavedShiftName(form.code || form.name);
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
  const activeCount = data.filter((s) => (s.status ?? "ACTIVE") === "ACTIVE").length;
  const projectSpecificCount = data.filter((s) => !!s.projectId).length;

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5">Shifts</Typography>
          <Typography variant="body2" color="text.secondary">
            Create shift templates first, then assign employees to those shifts from Shift Roster.
          </Typography>
        </Box>
        <Chip icon={<InfoOutlinedIcon />} label="Saving a shift does not assign it to employees." variant="outlined" />
      </Stack>

      <Grid container spacing={1.5} mb={2}>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<ScheduleIcon color="primary" />} title="1. Define working time" text="Set code, project, start/end time, break, and overnight flag." />
        </Grid>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<CalendarMonthIcon color="primary" />} title="2. Review sample week" text="Normal hours, declared OT, and weekly-off days drive timesheets and payroll." />
        </Grid>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<AddIcon color="primary" />} title="3. Assign from roster" text="After saving, open Shift Roster and assign this shift to employees with an effective date." />
        </Grid>
      </Grid>

      {savedShiftName && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSavedShiftName("")}>
          Shift {savedShiftName} saved. Next step: assign employees from Shift Roster.
        </Alert>
      )}

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1} mb={1.5}>
          <Box>
            <Typography variant="subtitle2">{form.id ? "Edit shift" : "Add shift"}</Typography>
            <Typography variant="body2" color="text.secondary">
              This screen defines the shift. The roster decides who uses it and from which date.
            </Typography>
          </Box>
          <Stack direction="row" spacing={1}>
            <Chip size="small" label={`${activeCount} active`} />
            <Chip size="small" label={`${projectSpecificCount} project-specific`} />
          </Stack>
        </Stack>

        <Alert severity="info" sx={{ mb: 2 }}>
          Workflow: create or update the shift here, then go to Shift Roster to assign employees before generating timesheets.
        </Alert>

        <Grid container spacing={1.5}>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          </Grid>
          <Grid item xs={6} sm={3}>
            <TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Project" value={form.projectId ?? ""} onChange={(e) => setForm({ ...form, projectId: e.target.value || undefined })}>
              <MenuItem value="">All projects</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" type="time" label="Start" InputLabelProps={{ shrink: true }} value={form.startTime ?? ""} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" type="time" label="End" InputLabelProps={{ shrink: true }} value={form.endTime ?? ""} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
          </Grid>
          <Grid item xs={6} sm={3}>
            <TextField fullWidth size="small" type="number" label="Break (min)" value={form.breakMinutes} onChange={(e) => setForm({ ...form, breakMinutes: Number(e.target.value) })} />
          </Grid>
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
            <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, bgcolor: "background.default" }}>
              <Typography variant="subtitle2">Sample week</Typography>
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
                Normal hours are the planned daily hours. Declared OT is the usual extra time. Weekly off marks rest days.
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
            </Paper>
          </Grid>

          <Grid item xs={12}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update shift" : "Add shift"}
              </Button>
              {form.id && <Button onClick={() => setForm(EMPTY)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      <Stack spacing={1.25}>
        {data.map((s) => (
          <Paper key={s.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
            <Stack direction={{ xs: "column", sm: "row" }} alignItems={{ xs: "stretch", sm: "center" }} justifyContent="space-between" spacing={1}>
              <Box>
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                  <Typography fontWeight={700}>{s.code} - {s.name}</Typography>
                  <Chip size="small" label={s.status ?? "ACTIVE"} color={(s.status ?? "ACTIVE") === "ACTIVE" ? "success" : "default"} />
                </Stack>
                <Typography variant="caption" color="text.secondary">
                  {s.startTime}-{s.endTime} - break {s.breakMinutes}m - off: {s.weeklyOff || "none"}{s.crossesMidnight ? " - overnight" : ""}
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
      </Stack>
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
