import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  MenuItem,
  Paper,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { selfApi } from "../api/resources";
import type { LeaveRequest } from "../api/types";

const money = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function MyPortalPage() {
  const [tab, setTab] = useState(0);
  return (
    <Box>
      <Typography variant="h5" mb={1}>My Portal</Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Your own payslips, timesheet, and leave — only your own data.
      </Typography>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="My Payslips" />
        <Tab label="My Timesheet" />
        <Tab label="My Leave" />
      </Tabs>
      {tab === 0 && <MyPayslipsTab />}
      {tab === 1 && <MyTimesheetTab />}
      {tab === 2 && <MyLeaveTab />}
    </Box>
  );
}

function MyPayslipsTab() {
  const [selected, setSelected] = useState<string | null>(null);
  const { data: payslips = [] } = useQuery({ queryKey: ["myPayslips"], queryFn: () => selfApi.payslips() });
  const { data: detail } = useQuery({
    queryKey: ["myPayslipDetail", selected],
    queryFn: () => selfApi.payslipDetail(selected!),
    enabled: !!selected,
  });

  if (selected && detail) {
    return (
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Button size="small" onClick={() => setSelected(null)} sx={{ mb: 2 }}>&larr; Back to list</Button>
        <Typography variant="h6" mb={1}>Payslip — {detail.periodYear}-{String(detail.periodMonth).padStart(2, "0")}</Typography>
        <Stack direction="row" spacing={4} mb={2}>
          <Box><Typography variant="caption" color="text.secondary">Gross</Typography><Typography variant="h6">{money(detail.gross)}</Typography></Box>
          <Box><Typography variant="caption" color="text.secondary">Deductions</Typography><Typography variant="h6" color="error.main">-{money(detail.totalDeductions)}</Typography></Box>
          <Box><Typography variant="caption" color="text.secondary">Net pay</Typography><Typography variant="h6" color="success.main">{money(detail.net)}</Typography></Box>
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Component</TableCell>
              <TableCell align="right">Qty</TableCell>
              <TableCell align="right">Rate</TableCell>
              <TableCell align="right">Amount</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(detail.lines ?? []).map((line) => (
              <TableRow key={line.id} hover>
                <TableCell>{line.componentName}</TableCell>
                <TableCell align="right">{line.quantity}</TableCell>
                <TableCell align="right">{money(line.rate)}</TableCell>
                <TableCell align="right" sx={{ color: line.componentType === "DEDUCTION" ? "error.main" : "success.main" }}>
                  {line.componentType === "DEDUCTION" ? "-" : ""}{money(line.amount)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
    );
  }

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Period</TableCell>
            <TableCell align="right">Gross</TableCell>
            <TableCell align="right">Deductions</TableCell>
            <TableCell align="right">Net pay</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right"></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {payslips.map((p) => (
            <TableRow key={p.id} hover>
              <TableCell>{p.periodYear}-{String(p.periodMonth).padStart(2, "0")}</TableCell>
              <TableCell align="right">{money(p.gross)}</TableCell>
              <TableCell align="right" sx={{ color: "error.main" }}>-{money(p.totalDeductions)}</TableCell>
              <TableCell align="right" sx={{ fontWeight: 700 }}>{money(p.net)}</TableCell>
              <TableCell><Chip size="small" label={p.status} /></TableCell>
              <TableCell align="right">
                <Button size="small" onClick={() => setSelected(p.id!)}>View</Button>
              </TableCell>
            </TableRow>
          ))}
          {payslips.length === 0 && (
            <TableRow><TableCell colSpan={6}><Typography variant="body2" color="text.secondary" p={1}>No payslips yet.</Typography></TableCell></TableRow>
          )}
        </TableBody>
      </Table>
    </Paper>
  );
}

function MyTimesheetTab() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const { data: ts, isError } = useQuery({
    queryKey: ["myTimesheet", year, month],
    queryFn: () => selfApi.timesheet(year, month),
    retry: false,
  });

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Stack direction="row" spacing={2} mb={2}>
        <TextField size="small" type="number" label="Year" value={year} onChange={(e) => setYear(Number(e.target.value))} sx={{ width: 120 }} />
        <TextField size="small" type="number" label="Month" value={month} onChange={(e) => setMonth(Number(e.target.value))} sx={{ width: 100 }} inputProps={{ min: 1, max: 12 }} />
      </Stack>
      {isError && <Alert severity="info">No timesheet found for this period yet.</Alert>}
      {ts && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell>Type</TableCell>
              <TableCell align="right">Planned</TableCell>
              <TableCell align="right">Worked</TableCell>
              <TableCell align="right">Normal</TableCell>
              <TableCell align="right">OT</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(ts.days ?? []).map((d) => (
              <TableRow key={d.id} hover>
                <TableCell>{d.workDate}</TableCell>
                <TableCell>{d.timeTypeCode}</TableCell>
                <TableCell align="right">{d.plannedHours ?? 0}</TableCell>
                <TableCell align="right">{d.workedHours ?? 0}</TableCell>
                <TableCell align="right">{d.normalHours ?? 0}</TableCell>
                <TableCell align="right">{d.otHours ?? 0}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Paper>
  );
}

function MyLeaveTab() {
  const qc = useQueryClient();
  const emptyForm = {
    id: "",
    leaveTypeId: "",
    startDate: "",
    endDate: "",
    returnDate: "",
    reason: "",
    contactPhone: "",
    contactEmail: "",
    addressDuringLeave: "",
    emergencyContactName: "",
    emergencyContactPhone: "",
  };
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState<string | null>(null);

  const { data: types = [] } = useQuery({ queryKey: ["myLeaveTypes"], queryFn: () => selfApi.leaveTypes() });
  const { data: requests = [] } = useQuery({ queryKey: ["myLeaveRequests"], queryFn: () => selfApi.leaveRequests() });
  const { data: balances = [] } = useQuery({ queryKey: ["myLeaveBalance"], queryFn: () => selfApi.leaveBalance() });

  const submit = useMutation({
    mutationFn: () => {
      const payload = {
        leaveTypeId: form.leaveTypeId,
        startDate: form.startDate,
        endDate: form.endDate,
        returnDate: form.returnDate || undefined,
        reason: form.reason,
        contactPhone: form.contactPhone,
        contactEmail: form.contactEmail,
        addressDuringLeave: form.addressDuringLeave,
        emergencyContactName: form.emergencyContactName,
        emergencyContactPhone: form.emergencyContactPhone,
      };
      return form.id ? selfApi.updateLeaveRequest(form.id, payload) : selfApi.submitLeaveRequest(payload);
    },
    onSuccess: () => {
      setMessage("Request submitted - awaiting manager approval.");
      setForm(emptyForm);
      qc.invalidateQueries({ queryKey: ["myLeaveRequests"] });
    },
    onError: (e: any) => setMessage(e?.response?.data?.message ?? "Could not submit the request."),
  });

  const editReturned = (request: LeaveRequest) => {
    setForm({
      id: request.id ?? "",
      leaveTypeId: request.leaveTypeId,
      startDate: request.startDate,
      endDate: request.endDate,
      returnDate: request.returnDate ?? "",
      reason: request.reason ?? "",
      contactPhone: request.contactPhone ?? "",
      contactEmail: request.contactEmail ?? "",
      addressDuringLeave: request.addressDuringLeave ?? "",
      emergencyContactName: request.emergencyContactName ?? "",
      emergencyContactPhone: request.emergencyContactPhone ?? "",
    });
    setMessage("Update the returned request and submit it again.");
  };

  return (
    <Stack spacing={2}>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Typography variant="subtitle1" fontWeight={700} mb={1}>My leave balance</Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Leave type</TableCell>
              <TableCell align="right">Entitled</TableCell>
              <TableCell align="right">Used</TableCell>
              <TableCell align="right">Remaining</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {balances.map((b) => (
              <TableRow key={b.leaveTypeCode} hover>
                <TableCell>{b.leaveTypeName}</TableCell>
                <TableCell align="right">{b.entitledToDate}</TableCell>
                <TableCell align="right">{b.usedApproved}</TableCell>
                <TableCell align="right" sx={{ fontWeight: 700 }}>{b.balance}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Typography variant="subtitle1" fontWeight={700} mb={1}>
          {form.id ? "Edit returned leave request" : "Submit a leave request"}
        </Typography>
        {message && <Alert severity="info" sx={{ mb: 1 }} onClose={() => setMessage(null)}>{message}</Alert>}
        <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
          <TextField select size="small" label="Leave type" value={form.leaveTypeId}
            onChange={(e) => setForm((f) => ({ ...f, leaveTypeId: e.target.value }))} sx={{ minWidth: 200 }}>
            {types.map((t) => <MenuItem key={t.id} value={t.id}>{t.name} ({t.code})</MenuItem>)}
          </TextField>
          <TextField size="small" type="date" label="Start date" InputLabelProps={{ shrink: true }}
            value={form.startDate} onChange={(e) => setForm((f) => ({ ...f, startDate: e.target.value }))} />
          <TextField size="small" type="date" label="End date" InputLabelProps={{ shrink: true }}
            value={form.endDate} onChange={(e) => setForm((f) => ({ ...f, endDate: e.target.value }))} />
          <TextField size="small" type="date" label="Return date" InputLabelProps={{ shrink: true }}
            value={form.returnDate} onChange={(e) => setForm((f) => ({ ...f, returnDate: e.target.value }))} />
          <TextField size="small" label="Reason" value={form.reason}
            onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} sx={{ minWidth: 240 }} />
          <TextField size="small" label="Address during leave" value={form.addressDuringLeave}
            onChange={(e) => setForm((f) => ({ ...f, addressDuringLeave: e.target.value }))} sx={{ minWidth: 280 }} />
          <TextField size="small" label="Contact phone" value={form.contactPhone}
            onChange={(e) => setForm((f) => ({ ...f, contactPhone: e.target.value }))} sx={{ minWidth: 180 }} />
          <TextField size="small" label="Contact email" value={form.contactEmail}
            onChange={(e) => setForm((f) => ({ ...f, contactEmail: e.target.value }))} sx={{ minWidth: 220 }} />
          <TextField size="small" label="Emergency contact" value={form.emergencyContactName}
            onChange={(e) => setForm((f) => ({ ...f, emergencyContactName: e.target.value }))} sx={{ minWidth: 220 }} />
          <TextField size="small" label="Emergency phone" value={form.emergencyContactPhone}
            onChange={(e) => setForm((f) => ({ ...f, emergencyContactPhone: e.target.value }))} sx={{ minWidth: 180 }} />
          <Button variant="contained" disabled={!form.leaveTypeId || !form.startDate || !form.endDate || submit.isPending}
            onClick={() => submit.mutate()}>
            {form.id ? "Resubmit" : "Submit"}
          </Button>
          {form.id && <Button onClick={() => { setForm(emptyForm); setMessage(null); }}>Cancel edit</Button>}
        </Stack>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Typography variant="subtitle1" fontWeight={700} mb={1}>My requests</Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Type</TableCell>
              <TableCell>Dates</TableCell>
              <TableCell align="right">Days</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {requests.map((r) => (
              <TableRow key={r.id} hover>
                <TableCell>{r.leaveTypeCode}</TableCell>
                <TableCell>{r.startDate} - {r.endDate}</TableCell>
                <TableCell align="right">{r.totalDays}</TableCell>
                <TableCell><Chip size="small" label={r.status} color={r.status === "APPROVED" ? "success" : r.status === "REJECTED" ? "error" : "default"} /></TableCell>
                <TableCell align="right">
                  {r.status === "RETURNED" && <Button size="small" onClick={() => editReturned(r)}>Edit & resubmit</Button>}
                </TableCell>
              </TableRow>
            ))}
            {requests.length === 0 && (
              <TableRow><TableCell colSpan={5}><Typography variant="body2" color="text.secondary" p={1}>No requests yet.</Typography></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Stack>
  );
}
