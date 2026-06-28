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
import { calendarApi, periodApi } from "../api/resources";

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

  const { data: calendars = [] } = useQuery({ queryKey: ["calendars"], queryFn: calendarApi.list });
  const { data: periods = [] } = useQuery({
    queryKey: ["periods", year],
    queryFn: () => periodApi.list(year),
  });

  const generate = useMutation({
    mutationFn: () => periodApi.generate(year, calendars[0]?.id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["periods", year] }),
  });
  const action = useMutation({
    mutationFn: ({ id, act }: { id: string; act: "lock" | "close" | "reopen" }) => periodApi[act](id),
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
                    <Button size="small" onClick={() => setOpenWeeks(openWeeks === p.id ? null : (p.id ?? null))}>Weeks</Button>
                    {p.status === "OPEN" && <Button size="small" color="warning" onClick={() => p.id && action.mutate({ id: p.id, act: "lock" })}>Lock</Button>}
                    {p.status === "LOCKED" && <Button size="small" color="error" onClick={() => p.id && action.mutate({ id: p.id, act: "close" })}>Close</Button>}
                    {p.status !== "CLOSED" && p.status !== "OPEN" && <Button size="small" onClick={() => p.id && action.mutate({ id: p.id, act: "reopen" })}>Reopen</Button>}
                  </TableCell>
                </TableRow>
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
              </>
            ))}
            {periods.length === 0 && (
              <TableRow><TableCell colSpan={5}><Typography variant="body2" color="text.secondary" p={1}>No periods for {year}. Click "Initialize / generate year".</Typography></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      {action.isError && <Typography color="error" variant="body2" mt={1}>{(action.error as any)?.response?.data?.message ?? "Action failed."}</Typography>}
    </Box>
  );
}
