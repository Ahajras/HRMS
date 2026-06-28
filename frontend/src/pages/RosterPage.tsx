import { useState } from "react";
import {
  Box,
  Button,
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
import { employeeApi, employeeShiftApi, shiftApi } from "../api/resources";
import type { EmployeeShift } from "../api/types";

const today = () => new Date().toISOString().slice(0, 10);
const EMPTY: EmployeeShift = { employeeId: "", shiftId: "", effectiveFrom: today() };

export default function RosterPage() {
  const qc = useQueryClient();
  const [form, setForm] = useState<EmployeeShift>(EMPTY);

  const { data: employees } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500) });
  const { data: shifts = [] } = useQuery({ queryKey: ["shifts"], queryFn: shiftApi.list });
  const { data: roster = [] } = useQuery({ queryKey: ["roster"], queryFn: () => employeeShiftApi.list() });

  const save = useMutation({
    mutationFn: (r: EmployeeShift) => (r.id ? employeeShiftApi.update(r.id, r) : employeeShiftApi.create(r)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["roster"] }); setForm(EMPTY); },
  });
  const del = useMutation({
    mutationFn: (id: string) => employeeShiftApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["roster"] }),
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Shift Roster</Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Assign each employee to a shift. Timesheet generation uses the shift that is in effect for the period.
      </Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit assignment" : "Assign employee to shift"}</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Employee" value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}>
              {(employees?.content ?? []).map((emp) => (
                <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} — {emp.firstName} {emp.lastName}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField select fullWidth size="small" label="Shift" value={form.shiftId} onChange={(e) => setForm({ ...form, shiftId: e.target.value })}>
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
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.employeeId || !form.shiftId || save.isPending} onClick={() => save.mutate(form)}>
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
            {roster.map((r) => (
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
            {roster.length === 0 && (
              <TableRow><TableCell colSpan={5}><Typography variant="body2" color="text.secondary" p={1}>No assignments yet.</Typography></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}
