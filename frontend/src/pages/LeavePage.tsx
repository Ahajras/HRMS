import { useState } from "react";
import {
  Alert,
  Autocomplete,
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
import { employeeApi, leaveApi, projectApi, timeTypeApi } from "../api/resources";
import type { Employee, LeaveRequest, LeaveType } from "../api/types";

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

function inclusiveDays(from?: string, to?: string) {
  if (!from || !to) return 0;
  const start = new Date(`${from}T00:00:00`);
  const end = new Date(`${to}T00:00:00`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end < start) return 0;
  return Math.floor((end.getTime() - start.getTime()) / 86400000) + 1;
}

export default function LeavePage() {
  const qc = useQueryClient();
  const [projectId, setProjectId] = useState("");
  const [employeeSearch, setEmployeeSearch] = useState("");
  const [requestSearch, setRequestSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [leaveTypeFilter, setLeaveTypeFilter] = useState("");
  const [page, setPage] = useState(0);
  const pageSize = 50;
  const [request, setRequest] = useState<LeaveRequest>(emptyRequest);
  const calculatedDays = inclusiveDays(request.startDate, request.endDate);
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const selectedProject = projects.find((p) => p.id === projectId);
  const { data: leaveTypes = [] } = useQuery({ queryKey: ["leaveTypes"], queryFn: leaveApi.types });
  const { data: employeePage } = useQuery({
    queryKey: ["leaveEmployeeSearch", projectId, employeeSearch],
    queryFn: () => employeeApi.list(0, 20, employeeSearch || undefined, undefined, projectId || undefined, { activeOnly: true }),
    enabled: !!projectId && employeeSearch.trim().length >= 2,
  });
  const employeeOptions = employeePage?.content ?? [];
  const selectedEmployee: Employee | null = request.employeeId
    ? employeeOptions.find((e) => e.id === request.employeeId)
      ?? (request.employeeNumber ? {
        id: request.employeeId,
        employeeNumber: request.employeeNumber,
        firstName: request.employeeName ?? "",
        lastName: "",
        hireDate: "",
      } : null)
    : null;
  const { data: requests } = useQuery({
    queryKey: ["leaveRequests", projectId, statusFilter, leaveTypeFilter, requestSearch, page],
    queryFn: () => leaveApi.requests({
      projectId,
      status: statusFilter || undefined,
      leaveTypeId: leaveTypeFilter || undefined,
      q: requestSearch || undefined,
      page,
      size: pageSize,
    }),
    enabled: !!projectId,
  });
  const requestRows = requests?.content ?? [];
  const { data: summaryRows = [] } = useQuery({
    queryKey: ["leaveProjectSummary", projectId, statusFilter, leaveTypeFilter],
    queryFn: () => leaveApi.projectSummary({
      projectId,
      status: statusFilter || undefined,
      leaveTypeId: leaveTypeFilter || undefined,
    }),
    enabled: !!projectId,
  });
  const summaryTotals = summaryRows.reduce((acc, row) => ({
    total: acc.total + row.total,
    pending: acc.pending + row.pending,
    approved: acc.approved + row.approved,
    rejected: acc.rejected + row.rejected,
    approvedDays: acc.approvedDays + Number(row.approvedDays ?? 0),
  }), { total: 0, pending: 0, approved: 0, rejected: 0, approvedDays: 0 });

  const saveRequest = useMutation({
    mutationFn: (payload: LeaveRequest) => leaveApi.saveRequest({ ...payload, totalDays: calculatedDays }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leaveRequests"] });
      qc.invalidateQueries({ queryKey: ["leaveProjectSummary"] });
      setRequest(emptyRequest);
    },
  });
  const status = useMutation({
    mutationFn: ({ id, next }: { id: string; next: string }) => leaveApi.setRequestStatus(id, next),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leaveRequests"] });
      qc.invalidateQueries({ queryKey: ["leaveProjectSummary"] });
    },
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Leave</Typography>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{request.id ? "Edit leave request" : "New leave request"}</Typography>
        {!projectId && (
          <Alert severity="info" sx={{ mb: 1 }}>
            Pick a project first, then search employees inside that project.
          </Alert>
        )}
        <Grid container spacing={1.5}>
          <Grid item xs={12} md={3}>
            <TextField select fullWidth size="small" label="Project" value={projectId}
              onChange={(e) => {
                setProjectId(e.target.value);
                setEmployeeSearch("");
                setRequest({ ...request, employeeId: "" });
                setPage(0);
              }}>
              <MenuItem value="" disabled>Select project</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={4}>
            <Autocomplete
              size="small"
              disabled={!projectId}
              options={employeeOptions}
              filterOptions={(x) => x}
              value={selectedEmployee}
              inputValue={employeeSearch}
              onInputChange={(_, value) => setEmployeeSearch(value)}
              onChange={(_, emp) => setRequest({
                ...request,
                employeeId: emp?.id ?? "",
                employeeNumber: emp?.employeeNumber,
                employeeName: emp ? `${emp.firstName} ${emp.lastName}`.trim() : undefined,
                ticketFrom: request.ticketFrom || emp?.workAirportCode,
                ticketTo: request.ticketTo || emp?.homeAirportCode,
              })}
              getOptionLabel={(emp) => `${emp.employeeNumber} - ${emp.firstName} ${emp.lastName}`.trim()}
              isOptionEqualToValue={(a, b) => a.id === b.id}
              renderInput={(params) => <TextField {...params} label="Employee search" placeholder="Type employee no/name" />}
            />
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
            <TextField fullWidth size="small" label="Days" value={calculatedDays || ""}
              InputProps={{ readOnly: true }} error={!!request.startDate && !!request.endDate && calculatedDays === 0} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField fullWidth size="small" type="date" label="Return" InputLabelProps={{ shrink: true }}
              value={request.returnDate ?? ""} onChange={(e) => setRequest({ ...request, returnDate: e.target.value })} />
          </Grid>
          <Grid item xs={6} md={2}>
            <TextField select fullWidth size="small" label="Status" value={request.status ?? "DRAFT"}
              onChange={(e) => setRequest({ ...request, status: e.target.value })}>
              {["DRAFT", "SUBMITTED"].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={8}>
            <TextField fullWidth size="small" label="Reason" value={request.reason ?? ""}
              onChange={(e) => setRequest({ ...request, reason: e.target.value })} />
          </Grid>

          <Grid item xs={12}>
            <FormControlLabel control={<Checkbox checked={request.requiresTicket}
              onChange={(e) => setRequest(e.target.checked
                ? { ...request, requiresTicket: true }
                : {
                  ...request,
                  requiresTicket: false,
                  ticketFrom: undefined,
                  ticketTo: undefined,
                  travelDate: undefined,
                  returnTravelDate: undefined,
                  destination: undefined,
                  passportNumber: undefined,
                  dependentCount: undefined,
                  travelRemarks: undefined,
                })} />} label="Requires ticket" />
          </Grid>
          {request.requiresTicket && (
            <>
              <Grid item xs={12} md={2}><TextField fullWidth size="small" label="Work airport" value={request.ticketFrom ?? selectedEmployee?.workAirportCode ?? ""} onChange={(e) => setRequest({ ...request, ticketFrom: e.target.value.toUpperCase() })} /></Grid>
              <Grid item xs={12} md={2}><TextField fullWidth size="small" label="Home airport" value={request.ticketTo ?? selectedEmployee?.homeAirportCode ?? ""} onChange={(e) => setRequest({ ...request, ticketTo: e.target.value.toUpperCase() })} /></Grid>
              <Grid item xs={6} md={2}><TextField fullWidth size="small" type="date" label="Departure" InputLabelProps={{ shrink: true }} value={request.travelDate ?? ""} onChange={(e) => setRequest({ ...request, travelDate: e.target.value })} /></Grid>
              <Grid item xs={6} md={2}><TextField fullWidth size="small" type="date" label="Return flight" InputLabelProps={{ shrink: true }} value={request.returnTravelDate ?? ""} onChange={(e) => setRequest({ ...request, returnTravelDate: e.target.value })} /></Grid>
              <Grid item xs={12} md={2}><TextField fullWidth size="small" label="Passport no." value={request.passportNumber ?? ""} onChange={(e) => setRequest({ ...request, passportNumber: e.target.value })} /></Grid>
              <Grid item xs={12} md={2}><TextField fullWidth size="small" type="number" label="Dependents" value={request.dependentCount ?? 0} onChange={(e) => setRequest({ ...request, dependentCount: Math.max(0, Number(e.target.value) || 0) })} /></Grid>
              <Grid item xs={12} md={4}><TextField fullWidth size="small" label="Route / destination" value={request.destination ?? ""} onChange={(e) => setRequest({ ...request, destination: e.target.value })} /></Grid>
              <Grid item xs={12} md={8}><TextField fullWidth size="small" label="Ticket remarks" value={request.travelRemarks ?? ""} onChange={(e) => setRequest({ ...request, travelRemarks: e.target.value })} /></Grid>
            </>
          )}
          <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Travel phone" value={request.contactPhone ?? ""} onChange={(e) => setRequest({ ...request, contactPhone: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Travel email" value={request.contactEmail ?? ""} onChange={(e) => setRequest({ ...request, contactEmail: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Emergency contact" value={request.emergencyContactName ?? ""} onChange={(e) => setRequest({ ...request, emergencyContactName: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Emergency phone" value={request.emergencyContactPhone ?? ""} onChange={(e) => setRequest({ ...request, emergencyContactPhone: e.target.value })} /></Grid>
          <Grid item xs={12}><TextField fullWidth size="small" label="Address during leave" value={request.addressDuringLeave ?? ""} onChange={(e) => setRequest({ ...request, addressDuringLeave: e.target.value })} /></Grid>
          <Grid item xs={12}>
            <Stack direction="row" spacing={1}>
              <Button variant="contained" disabled={!projectId || !request.employeeId || !request.leaveTypeId || calculatedDays <= 0 || saveRequest.isPending} onClick={() => saveRequest.mutate(request)}>
                {request.id ? "Update" : "Create"}
              </Button>
              {request.id && <Button onClick={() => setRequest(emptyRequest)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      <LeaveTypesPanel rows={leaveTypes} />

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        {!projectId && (
          <Alert severity="info" sx={{ mb: 1.5 }}>
            Select a project to view leave requests and project leave totals.
          </Alert>
        )}
        {projectId && (
          <Typography variant="subtitle2" gutterBottom>
            Leave summary for {selectedProject ? `${selectedProject.code} - ${selectedProject.name}` : "selected project"}
          </Typography>
        )}
        <Stack direction="row" spacing={1} flexWrap="wrap" mb={1.5}>
          {[
            { label: "Total", value: summaryTotals.total },
            { label: "Pending", value: summaryTotals.pending },
            { label: "Approved", value: summaryTotals.approved },
            { label: "Rejected", value: summaryTotals.rejected },
            { label: "Approved days", value: summaryTotals.approvedDays.toFixed(2) },
          ].map((item) => (
            <Box key={item.label} sx={{ minWidth: 120, border: "1px solid", borderColor: "divider", borderRadius: 1, px: 1.25, py: 1 }}>
              <Typography variant="caption" color="text.secondary">{item.label}</Typography>
              <Typography variant="h6">{item.value}</Typography>
            </Box>
          ))}
        </Stack>
        {projectId && summaryRows.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Pay group</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell align="right">Pending</TableCell>
                <TableCell align="right">Approved</TableCell>
                <TableCell align="right">Rejected</TableCell>
                <TableCell align="right">Approved days</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {summaryRows.map((row) => (
                <TableRow key={`${row.projectId}-${row.payGroup}`} hover>
                  <TableCell>{row.payGroup}</TableCell>
                  <TableCell align="right">{row.total}</TableCell>
                  <TableCell align="right">{row.pending}</TableCell>
                  <TableCell align="right">{row.approved}</TableCell>
                  <TableCell align="right">{row.rejected}</TableCell>
                  <TableCell align="right">{Number(row.approvedDays ?? 0).toFixed(2)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>

      <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5}>
          <Grid item xs={12} md={3}>
            <TextField size="small" fullWidth label="Search requests" value={requestSearch}
              onChange={(e) => { setRequestSearch(e.target.value); setPage(0); }} />
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField select size="small" fullWidth label="Status" value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}>
              <MenuItem value="">All statuses</MenuItem>
              {["DRAFT", "SUBMITTED", "APPROVED", "REJECTED"].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField select size="small" fullWidth label="Leave type" value={leaveTypeFilter}
              onChange={(e) => { setLeaveTypeFilter(e.target.value); setPage(0); }}>
              <MenuItem value="">All leave types</MenuItem>
              {leaveTypes.map((type) => <MenuItem key={type.id} value={type.id}>{type.code} - {type.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={3}>
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button size="small" variant="outlined" disabled={!requests || requests.first} onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
              <Button size="small" variant="outlined" disabled={!requests || requests.last} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </Stack>
            <Typography variant="caption" color="text.secondary" display="block" textAlign="right">
              {requests ? `${requests.totalElements} request(s) - page ${requests.totalPages === 0 ? 0 : requests.page + 1} of ${requests.totalPages}` : "Loading..."}
            </Typography>
          </Grid>
        </Grid>
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto" }}>
        {!projectId ? (
          <Typography variant="body2" color="text.secondary" p={2}>Select a project first.</Typography>
        ) : (
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
              {requestRows.map((row) => (
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
              {requestRows.length === 0 && (
                <TableRow><TableCell colSpan={7}><Typography variant="body2" color="text.secondary">No leave requests for this project.</Typography></TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        )}
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
