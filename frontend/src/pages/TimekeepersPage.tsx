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
import { employeeApi, projectApi, timekeeperApi } from "../api/resources";
import type { TimekeeperProject } from "../api/types";

export default function TimekeepersPage() {
  const qc = useQueryClient();
  const { data: rows = [] } = useQuery({ queryKey: ["timekeeperProjects"], queryFn: timekeeperApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: employees } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500) });
  const empList = employees?.content ?? [];

  const [employeeId, setEmployeeId] = useState("");
  const [projectId, setProjectId] = useState("");

  const save = useMutation({
    mutationFn: () => timekeeperApi.create({ employeeId, projectId } as TimekeeperProject),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["timekeeperProjects"] }); setEmployeeId(""); setProjectId(""); },
  });
  const del = useMutation({
    mutationFn: (id: string) => timekeeperApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["timekeeperProjects"] }),
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Timekeepers</Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Link a timekeeper (employee) to the project(s) they may enter attendance for. A timekeeper can be on more than one project.
      </Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={5}>
            <TextField select fullWidth size="small" label="Timekeeper (employee)" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
              {empList.map((emp) => <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} — {emp.firstName} {emp.lastName}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Project" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={3}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!employeeId || !projectId || save.isPending} onClick={() => save.mutate()}>
                Assign
              </Button>
            </Stack>
          </Grid>
        </Grid>
        {save.isError && <Typography color="error" variant="body2" mt={1}>{(save.error as any)?.response?.data?.message ?? "Failed."}</Typography>}
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Timekeeper</TableCell>
              <TableCell>Project</TableCell>
              <TableCell align="right" />
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.id} hover>
                <TableCell>{r.employeeNumber} — {r.employeeName}</TableCell>
                <TableCell>{r.projectCode}</TableCell>
                <TableCell align="right"><IconButton size="small" color="error" onClick={() => r.id && del.mutate(r.id)}><DeleteIcon fontSize="small" /></IconButton></TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && <TableRow><TableCell colSpan={3}><Typography variant="body2" color="text.secondary" p={1}>No timekeepers assigned yet.</Typography></TableCell></TableRow>}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}
