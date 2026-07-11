import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
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
import { auditApi, employeeApi } from "../api/resources";
import type { Employee } from "../api/types";

const money = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function AuditToolsPage() {
  return (
    <Box>
      <Typography variant="h5" mb={1}>Audit Tools</Typography>
      <Alert severity="warning" sx={{ mb: 2 }}>
        Manager-only cleanup utilities. Every delete here is guarded — only PENDING Day Zero adjustments and DRAFT/CALCULATED
        payroll runs can be removed. Anything already applied, approved, or locked must be reopened from its normal screen instead.
      </Alert>
      <DayZeroAdjustmentsPanel />
      <Box mt={4}>
        <TimeUsagePanel />
      </Box>
      <Box mt={4}>
        <LeaveDiscrepancyPanel />
      </Box>
      <Box mt={4}>
        <PayrollRunsPanel />
      </Box>
    </Box>
  );
}

function DayZeroAdjustmentsPanel() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const { data: matchesPage } = useQuery({
    queryKey: ["auditEmployeeSearch", search],
    queryFn: () => employeeApi.list(0, 10, search),
    enabled: search.length >= 2 && !employee,
  });
  const matches = matchesPage?.content ?? [];

  const { data: adjustments = [] } = useQuery({
    queryKey: ["auditDayZeroAdjustments", employee?.id],
    queryFn: () => auditApi.dayZeroAdjustments(employee!.id!),
    enabled: !!employee?.id,
  });

  const del = useMutation({
    mutationFn: (id: string) => auditApi.deleteDayZeroAdjustment(id),
    onSuccess: () => {
      setMessage("Adjustment deleted.");
      qc.invalidateQueries({ queryKey: ["auditDayZeroAdjustments", employee?.id] });
    },
    onError: (e: any) => setMessage(e?.response?.data?.message ?? "Could not delete — it may already be applied."),
  });

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Typography variant="subtitle1" fontWeight={700} mb={1}>Day Zero adjustments</Typography>
      {!employee ? (
        <TextField
          fullWidth
          size="small"
          label="Search employee (name or number)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      ) : (
        <Stack direction="row" spacing={2} alignItems="center" mb={1}>
          <Typography variant="body2" fontWeight={700}>{employee.employeeNumber} — {employee.firstName} {employee.lastName}</Typography>
          <Button size="small" onClick={() => { setEmployee(null); setSearch(""); setMessage(null); }}>Change employee</Button>
        </Stack>
      )}
      {!employee && search.length >= 2 && (
        <Paper variant="outlined" sx={{ mt: 1, maxHeight: 200, overflow: "auto" }}>
          {matches.map((e) => (
            <Box key={e.id} sx={{ p: 1, cursor: "pointer", "&:hover": { bgcolor: "action.hover" } }} onClick={() => { setEmployee(e); setSearch(""); }}>
              {e.employeeNumber} — {e.firstName} {e.lastName}
            </Box>
          ))}
          {matches.length === 0 && <Box sx={{ p: 1 }}><Typography variant="body2" color="text.secondary">No matches.</Typography></Box>}
        </Paper>
      )}

      {employee && (
        <>
          {message && <Alert severity="info" sx={{ my: 1 }} onClose={() => setMessage(null)}>{message}</Alert>}
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Date</TableCell>
                <TableCell>Period</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Reason</TableCell>
                <TableCell align="right">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {adjustments.map((a) => (
                <TableRow key={a.id} hover>
                  <TableCell>{a.workDate}</TableCell>
                  <TableCell>{a.periodYear}-{String(a.periodMonth).padStart(2, "0")}</TableCell>
                  <TableCell align="right" sx={{ color: a.amount < 0 ? "error.main" : "success.main" }}>{money(a.amount)}</TableCell>
                  <TableCell><Chip size="small" label={a.status} color={a.status === "PENDING" ? "warning" : "success"} /></TableCell>
                  <TableCell sx={{ maxWidth: 320, whiteSpace: "pre-wrap", fontSize: 12 }}>{a.reason}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small" color="error" disabled={a.status !== "PENDING" || del.isPending}
                      onClick={() => { if (confirm("Delete this Day Zero adjustment? This cannot be undone.")) del.mutate(a.id); }}
                    >
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {adjustments.length === 0 && (
                <TableRow><TableCell colSpan={6}><Typography variant="body2" color="text.secondary" p={1}>No adjustments for this employee.</Typography></TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </>
      )}
    </Paper>
  );
}

function PayrollRunsPanel() {
  const qc = useQueryClient();
  const [message, setMessage] = useState<string | null>(null);
  const { data: runs = [] } = useQuery({ queryKey: ["auditPayrollRuns"], queryFn: () => auditApi.payrollRuns(50) });

  const del = useMutation({
    mutationFn: (id: string) => auditApi.deletePayrollRun(id),
    onSuccess: () => {
      setMessage("Run deleted.");
      qc.invalidateQueries({ queryKey: ["auditPayrollRuns"] });
    },
    onError: (e: any) => setMessage(e?.response?.data?.message ?? "Could not delete — it may already be approved or locked."),
  });

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Typography variant="subtitle1" fontWeight={700} mb={1}>Payroll runs (most recent 50)</Typography>
      {message && <Alert severity="info" sx={{ mb: 1 }} onClose={() => setMessage(null)}>{message}</Alert>}
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Period</TableCell>
            <TableCell>Project</TableCell>
            <TableCell>Pay group</TableCell>
            <TableCell align="right">Employees</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Action</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {runs.map((r) => (
            <TableRow key={r.id} hover>
              <TableCell>{r.periodYear}-{String(r.periodMonth).padStart(2, "0")}</TableCell>
              <TableCell>{r.projectCode ?? "All projects"}</TableCell>
              <TableCell>{r.payGroup}</TableCell>
              <TableCell align="right">{r.employeeCount}</TableCell>
              <TableCell><Chip size="small" label={r.status} color={r.status === "LOCKED" || r.status === "APPROVED" ? "success" : "default"} /></TableCell>
              <TableCell align="right">
                <Button
                  size="small" color="error"
                  disabled={(r.status !== "DRAFT" && r.status !== "CALCULATED") || del.isPending}
                  onClick={() => { if (confirm("Delete this payroll run and all its results? This cannot be undone.")) del.mutate(r.id); }}
                >
                  Delete
                </Button>
              </TableCell>
            </TableRow>
          ))}
          {runs.length === 0 && (
            <TableRow><TableCell colSpan={6}><Typography variant="body2" color="text.secondary" p={1}>No runs found.</Typography></TableCell></TableRow>
          )}
        </TableBody>
      </Table>
    </Paper>
  );
}

function EmployeePicker({ employee, setEmployee }: { employee: Employee | null; setEmployee: (e: Employee | null) => void }) {
  const [search, setSearch] = useState("");
  const { data: matchesPage } = useQuery({
    queryKey: ["auditEmployeeSearch2", search],
    queryFn: () => employeeApi.list(0, 10, search),
    enabled: search.length >= 2 && !employee,
  });
  const matches = matchesPage?.content ?? [];

  return (
    <>
      {!employee ? (
        <TextField
          fullWidth
          size="small"
          label="Search employee (name or number)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      ) : (
        <Stack direction="row" spacing={2} alignItems="center" mb={1}>
          <Typography variant="body2" fontWeight={700}>{employee.employeeNumber} — {employee.firstName} {employee.lastName}</Typography>
          <Button size="small" onClick={() => { setEmployee(null); setSearch(""); }}>Change employee</Button>
        </Stack>
      )}
      {!employee && search.length >= 2 && (
        <Paper variant="outlined" sx={{ mt: 1, maxHeight: 200, overflow: "auto" }}>
          {matches.map((e) => (
            <Box key={e.id} sx={{ p: 1, cursor: "pointer", "&:hover": { bgcolor: "action.hover" } }} onClick={() => { setEmployee(e); setSearch(""); }}>
              {e.employeeNumber} — {e.firstName} {e.lastName}
            </Box>
          ))}
          {matches.length === 0 && <Box sx={{ p: 1 }}><Typography variant="body2" color="text.secondary">No matches.</Typography></Box>}
        </Paper>
      )}
    </>
  );
}

function TimeUsagePanel() {
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [year, setYear] = useState(new Date().getFullYear());

  const { data, isFetching, refetch } = useQuery({
    queryKey: ["auditTimeUsage", employee?.id, year],
    queryFn: () => auditApi.timeUsage(employee!.id!, year),
    enabled: !!employee?.id,
  });
  const rows = data?.rows ?? [];

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Typography variant="subtitle1" fontWeight={700} mb={1}>Recalculate Time Usage</Typography>
      <Typography variant="body2" color="text.secondary" mb={1}>
        Time Usage is always computed live from the current timesheet (including Day Zero corrections) — there is nothing
        cached to go stale. This just lets you look it up for any employee without leaving Audit Tools.
      </Typography>
      <EmployeePicker employee={employee} setEmployee={setEmployee} />
      {employee && (
        <>
          <Stack direction="row" spacing={2} alignItems="center" my={1}>
            <TextField size="small" type="number" label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))} sx={{ width: 120 }} />
            <Button size="small" variant="contained" onClick={() => refetch()} disabled={isFetching}>Recalculate</Button>
          </Stack>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Time type</TableCell>
                <TableCell align="right">Used days</TableCell>
                <TableCell align="right">Used hours</TableCell>
                <TableCell align="right">Occurrences</TableCell>
                <TableCell>First</TableCell>
                <TableCell>Last</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.timeTypeCode} hover>
                  <TableCell>{row.timeTypeCode} — {row.timeTypeName}</TableCell>
                  <TableCell align="right">{row.usedDays}</TableCell>
                  <TableCell align="right">{row.usedHours}</TableCell>
                  <TableCell align="right">{row.occurrences}</TableCell>
                  <TableCell>{row.firstDate}</TableCell>
                  <TableCell>{row.lastDate}</TableCell>
                </TableRow>
              ))}
              {rows.length === 0 && (
                <TableRow><TableCell colSpan={6}><Typography variant="body2" color="text.secondary" p={1}>No usage this year.</Typography></TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </>
      )}
    </Paper>
  );
}

function LeaveDiscrepancyPanel() {
  const qc = useQueryClient();
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const { data: discrepancies = [], isFetching } = useQuery({
    queryKey: ["auditLeaveDiscrepancies", employee?.id],
    queryFn: () => auditApi.leaveDiscrepancies(employee!.id!),
    enabled: !!employee?.id,
  });

  const fix = useMutation({
    mutationFn: ({ id, newTotalDays }: { id: string; newTotalDays: number }) => auditApi.recalculateLeaveRequest(id, newTotalDays),
    onSuccess: () => {
      setMessage("Leave request updated to match the timesheet.");
      qc.invalidateQueries({ queryKey: ["auditLeaveDiscrepancies", employee?.id] });
    },
  });

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Typography variant="subtitle1" fontWeight={700} mb={1}>Recalculate Leave</Typography>
      <Typography variant="body2" color="text.secondary" mb={1}>
        Unlike Time Usage, a leave request's day count is a fixed number set when it was approved — it can drift from the
        timesheet if days were edited afterward, or corrected via Day Zero. This compares the two and lets you confirm a fix;
        nothing changes automatically.
      </Typography>
      <EmployeePicker employee={employee} setEmployee={setEmployee} />
      {employee && (
        <>
          {message && <Alert severity="info" sx={{ my: 1 }} onClose={() => setMessage(null)}>{message}</Alert>}
          {isFetching && <Typography variant="body2" color="text.secondary">Checking...</Typography>}
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Leave type</TableCell>
                <TableCell>Dates</TableCell>
                <TableCell align="right">Recorded days</TableCell>
                <TableCell align="right">Actual days (now)</TableCell>
                <TableCell align="right">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {discrepancies.map((d) => (
                <TableRow key={d.leaveRequestId} hover>
                  <TableCell>{d.leaveTypeCode}</TableCell>
                  <TableCell>{d.startDate} → {d.endDate}</TableCell>
                  <TableCell align="right">{d.recordedDays}</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>{d.actualDays}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small" variant="contained" disabled={fix.isPending}
                      onClick={() => {
                        if (confirm(`Update recorded days from ${d.recordedDays} to ${d.actualDays}?`)) {
                          fix.mutate({ id: d.leaveRequestId, newTotalDays: d.actualDays });
                        }
                      }}
                    >
                      Fix
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {discrepancies.length === 0 && !isFetching && (
                <TableRow><TableCell colSpan={5}><Typography variant="body2" color="text.secondary" p={1}>No discrepancies — all approved leave matches the timesheet.</Typography></TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </>
      )}
    </Paper>
  );
}
