import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
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
import CalendarMonthIcon from "@mui/icons-material/CalendarMonth";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import LockIcon from "@mui/icons-material/Lock";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import ViewWeekIcon from "@mui/icons-material/ViewWeek";
import { calendarApi, lookupApi, periodApi, periodLockApi } from "../api/resources";
import type { PayrollPeriod } from "../api/types";

const STATUS_COLOR: Record<string, "default" | "success" | "warning" | "error"> = {
  OPEN: "success",
  LOCKED: "warning",
  CLOSED: "error",
};

const dateLabel = (value?: string) => value || "-";
const daysBetweenInclusive = (start?: string, end?: string) => {
  if (!start || !end) return 0;
  const s = new Date(`${start}T00:00:00`);
  const e = new Date(`${end}T00:00:00`);
  return Math.max(0, Math.round((e.getTime() - s.getTime()) / 86400000) + 1);
};

export default function CalendarPage() {
  const qc = useQueryClient();
  const now = new Date();
  const currentYear = now.getFullYear();
  const [year, setYear] = useState(currentYear);
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

  const statusCounts = periods.reduce<Record<string, number>>((acc, period) => {
    const status = period.status ?? "OPEN";
    acc[status] = (acc[status] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5">Payroll Calendar</Typography>
          <Typography variant="body2" color="text.secondary">
            Build the payroll year, review monthly periods, and lock each project/pay group when ready for payroll.
          </Typography>
        </Box>
        <Chip icon={<CalendarMonthIcon />} label="Generate once per year, re-run safely if months are missing." variant="outlined" />
      </Stack>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} md={5}>
            <Stack direction="row" spacing={1} alignItems="center">
              <IconButton aria-label="Previous year" onClick={() => setYear((y) => y - 1)}>
                <ChevronLeftIcon />
              </IconButton>
              <TextField
                size="small"
                type="number"
                label="Payroll year"
                value={year}
                onChange={(e) => setYear(Number(e.target.value) || currentYear)}
                sx={{ width: 150 }}
              />
              <IconButton aria-label="Next year" onClick={() => setYear((y) => y + 1)}>
                <ChevronRightIcon />
              </IconButton>
              <Button size="small" startIcon={<RestartAltIcon />} onClick={() => setYear(currentYear)}>
                Current
              </Button>
            </Stack>
          </Grid>
          <Grid item xs={12} md={4}>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Chip size="small" label={`${periods.length} months`} />
              <Chip size="small" label={`${statusCounts.OPEN ?? 0} open`} color="success" />
              <Chip size="small" label={`${statusCounts.LOCKED ?? 0} locked`} color="warning" />
              <Chip size="small" label={`${statusCounts.CLOSED ?? 0} closed`} color="error" />
            </Stack>
          </Grid>
          <Grid item xs={12} md={3}>
            <Button fullWidth variant="contained" disabled={generate.isPending} onClick={() => generate.mutate()}>
              Initialize / generate year
            </Button>
          </Grid>
        </Grid>
        <Typography variant="caption" color="text.secondary" display="block" mt={1}>
          Creates the 12 months and their weeks. Safe to re-run because existing months are kept.
        </Typography>
      </Paper>

      {periods.length === 0 && (
        <Alert severity="info" sx={{ mb: 2 }}>
          No periods for {year}. Click Initialize / generate year to create the 12 payroll months.
        </Alert>
      )}

      <Grid container spacing={1.5}>
        {periods.map((period) => (
          <Grid item xs={12} md={6} xl={4} key={period.id}>
            <PeriodCard
              period={period}
              openProjects={openProjects === period.id}
              openWeeks={openWeeks === period.id}
              onToggleProjects={() => setOpenProjects(openProjects === period.id ? null : (period.id ?? null))}
              onToggleWeeks={() => setOpenWeeks(openWeeks === period.id ? null : (period.id ?? null))}
            />
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

function PeriodCard({
  period,
  openProjects,
  openWeeks,
  onToggleProjects,
  onToggleWeeks,
}: {
  period: PayrollPeriod;
  openProjects: boolean;
  openWeeks: boolean;
  onToggleProjects: () => void;
  onToggleWeeks: () => void;
}) {
  const dayCount = daysBetweenInclusive(period.startDate, period.endDate);
  const status = period.status ?? "OPEN";

  return (
    <Card variant="outlined" sx={{ borderRadius: 2, height: "100%" }}>
      <CardContent>
        <Stack spacing={1.5}>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
            <Box>
              <Typography variant="h6">{period.name}</Typography>
              <Typography variant="body2" color="text.secondary">{dateLabel(period.startDate)} - {dateLabel(period.endDate)}</Typography>
            </Box>
            <Chip size="small" label={status} color={STATUS_COLOR[status] ?? "default"} />
          </Stack>

          <Grid container spacing={1}>
            <Grid item xs={4}><MiniMetric label="Days" value={dayCount} /></Grid>
            <Grid item xs={4}><MiniMetric label="Weeks" value={period.weeks?.length ?? 0} /></Grid>
            <Grid item xs={4}><MiniMetric label="Pay date" value={period.payDate ? period.payDate.slice(5) : "-"} /></Grid>
          </Grid>

          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Button size="small" startIcon={<LockIcon />} onClick={onToggleProjects} variant={openProjects ? "contained" : "outlined"}>
              Lock by project
            </Button>
            <Button size="small" startIcon={<ViewWeekIcon />} onClick={onToggleWeeks} variant={openWeeks ? "contained" : "outlined"}>
              Weeks
            </Button>
          </Stack>

          {openWeeks && (
            <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 1.5, bgcolor: "background.default" }}>
              <Typography variant="subtitle2" mb={0.75}>Weeks inside this month</Typography>
              <Stack spacing={0.5}>
                {period.weeks.map((w) => (
                  <Typography key={w.id} variant="body2">Week {w.weekNo}: {w.startDate} - {w.endDate}</Typography>
                ))}
                {period.weeks.length === 0 && <Typography variant="body2" color="text.secondary">No weeks.</Typography>}
              </Stack>
            </Paper>
          )}

          {openProjects && period.id && (
            <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 1.5, bgcolor: "background.default" }}>
              <ProjectLocksPanel periodId={period.id} />
            </Paper>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}

function MiniMetric({ label, value }: { label: string; value: string | number }) {
  return (
    <Paper variant="outlined" sx={{ p: 1, borderRadius: 1.5 }}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="subtitle2">{value}</Typography>
    </Paper>
  );
}

function ProjectLocksPanel({ periodId }: { periodId: string }) {
  const qc = useQueryClient();
  const [payGroup, setPayGroup] = useState("");
  const { data: payGroups = [] } = useQuery({ queryKey: ["lookup", "PAY_STATUS"], queryFn: () => lookupApi.byCategory("PAY_STATUS") });
  useEffect(() => {
    if (!payGroup && payGroups.length > 0) {
      setPayGroup(payGroups[0].code ?? "");
    }
  }, [payGroup, payGroups]);
  const { data: rows = [] } = useQuery({
    queryKey: ["periodLocks", periodId, payGroup],
    queryFn: () => periodLockApi.statuses(periodId, payGroup),
    enabled: !!payGroup,
  });
  const act = useMutation({
    mutationFn: ({ projectId, a }: { projectId: string; a: "lock" | "close" | "reopen" }) =>
      periodLockApi[a](periodId, projectId, payGroup),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["periodLocks", periodId, payGroup] }),
  });

  return (
    <Box>
      <Stack direction={{ xs: "column", sm: "row" }} spacing={1} alignItems={{ xs: "stretch", sm: "center" }} mb={1}>
        <Typography variant="subtitle2" sx={{ flex: 1 }}>Project locks</Typography>
        <TextField select size="small" label="Pay group" value={payGroup} onChange={(e) => setPayGroup(e.target.value)} sx={{ minWidth: 160 }}>
          <MenuItem value="" disabled>Select pay group</MenuItem>
          {payGroups.map((g) => (
            <MenuItem key={g.code} value={g.code}>{g.label || g.code}</MenuItem>
          ))}
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
                {r.status === "LOCKED" && <Button size="small" disabled={act.isPending} onClick={() => act.mutate({ projectId: r.projectId, a: "reopen" })}>Reopen</Button>}
              </TableCell>
            </TableRow>
          ))}
          {rows.length === 0 && <TableRow><TableCell colSpan={3}><Typography variant="body2" color="text.secondary">No projects.</Typography></TableCell></TableRow>}
        </TableBody>
      </Table>
    </Box>
  );
}
