import { Fragment, useState } from "react";
import {
  Box,
  Button,
  Chip,
  Grid,
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
import { calendarApi, periodApi, periodLockApi } from "../api/resources";

const STATUS_COLOR: Record<string, "default" | "success" | "warning" | "error"> = {
  OPEN: "success",
  LOCKED: "warning",
  CLOSED: "error",
};

export default function CalendarPage() {
  const qc = useQueryClient();
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [openWeeks, setOpenWeeks] = useState<string | null>(null);
  const [openProjects, setOpenProjects] = useState<string | null>(null);

  const { data: calendars = [] } = useQuery({ queryKey: ["calendars"], queryFn: calendarApi.list });
  const { data: periods = [] } = useQuery({
    queryKey: ["periods", year],
    queryFn: () => periodApi.list(year),
  });

  const generate = useMutation({
    mutationFn: () => periodApi.generate(year, calendars[0]?.id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["periods", year] }),
  });

  const years = Array.from({ length: 26 }, (_, i) => now.getFullYear() - 20 + i);

  return (
    <Box>
      <Typography variant="h5" mb={2}>Payroll Calendar</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={6} sm={3}>
            <TextField select fullWidth size="small" label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
              {years.map((y) => <MenuItem key={y} value={y}>{y}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={5}>
            <Button variant="contained" disabled={generate.isPending} onClick={() => generate.mutate()}>
              Initialize / generate year
            </Button>
            <Typography variant="caption" color="text.secondary" display="block" mt={0.5}>
              Creates the 12 months + their weeks. Safe to re-run — existing months are kept.
            </Typography>
          </Grid>
        </Grid>
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Month</TableCell>
              <TableCell>Dates</TableCell>
              <TableCell>Pay date</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {periods.map((p) => (
              <Fragment key={p.id}>
                <TableRow hover>
                  <TableCell>{p.name}</TableCell>
                  <TableCell>{p.startDate} → {p.endDate}</TableCell>
                  <TableCell>{p.payDate ?? "—"}</TableCell>
                  <TableCell><Chip size="small" label={p.status} color={STATUS_COLOR[p.status ?? "OPEN"] ?? "default"} /></TableCell>
                  <TableCell align="right">
                    <Button size="small" onClick={() => setOpenProjects(openProjects === p.id ? null : (p.id ?? null))}>Lock by project</Button>
                    <Button size="small" onClick={() => setOpenWeeks(openWeeks === p.id ? null : (p.id ?? null))}>Weeks</Button>
                  </TableCell>
                </TableRow>
                {openProjects === p.id && p.id && (
                  <TableRow>
                    <TableCell colSpan={5} sx={{ bgcolor: "action.hover" }}>
                      <ProjectLocksPanel periodId={p.id} />
                    </TableCell>
                  </TableRow>
                )}
                {openWeeks === p.id && (
                  <TableRow>
                    <TableCell colSpan={5} sx={{ bgcolor: "action.hover" }}>
                      <Typography variant="subtitle2" gutterBottom>Weeks</Typography>
                      <Stack spacing={0.5}>
                        {p.weeks.map((w) => (
                          <Typography key={w.id} variant="body2">Week {w.weekNo}: {w.startDate} → {w.endDate}</Typography>
                        ))}
                        {p.weeks.length === 0 && <Typography variant="body2" color="text.secondary">No weeks.</Typography>}
                      </Stack>
                    </TableCell>
                  </TableRow>
                )}
              </Fragment>
            ))}
            {periods.length === 0 && (
              <TableRow><TableCell colSpan={5}><Typography variant="body2" color="text.secondary" p={1}>No periods for {year}. Click "Initialize / generate year".</Typography></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}

function ProjectLocksPanel({ periodId }: { periodId: string }) {
  const qc = useQueryClient();
  const [payGroup, setPayGroup] = useState("ALL");
  const { data: rows = [] } = useQuery({
    queryKey: ["periodLocks", periodId, payGroup],
    queryFn: () => periodLockApi.statuses(periodId, payGroup),
  });
  const act = useMutation({
    mutationFn: ({ projectId, a }: { projectId: string; a: "lock" | "close" | "reopen" }) =>
      periodLockApi[a](periodId, projectId, payGroup),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["periodLocks", periodId, payGroup] }),
  });

  return (
    <Box>
      <Stack direction="row" spacing={1.5} alignItems="center" mb={1}>
        <Typography variant="subtitle2">Lock per project (ready for payroll)</Typography>
        <TextField select size="small" label="Pay group" value={payGroup} onChange={(e) => setPayGroup(e.target.value)} sx={{ minWidth: 160 }}>
          <MenuItem value="ALL">All</MenuItem>
          <MenuItem value="DAILY">Daily</MenuItem>
          <MenuItem value="MONTHLY">Monthly</MenuItem>
        </TextField>
      </Stack>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Project</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={r.projectId}>
              <TableCell>{r.projectLabel}</TableCell>
              <TableCell><Chip size="small" label={r.status} color={STATUS_COLOR[r.status] ?? "default"} /></TableCell>
              <TableCell align="right">
                {r.status === "OPEN" && <Button size="small" color="warning" disabled={act.isPending} onClick={() => act.mutate({ projectId: r.projectId, a: "lock" })}>Lock</Button>}
                {r.status === "LOCKED" && <Button size="small" color="error" disabled={act.isPending} onClick={() => act.mutate({ projectId: r.projectId, a: "close" })}>Close</Button>}
                {r.status !== "OPEN" && <Button size="small" disabled={act.isPending} onClick={() => act.mutate({ projectId: r.projectId, a: "reopen" })}>Reopen</Button>}
              </TableCell>
            </TableRow>
          ))}
          {rows.length === 0 && <TableRow><TableCell colSpan={3}><Typography variant="body2" color="text.secondary">No projects.</Typography></TableCell></TableRow>}
        </TableBody>
      </Table>
    </Box>
  );
}
