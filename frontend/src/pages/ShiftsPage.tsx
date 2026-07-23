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
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import SearchIcon from "@mui/icons-material/Search";
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

const requiredFieldSx = (missing: boolean) => missing ? {
  "& .MuiOutlinedInput-root": {
    backgroundColor: "#fff7ed",
    "& fieldset": { borderColor: "#fb923c" },
  },
  "& .MuiInputLabel-root": { color: "#c2410c" },
} : undefined;

export default function ShiftsPage() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const [form, setForm] = useState<Shift>(EMPTY);
  const [savedShiftName, setSavedShiftName] = useState("");
  const [shiftSearch, setShiftSearch] = useState("");

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
  const filteredShifts = data.filter((s) => {
    const q = shiftSearch.trim().toLowerCase();
    if (!q) return true;
    return `${s.code ?? ""} ${s.name ?? ""} ${s.status ?? ""}`.toLowerCase().includes(q);
  });
  const missingRequired = [
    !form.code.trim() ? "Code is required so payroll/timesheet records can identify the shift." : "",
    !form.name.trim() ? "Name is required so users can recognize the shift." : "",
    !form.startTime ? "Start time is required for planned attendance." : "",
    !form.endTime ? "End time is required for planned attendance." : "",
  ].filter(Boolean);

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

      <Grid container spacing={2} alignItems="flex-start">
        <Grid item xs={12} lg={4}>
          <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
            <Stack spacing={1.25}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="subtitle2">Existing shifts</Typography>
                  <Typography variant="body2" color="text.secondary">Select one to edit.</Typography>
                </Box>
                <Chip size="small" label={`${data.length} total`} />
              </Stack>
              <Stack direction="row" spacing={1}>
                <Chip size="small" label={`${activeCount} active`} />
                <Chip size="small" label={`${projectSpecificCount} project-specific`} />
              </Stack>
              <TextField
                fullWidth
                size="small"
                label="Search shifts"
                value={shiftSearch}
                onChange={(e) => setShiftSearch(e.target.value)}
                InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
              />
              <Stack spacing={1} sx={{ maxHeight: { lg: "calc(100vh - 360px)" }, overflow: "auto", pr: 0.5 }}>
                {filteredShifts.map((s) => (
                  <Paper
                    key={s.id}
                    variant="outlined"
                    onClick={() => edit(s)}
                    sx={{
                      p: 1.25,
                      borderRadius: 1.5,
                      cursor: "pointer",
                      borderColor: form.id === s.id ? "primary.main" : "divider",
                      bgcolor: form.id === s.id ? "action.selected" : "background.paper",
                    }}
                  >
                    <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1}>
                      <Box>
                        <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
                          <Typography fontWeight={700}>{s.code}</Typography>
                          <Chip size="small" label={s.status ?? "ACTIVE"} color={(s.status ?? "ACTIVE") === "ACTIVE" ? "success" : "default"} />
                        </Stack>
                        <Typography variant="body2">{s.name}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {s.startTime}-{s.endTime} - break {s.breakMinutes}m
                        </Typography>
                      </Box>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={(e) => {
                          e.stopPropagation();
                          if (s.id) del.mutate(s.id);
                        }}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  </Paper>
                ))}
                {filteredShifts.length === 0 && (
                  <Typography variant="body2" color="text.secondary" p={1}>
                    {data.length === 0 ? "No shifts yet. Add one on the right." : "No shifts match the search."}
                  </Typography>
                )}
              </Stack>
            </Stack>
          </Paper>
        </Grid>

        <Grid item xs={12} lg={8}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
            <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1} mb={1.5}>
              <Box>
                <Typography variant="subtitle2">{form.id ? "Edit shift" : "Add new shift"}</Typography>
                <Typography variant="body2" color="text.secondary">
                  This screen defines the shift. The roster decides who uses it and from which date.
                </Typography>
              </Box>
              <Button startIcon={<AddIcon />} variant="outlined" onClick={() => setForm(EMPTY)}>
                New shift
              </Button>
            </Stack>

            <Alert severity="info" sx={{ mb: 2 }}>
              Workflow: create or update the shift here, then go to Shift Roster to assign employees before generating timesheets.
            </Alert>
            {missingRequired.length > 0 && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                Complete required fields: {missingRequired.join(" ")}
              </Alert>
            )}

            <Grid container spacing={1.5}>
              <Grid item xs={6} sm={3}>
                <TextField fullWidth required size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} sx={requiredFieldSx(!form.code.trim())} />
              </Grid>
              <Grid item xs={6} sm={5}>
                <TextField fullWidth required size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} sx={requiredFieldSx(!form.name.trim())} />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField select fullWidth size="small" label="Project" value={form.projectId ?? ""} onChange={(e) => setForm({ ...form, projectId: e.target.value || undefined })}>
                  <MenuItem value="">All projects</MenuItem>
                  {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={6} sm={3}>
                <TextField fullWidth required size="small" type="time" label="Start" InputLabelProps={{ shrink: true }} value={form.startTime ?? ""} onChange={(e) => setForm({ ...form, startTime: e.target.value })} sx={requiredFieldSx(!form.startTime)} />
              </Grid>
              <Grid item xs={6} sm={3}>
                <TextField fullWidth required size="small" type="time" label="End" InputLabelProps={{ shrink: true }} value={form.endTime ?? ""} onChange={(e) => setForm({ ...form, endTime: e.target.value })} sx={requiredFieldSx(!form.endTime)} />
              </Grid>
              <Grid item xs={6} sm={3}>
                <TextField fullWidth size="small" type="number" label="Break (min)" value={form.breakMinutes} onChange={(e) => setForm({ ...form, breakMinutes: Number(e.target.value) })} />
              </Grid>
              <Grid item xs={6} sm={3}>
                <TextField select fullWidth size="small" label="Status" value={form.status ?? "ACTIVE"} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  <MenuItem value="ACTIVE">ACTIVE</MenuItem>
                  <MenuItem value="INACTIVE">INACTIVE</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12}>
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
        </Grid>
      </Grid>
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
