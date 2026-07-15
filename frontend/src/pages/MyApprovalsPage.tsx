import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import CloseIcon from "@mui/icons-material/Close";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import EditNoteIcon from "@mui/icons-material/EditNote";
import VisibilityIcon from "@mui/icons-material/Visibility";
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

async function approveTask(task: ApprovalTask, remarks?: string) {
  if (task.entityType === "LEAVE_REQUEST") {
    return approvalApi.approveLeaveRequest(task.entityId, remarks);
  }
  return approvalApi.approveTimesheet(task.entityId);
}

export default function MyApprovalsPage() {
  const qc = useQueryClient();
  const [query, setQuery] = useState("");
  const [project, setProject] = useState("ALL");
  const [period, setPeriod] = useState("ALL");
  const [page, setPage] = useState(0);
  const [detailTask, setDetailTask] = useState<ApprovalTask | null>(null);
  const [decisionTask, setDecisionTask] = useState<ApprovalTask | null>(null);
  const [decision, setDecision] = useState<"APPROVE" | "REJECT" | "RETURN">("APPROVE");
  const [remarks, setRemarks] = useState("");
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
  const leaveDecision = useMutation({
    mutationFn: async () => {
      if (!decisionTask) return;
      if (decision === "REJECT") return approvalApi.rejectLeaveRequest(decisionTask.entityId, remarks);
      if (decision === "RETURN") return approvalApi.returnLeaveRequest(decisionTask.entityId, remarks);
      return approvalApi.approveLeaveRequest(decisionTask.entityId, remarks);
    },
    onSuccess: () => {
      setDecisionTask(null);
      setRemarks("");
      qc.invalidateQueries({ queryKey: ["myApprovalTasks"] });
      qc.invalidateQueries({ queryKey: ["leaveRequests"] });
      qc.invalidateQueries({ queryKey: ["timesheets"] });
      qc.invalidateQueries({ queryKey: ["timesheet"] });
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
    for (const task of rows.filter((t) => t.entityType === "TIMESHEET")) {
      await approveTask(task);
    }
    await qc.invalidateQueries({ queryKey: ["myApprovalTasks"] });
    await qc.invalidateQueries({ queryKey: ["timesheets"] });
    await qc.invalidateQueries({ queryKey: ["timesheet"] });
    await qc.invalidateQueries({ queryKey: ["leaveRequests"] });
  };

  const approveVisible = useMutation({ mutationFn: () => approveMany(visible) });
  const approveFiltered = useMutation({ mutationFn: () => approveMany(filtered) });
  const busy = approve.isPending || approveVisible.isPending || approveFiltered.isPending || leaveDecision.isPending;
  const visibleTimesheets = visible.filter((t) => t.entityType === "TIMESHEET").length;
  const filteredTimesheets = filtered.filter((t) => t.entityType === "TIMESHEET").length;

  const openDecision = (task: ApprovalTask, next: "APPROVE" | "REJECT" | "RETURN") => {
    setDecisionTask(task);
    setDecision(next);
    setRemarks("");
  };

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
            disabled={busy || visibleTimesheets === 0}
            onClick={() => approveVisible.mutate()}
          >
            Approve visible timesheets ({visibleTimesheets})
          </Button>
          <Button
            variant="contained"
            startIcon={<DoneAllIcon />}
            disabled={busy || filteredTimesheets === 0}
            onClick={() => approveFiltered.mutate()}
          >
            Approve all filtered timesheets ({filteredTimesheets})
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
      {leaveDecision.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {(leaveDecision.error as any)?.response?.data?.message ?? "Could not submit this leave decision."}
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
                  <Stack direction="row" gap={0.5} justifyContent="flex-end" flexWrap="wrap">
                    <Button
                      size="small"
                      startIcon={<VisibilityIcon />}
                      onClick={() => setDetailTask(task)}
                    >
                      Details
                    </Button>
                  {task.entityType === "TIMESHEET" && (
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
                  {task.entityType === "LEAVE_REQUEST" && (
                    <>
                      <Button size="small" color="success" startIcon={<CheckIcon />} onClick={() => openDecision(task, "APPROVE")}>
                        Approve
                      </Button>
                      <Button size="small" color="warning" startIcon={<EditNoteIcon />} onClick={() => openDecision(task, "RETURN")}>
                        Return
                      </Button>
                      <Button size="small" color="error" startIcon={<CloseIcon />} onClick={() => openDecision(task, "REJECT")}>
                        Reject
                      </Button>
                    </>
                  )}
                  </Stack>
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
      <LeaveDetailsDialog task={detailTask} onClose={() => setDetailTask(null)} />
      <Dialog open={!!decisionTask} onClose={() => setDecisionTask(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{decision === "APPROVE" ? "Approve leave" : decision === "REJECT" ? "Reject leave" : "Return leave for correction"}</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} mt={1}>
            <Typography variant="body2">
              {decisionTask?.employeeNumber} - {decisionTask?.employeeName}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {decisionTask?.leaveTypeCode} - {decisionTask?.leaveTypeName} | {periodOrDates(decisionTask ?? {} as ApprovalTask)} | {num(decisionTask?.leaveTotalDays)} day(s)
            </Typography>
            <TextField
              label={decision === "APPROVE" ? "Comment optional" : "Comment required"}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              multiline
              minRows={4}
              fullWidth
              required={decision !== "APPROVE"}
              error={decision !== "APPROVE" && !remarks.trim()}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDecisionTask(null)}>Cancel</Button>
          <Button
            variant="contained"
            color={decision === "REJECT" ? "error" : decision === "RETURN" ? "warning" : "success"}
            disabled={leaveDecision.isPending || (decision !== "APPROVE" && !remarks.trim())}
            onClick={() => leaveDecision.mutate()}
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

function LeaveDetailsDialog({ task, onClose }: { task: ApprovalTask | null; onClose: () => void }) {
  if (!task) return null;
  const isLeave = task.entityType === "LEAVE_REQUEST";
  return (
    <Dialog open={!!task} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Approval details</DialogTitle>
      <DialogContent>
        <Stack spacing={2} mt={1}>
          <Stack direction={{ xs: "column", md: "row" }} gap={1.5} flexWrap="wrap">
            <Info label="Process" value={task.processCode} />
            <Info label="Project" value={task.projectCode ?? "-"} />
            <Info label="Employee" value={`${task.employeeNumber ?? ""} - ${task.employeeName ?? ""}`} />
            <Info label="Step" value={`${task.stepOrder} - ${task.stepName}`} />
          </Stack>
          {isLeave && (
            <>
              <Divider />
              <Stack direction={{ xs: "column", md: "row" }} gap={1.5} flexWrap="wrap">
                <Info label="Leave type" value={`${task.leaveTypeCode ?? "-"} - ${task.leaveTypeName ?? ""}`} />
                <Info label="Dates" value={periodOrDates(task)} />
                <Info label="Return date" value={task.leaveReturnDate ?? "-"} />
                <Info label="Days" value={num(task.leaveTotalDays)} />
                <Info label="Status" value={task.leaveStatus ?? "-"} />
              </Stack>
              <Info label="Reason" value={task.leaveReason ?? "-"} wide />
              {task.leaveRequiresTicket && (
                <>
                  <Divider />
                  <Typography variant="subtitle2">Ticket</Typography>
                  <Stack direction={{ xs: "column", md: "row" }} gap={1.5} flexWrap="wrap">
                    <Info label="Route" value={`${task.leaveTicketFrom ?? "-"} -> ${task.leaveTicketTo ?? "-"}`} />
                    <Info label="Departure" value={task.leaveTravelDate ?? "-"} />
                    <Info label="Return flight" value={task.leaveReturnTravelDate ?? "-"} />
                    <Info label="Passport" value={task.leavePassportNumber ?? "-"} />
                    <Info label="Dependents" value={num(task.leaveDependentCount)} />
                  </Stack>
                  <Info label="Ticket remarks" value={task.leaveTravelRemarks ?? "-"} wide />
                </>
              )}
              <Divider />
              <Typography variant="subtitle2">Contact during leave</Typography>
              <Stack direction={{ xs: "column", md: "row" }} gap={1.5} flexWrap="wrap">
                <Info label="Phone" value={task.leaveContactPhone ?? "-"} />
                <Info label="Email" value={task.leaveContactEmail ?? "-"} />
                <Info label="Emergency contact" value={task.leaveEmergencyContactName ?? "-"} />
                <Info label="Emergency phone" value={task.leaveEmergencyContactPhone ?? "-"} />
              </Stack>
              <Info label="Address" value={task.leaveAddressDuringLeave ?? "-"} wide />
            </>
          )}
          <Divider />
          <Typography variant="subtitle2">Approval history</Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Step</TableCell>
                <TableCell>Approver</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Decision</TableCell>
                <TableCell>Remarks</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(task.history ?? []).map((row) => (
                <TableRow key={`${row.stepOrder}-${row.approverNumber ?? row.stepName}`}>
                  <TableCell>{row.stepOrder} - {row.stepName}</TableCell>
                  <TableCell>{row.approverNumber ?? "-"} {row.approverName ? `- ${row.approverName}` : ""}</TableCell>
                  <TableCell>{row.status}</TableCell>
                  <TableCell>{row.decidedAt ? new Date(row.decidedAt).toLocaleString() : "-"}</TableCell>
                  <TableCell>{row.remarks ?? "-"}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

function Info({ label, value, wide = false }: { label: string; value: string | number; wide?: boolean }) {
  return (
    <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 1.5, minWidth: wide ? "100%" : 180, flex: wide ? "1 1 100%" : "1 1 180px" }}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="body2">{value}</Typography>
    </Paper>
  );
}
