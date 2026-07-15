import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  LinearProgress,
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
import CheckIcon from "@mui/icons-material/Check";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { approvalApi } from "../api/resources";
import type { ApprovalTask } from "../api/types";

const PAGE_SIZE = 50;

function monthLabel(year?: number, month?: number) {
  if (!year || !month) return "-";
  return new Date(year, month - 1, 1).toLocaleDateString(undefined, { month: "short", year: "numeric" });
}

function num(value?: number) {
  return Number(value ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 });
}

function approvalKey(task: ApprovalTask) {
  if (task.entityType === "LEAVE_REQUEST") {
    return task.leaveStartDate ? `LEAVE-${task.leaveStartDate.slice(0, 7)}` : "LEAVE";
  }
  return `${task.periodYear ?? ""}-${task.periodMonth ?? ""}`;
}

function periodOrDates(task: ApprovalTask) {
  if (task.entityType === "LEAVE_REQUEST") {
    return task.leaveStartDate && task.leaveEndDate ? `${task.leaveStartDate} - ${task.leaveEndDate}` : "-";
  }
  return monthLabel(task.periodYear, task.periodMonth);
}

function approvalStatus(task: ApprovalTask) {
  return task.entityType === "LEAVE_REQUEST" ? task.leaveStatus ?? task.status : task.timesheetStatus ?? task.status;
}

async function approveTask(task: ApprovalTask) {
  if (task.entityType === "LEAVE_REQUEST") {
    return approvalApi.approveLeaveRequest(task.entityId);
  }
  return approvalApi.approveTimesheet(task.entityId);
}

export default function MyApprovalsPage() {
  const qc = useQueryClient();
  const [query, setQuery] = useState("");
  const [project, setProject] = useState("ALL");
  const [period, setPeriod] = useState("ALL");
  const [page, setPage] = useState(0);
  const { data: tasks = [], isLoading } = useQuery({ queryKey: ["myApprovalTasks"], queryFn: approvalApi.myTasks });
  const approve = useMutation({
    mutationFn: (task: ApprovalTask) => approveTask(task),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["myApprovalTasks"] });
      qc.invalidateQueries({ queryKey: ["timesheets"] });
      qc.invalidateQueries({ queryKey: ["timesheet"] });
      qc.invalidateQueries({ queryKey: ["leaveRequests"] });
    },
  });

  const projects = useMemo(() => {
    return Array.from(new Set(tasks.map((t) => t.projectCode).filter(Boolean))).sort() as string[];
  }, [tasks]);

  const periods = useMemo(() => {
    return Array.from(new Map(tasks
      .filter((t) => (t.periodYear && t.periodMonth) || t.leaveStartDate)
      .map((t) => [approvalKey(t), t.entityType === "LEAVE_REQUEST" && t.leaveStartDate ? t.leaveStartDate.slice(0, 7) : monthLabel(t.periodYear, t.periodMonth)])
    ).entries());
  }, [tasks]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return tasks.filter((task) => {
      const text = [
        task.employeeNumber,
        task.employeeName,
        task.projectCode,
        task.processCode,
        task.payGroup,
        task.leaveTypeCode,
        task.leaveTypeName,
      ].join(" ").toLowerCase();
      return (project === "ALL" || task.projectCode === project)
        && (period === "ALL" || approvalKey(task) === period)
        && (!q || text.includes(q));
    });
  }, [tasks, project, period, query]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const visible = filtered.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);

  const approveMany = async (rows: ApprovalTask[]) => {
    for (const task of rows.filter((t) => t.entityType === "TIMESHEET" || t.entityType === "LEAVE_REQUEST")) {
      await approveTask(task);
    }
    await qc.invalidateQueries({ queryKey: ["myApprovalTasks"] });
    await qc.invalidateQueries({ queryKey: ["timesheets"] });
    await qc.invalidateQueries({ queryKey: ["timesheet"] });
    await qc.invalidateQueries({ queryKey: ["leaveRequests"] });
  };

  const approveVisible = useMutation({ mutationFn: () => approveMany(visible) });
  const approveFiltered = useMutation({ mutationFn: () => approveMany(filtered) });
  const busy = approve.isPending || approveVisible.isPending || approveFiltered.isPending;

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} alignItems={{ md: "center" }} justifyContent="space-between" gap={2} mb={2}>
        <Box>
          <Typography variant="h5">My Approvals</Typography>
          <Typography variant="body2" color="text.secondary">
            Review pending workflow tasks by project, period, and employee.
          </Typography>
        </Box>
        <Stack direction="row" gap={1} flexWrap="wrap">
          <Button
            variant="outlined"
            startIcon={<DoneAllIcon />}
            disabled={busy || visible.length === 0}
            onClick={() => approveVisible.mutate()}
          >
            Approve visible ({visible.length})
          </Button>
          <Button
            variant="contained"
            startIcon={<DoneAllIcon />}
            disabled={busy || filtered.length === 0}
            onClick={() => approveFiltered.mutate()}
          >
            Approve all filtered ({filtered.length})
          </Button>
        </Stack>
      </Stack>
      {approve.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {(approve.error as any)?.response?.data?.message ?? "Could not approve this task."}
        </Alert>
      )}
      {(approveVisible.isError || approveFiltered.isError) && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {((approveVisible.error || approveFiltered.error) as any)?.response?.data?.message ?? "Could not approve all selected tasks."}
        </Alert>
      )}
      <Stack direction={{ xs: "column", md: "row" }} gap={2} mb={2}>
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, minWidth: 160 }}>
          <Typography variant="caption" color="text.secondary">Pending tasks</Typography>
          <Typography variant="h5">{tasks.length}</Typography>
        </Paper>
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, minWidth: 160 }}>
          <Typography variant="caption" color="text.secondary">Visible after filters</Typography>
          <Typography variant="h5">{filtered.length}</Typography>
        </Paper>
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, minWidth: 160 }}>
          <Typography variant="caption" color="text.secondary">Projects</Typography>
          <Typography variant="h5">{projects.length}</Typography>
        </Paper>
      </Stack>
      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
        {busy && <LinearProgress />}
        <Stack direction={{ xs: "column", md: "row" }} gap={1.5} p={2}>
          <TextField
            size="small"
            label="Search employee"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setPage(0); }}
            sx={{ minWidth: 260, flex: 1 }}
          />
          <TextField
            select
            size="small"
            label="Project"
            value={project}
            onChange={(e) => { setProject(e.target.value); setPage(0); }}
            sx={{ minWidth: 180 }}
          >
            <MenuItem value="ALL">All projects</MenuItem>
            {projects.map((p) => <MenuItem key={p} value={p}>{p}</MenuItem>)}
          </TextField>
          <TextField
            select
            size="small"
            label="Period"
            value={period}
            onChange={(e) => { setPeriod(e.target.value); setPage(0); }}
            sx={{ minWidth: 180 }}
          >
            <MenuItem value="ALL">All periods</MenuItem>
            {periods.map(([key, label]) => <MenuItem key={key} value={key}>{label}</MenuItem>)}
          </TextField>
        </Stack>
        <Divider />
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Process</TableCell>
              <TableCell>Period</TableCell>
              <TableCell>Project</TableCell>
              <TableCell>Employee</TableCell>
              <TableCell>Pay</TableCell>
              <TableCell align="right">Worked</TableCell>
              <TableCell align="right">OT</TableCell>
              <TableCell align="right">Absence</TableCell>
              <TableCell align="right">Leave days</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Step</TableCell>
              <TableCell>Submitted</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {visible.map((task) => (
              <TableRow key={task.stepId} hover>
                <TableCell><Chip size="small" label={task.processCode} /></TableCell>
                <TableCell>{periodOrDates(task)}</TableCell>
                <TableCell>{task.projectCode ?? "-"}</TableCell>
                <TableCell>
                  <Stack spacing={0.25}>
                    <Typography variant="body2">{task.employeeNumber} - {task.employeeName}</Typography>
                    {task.entityType === "LEAVE_REQUEST" && (
                      <Typography variant="caption" color="text.secondary">
                        {task.leaveTypeCode ?? "-"} {task.leaveTypeName ? `- ${task.leaveTypeName}` : ""}
                      </Typography>
                    )}
                  </Stack>
                </TableCell>
                <TableCell>{task.payGroup ?? "-"}</TableCell>
                <TableCell align="right">{task.entityType === "TIMESHEET" ? num(task.totalWorkedHours) : "-"}</TableCell>
                <TableCell align="right">{task.entityType === "TIMESHEET" ? num(task.totalOtHours) : "-"}</TableCell>
                <TableCell align="right">{task.entityType === "TIMESHEET" ? num(task.totalAbsenceDays) : "-"}</TableCell>
                <TableCell align="right">{task.entityType === "LEAVE_REQUEST" ? num(task.leaveTotalDays) : "-"}</TableCell>
                <TableCell><Chip size="small" color="warning" variant="outlined" label={approvalStatus(task)} /></TableCell>
                <TableCell>
                  <Stack spacing={0.25}>
                    <Typography variant="body2">{task.stepName}</Typography>
                    <Typography variant="caption" color="text.secondary">Step {task.stepOrder}</Typography>
                  </Stack>
                </TableCell>
                <TableCell>{task.submittedAt ? new Date(task.submittedAt).toLocaleString() : ""}</TableCell>
                <TableCell align="right">
                  {(task.entityType === "TIMESHEET" || task.entityType === "LEAVE_REQUEST") && (
                    <Button
                      size="small"
                      color="success"
                      startIcon={<CheckIcon />}
                      disabled={approve.isPending}
                      onClick={() => approve.mutate(task)}
                    >
                      Approve
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {!isLoading && tasks.length === 0 && (
              <TableRow>
                <TableCell colSpan={13}>
                  <Typography variant="body2" color="text.secondary" p={1}>No pending approvals.</Typography>
                </TableCell>
              </TableRow>
            )}
            {!isLoading && tasks.length > 0 && filtered.length === 0 && (
              <TableRow>
                <TableCell colSpan={13}>
                  <Typography variant="body2" color="text.secondary" p={1}>No approvals match the selected filters.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <Stack direction="row" alignItems="center" justifyContent="space-between" p={1.5}>
          <Typography variant="body2" color="text.secondary">
            Showing {visible.length} of {filtered.length} approvals - page {safePage + 1} of {totalPages}
          </Typography>
          <Stack direction="row" gap={1}>
            <Button size="small" variant="outlined" disabled={safePage === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>
              Previous
            </Button>
            <Button size="small" variant="outlined" disabled={safePage >= totalPages - 1} onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}>
              Next
            </Button>
          </Stack>
        </Stack>
      </Paper>
    </Box>
  );
}
