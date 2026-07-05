import { useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
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
import { employeeApi, leaveApi, timeTypeApi } from "../api/resources";
import type { LeaveRequest, LeaveType } from "../api/types";

const today = new Date().toISOString().slice(0, 10);
const emptyRequest: LeaveRequest = {
  employeeId: "",
  leaveTypeId: "",
  startDate: today,
  endDate: today,
  returnDate: "",
  status: "DRAFT",
  requiresTicket: false,
};

export default function LeavePage() {
  const qc = useQueryClient();
  const { data: leaveTypes = [] } = useQuery({ queryKey: ["leaveTypes"], queryFn: leaveApi.types });
  const { data: employees } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500) });
  const { data: requests = [] } = useQuery({ queryKey: ["leaveRequests"], queryFn: () => leaveApi.requests() });
  const [request, setRequest] = useState<LeaveRequest>(emptyRequest);

  const saveRequest = useMutation({
    mutationFn: (payload: LeaveRequest) => leaveApi.saveRequest(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leaveRequests"] });
      setRequest(emptyRequest);
    },
  });
  const status = useMutation({
    mutationFn: ({ id, next }: { id: string; next: string }) => leaveApi.setRequestStatus(id, next),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["leaveRequests"] }),
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Leave</Typography>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{request.id ? "Edit leave request" : "New leave request"}</Typography>
        <Grid container spacing={1.5}>
          <Grid item xs={12} md={4}>
            <TextField select fullWidth size="small" label="Employee" value={request.employeeId}
              onChange={(e) => setRequest({ ...request, employeeId: e.target.value })}>
              {(employees?.content ?? []).map((emp) => (
                <MenuItem key={emp.id} value={emp.id}>{emp.employeeNumber} - {emp.firstName} {emp.lastName}</MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField select fullWidth size="small" label="Leave type" value={request.leaveTypeId}
              onChange={(e) => {
                const type = leaveTypes.find((t) => t.id === e.target.value);
                setRequest({ ...request, leaveTypeId: e.target.value, requiresTicket: !!type?.requiresTicketDefault });
              }}>
              {leaveTypes.map((type) => <MenuItem key={type.id} value={type.id}>{type.code} - {type.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="date" label="From" InputLabelProps={{ shrink: true }}
              value={request.startDate} onChange={(e) => setRequest({ ...request, startDate: e.target.value })} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="date" label="To" InputLabelProps={{ shrink: true }}
              value={request.endDate} onChange={(e) => setRequest({ ...request, endDate: e.target.value })} />
          </Grid>
          <Grid item xs={6} md={1}>
            <TextField fullWidth size="small" type="number" label="Days" value={request.totalDays ?? ""}
              onChange={(e) => setRequest({ ...request, totalDays: Number(e.target.value) })} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="date" label="Return" InputLabelProps={{ shrink: true }}
              value={request.returnDate ?? ""} onChange={(e) => setRequest({ ...request, returnDate: e.target.value })} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField select fullWidth size="small" label="Status" value={request.status ?? "DRAFT"}
              onChange={(e) => setRequest({ ...request, status: e.target.value })}>
              {["DRAFT", "SUBMITTED", "APPROVED", "REJECTED"].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={8}>
            <TextField fullWidth size="small" label="Reason" value={request.reason ?? ""}
              onChange={(e) => setRequest({ ...request, reason: e.target.value })} />
          </Grid>

          <Grid item xs={12}>
            <FormControlLabel control={<Checkbox checked={request.requiresTicket}
              onChange={(e) => setRequest({ ...request, requiresTicket: e.target.checked })} />} label="Requires ticket" />
          </Grid>
          {request.requiresTicket && (
            <>
              <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Ticket from" value={request.ticketFrom ?? ""} onChange={(e) => setRequest({ ...request, ticketFrom: e.target.value })} /></Grid>
              <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Ticket to" value={request.ticketTo ?? ""} onChange={(e) => setRequest({ ...request, ticketTo: e.target.value })} /></Grid>
              <Grid item xs={6} md={2}><TextField fullWidth size="small" type="date" label="Travel date" InputLabelProps={{ shrink: true }} value={request.travelDate ?? ""} onChange={(e) => setRequest({ ...request, travelDate: e.target.value })} /></Grid>
              <Grid item xs={6} md={2}><TextField fullWidth size="small" type="date" label="Return travel" InputLabelProps={{ shrink: true }} value={request.returnTravelDate ?? ""} onChange={(e) => setRequest({ ...request, returnTravelDate: e.target.value })} /></Grid>
              <Grid item xs={12} md={2}><TextField fullWidth size="small" label="Destination" value={request.destination ?? ""} onChange={(e) => setRequest({ ...request, destination: e.target.value })} /></Grid>
            </>
          )}
          <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Travel phone" value={request.contactPhone ?? ""} onChange={(e) => setRequest({ ...request, contactPhone: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Travel email" value={request.contactEmail ?? ""} onChange={(e) => setRequest({ ...request, contactEmail: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Emergency contact" value={request.emergencyContactName ?? ""} onChange={(e) => setRequest({ ...request, emergencyContactName: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Emergency phone" value={request.emergencyContactPhone ?? ""} onChange={(e) => setRequest({ ...request, emergencyContactPhone: e.target.value })} /></Grid>
          <Grid item xs={12}><TextField fullWidth size="small" label="Address during leave" value={request.addressDuringLeave ?? ""} onChange={(e) => setRequest({ ...request, addressDuringLeave: e.target.value })} /></Grid>
          <Grid item xs={12}>
            <Stack direction="row" spacing={1}>
              <Button variant="contained" disabled={!request.employeeId || !request.leaveTypeId || saveRequest.isPending} onClick={() => saveRequest.mutate(request)}>
                {request.id ? "Update" : "Create"}
              </Button>
              {request.id && <Button onClick={() => setRequest(emptyRequest)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      <LeaveTypesPanel rows={leaveTypes} />

      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Employee</TableCell>
              <TableCell>Leave</TableCell>
              <TableCell>Dates</TableCell>
              <TableCell align="right">Days</TableCell>
              <TableCell>Ticket</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {requests.map((row) => (
              <TableRow key={row.id} hover>
                <TableCell>{row.employeeNumber} - {row.employeeName}</TableCell>
                <TableCell>{row.leaveTypeCode}</TableCell>
                <TableCell>{row.startDate} - {row.endDate}</TableCell>
                <TableCell align="right">{Number(row.totalDays ?? 0).toFixed(2)}</TableCell>
                <TableCell>{row.requiresTicket ? `${row.ticketFrom ?? ""} -> ${row.ticketTo ?? ""}` : ""}</TableCell>
                <TableCell>{row.status}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setRequest(row)}>Edit</Button>
                  {row.id && row.status !== "APPROVED" && <Button size="small" onClick={() => status.mutate({ id: row.id!, next: "APPROVED" })}>Approve</Button>}
                  {row.id && row.status !== "REJECTED" && <Button size="small" color="error" onClick={() => status.mutate({ id: row.id!, next: "REJECTED" })}>Reject</Button>}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
}

function LeaveTypesPanel({ rows }: { rows: LeaveType[] }) {
  const qc = useQueryClient();
  const { data: timeTypes = [] } = useQuery({ queryKey: ["timeTypes"], queryFn: timeTypeApi.list });
  const [form, setForm] = useState<LeaveType>({ code: "", name: "", timeTypeId: "", deductsBalance: true, paid: true, requiresTicketDefault: false, status: "ACTIVE" });
  const save = useMutation({
    mutationFn: (payload: LeaveType) => leaveApi.saveType(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leaveTypes"] });
      setForm({ code: "", name: "", timeTypeId: "", deductsBalance: true, paid: true, requiresTicketDefault: false, status: "ACTIVE" });
    },
  });
  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
      <Typography variant="subtitle2" gutterBottom>Leave Types</Typography>
      <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} mb={1.5}>
        <TextField size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
        <TextField size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <TextField select size="small" label="Time type" value={form.timeTypeId} onChange={(e) => setForm({ ...form, timeTypeId: e.target.value })} sx={{ minWidth: 220 }}>
          {timeTypes.map((t) => <MenuItem key={t.id} value={t.id}>{t.code} - {t.name}</MenuItem>)}
        </TextField>
        <FormControlLabel control={<Checkbox checked={form.deductsBalance} onChange={(e) => setForm({ ...form, deductsBalance: e.target.checked })} />} label="Deducts balance" />
        <FormControlLabel control={<Checkbox checked={form.requiresTicketDefault} onChange={(e) => setForm({ ...form, requiresTicketDefault: e.target.checked })} />} label="Ticket" />
        <Button variant="contained" disabled={!form.code || !form.name || !form.timeTypeId || save.isPending} onClick={() => save.mutate(form)}>
          {form.id ? "Update" : "Add"}
        </Button>
      </Stack>
      {rows.map((type) => (
        <Typography key={type.id} variant="body2" sx={{ py: 0.25 }}>
          <b>{type.code}</b> - {type.name} · {type.timeTypeCode} · {type.deductsBalance ? "balance" : "no balance"}{type.requiresTicketDefault ? " · ticket" : ""}
          <Button size="small" onClick={() => setForm(type)}>Edit</Button>
        </Typography>
      ))}
    </Paper>
  );
}
