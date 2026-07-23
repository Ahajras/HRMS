import { useState } from "react";
import type { ReactNode } from "react";
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  FormControlLabel,
  Grid,
  InputAdornment,
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
import AssignmentTurnedInIcon from "@mui/icons-material/AssignmentTurnedIn";
import FlightTakeoffIcon from "@mui/icons-material/FlightTakeoff";
import GroupsIcon from "@mui/icons-material/Groups";
import SearchIcon from "@mui/icons-material/Search";
import SummarizeIcon from "@mui/icons-material/Summarize";
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

const requiredFieldSx = (missing: boolean) => missing ? {
  "& .MuiOutlinedInput-root": {
    backgroundColor: "#fff7ed",
    "& fieldset": { borderColor: "#fb923c" },
  },
  "& .MuiInputLabel-root": { color: "#c2410c" },
} : undefined;

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
      setEmployeeSearch("");
    },
  });

  const missingRequired = [
    !projectId ? "Project is required so employees and requests stay scoped to the correct project." : "",
    !request.employeeId ? "Employee is required before a leave request can be created." : "",
    !request.leaveTypeId ? "Leave type is required so payroll/timesheet rules know how to treat the absence." : "",
    !request.startDate ? "From date is required." : "",
    !request.endDate ? "To date is required." : "",
    request.startDate && request.endDate && calculatedDays <= 0 ? "To date must be the same as or after From date." : "",
    request.requiresTicket && !(request.ticketFrom ?? selectedEmployee?.workAirportCode) ? "Work airport is required when ticket is requested." : "",
    request.requiresTicket && !(request.ticketTo ?? selectedEmployee?.homeAirportCode) ? "Home airport is required when ticket is requested." : "",
  ].filter(Boolean);

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5">Leave</Typography>
          <Typography variant="body2" color="text.secondary">
            Create project-scoped leave requests, review balances impact, and track approvals by pay group.
          </Typography>
        </Box>
        <Chip icon={<AssignmentTurnedInIcon />} label="Pick project first, then employee, leave type, and dates." variant="outlined" />
      </Stack>

      <Grid container spacing={1.5} mb={2}>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<GroupsIcon color="primary" />} title="1. Select project" text="Employees and request history are filtered by the selected project." />
        </Grid>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<AssignmentTurnedInIcon color="primary" />} title="2. Enter leave details" text="Required fields are highlighted until the request is ready to save." />
        </Grid>
        <Grid item xs={12} md={4}>
          <GuideCard icon={<FlightTakeoffIcon color="primary" />} title="3. Ticket details" text="Ticket fields appear only when the request requires a ticket." />
        </Grid>
      </Grid>

      <Grid container spacing={2} alignItems="flex-start">
        <Grid item xs={12} lg={8}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
            <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1} mb={1.5}>
              <Box>
                <Typography variant="subtitle2">{request.id ? "Edit leave request" : "New leave request"}</Typography>
                <Typography variant="body2" color="text.secondary">
                  The selected project controls which employees can be searched and where this request appears.
                </Typography>
              </Box>
              <Button variant="outlined" startIcon={<AddIcon />} onClick={() => { setRequest(emptyRequest); setEmployeeSearch(""); }}>
                New request
              </Button>
            </Stack>

            {missingRequired.length > 0 && (
              <Alert severity="warning" sx={{ mb: 2 }}>
                Complete required fields: {missingRequired.join(" ")}
              </Alert>
            )}

            <Grid container spacing={1.5}>
              <Grid item xs={12} md={4}>
                <TextField
                  select
                  required
                  fullWidth
                  size="small"
                  label="Project"
                  value={projectId}
                  sx={requiredFieldSx(!projectId)}
                  onChange={(e) => {
                    setProjectId(e.target.value);
                    setEmployeeSearch("");
                    setRequest({ ...request, employeeId: "" });
                    setPage(0);
                  }}
                >
                  <MenuItem value="" disabled>Select project</MenuItem>
                  {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} md={5}>
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
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      required
                      label="Employee search"
                      placeholder={projectId ? "Type employee no/name" : "Pick project first"}
                      sx={requiredFieldSx(!request.employeeId)}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} md={3}>
                <TextField
                  select
                  required
                  fullWidth
                  size="small"
                  label="Leave type"
                  value={request.leaveTypeId}
                  sx={requiredFieldSx(!request.leaveTypeId)}
                  onChange={(e) => {
                    const type = leaveTypes.find((t) => t.id === e.target.value);
                    setRequest({ ...request, leaveTypeId: e.target.value, requiresTicket: !!type?.requiresTicketDefault });
                  }}
                >
                  {leaveTypes.map((type) => <MenuItem key={type.id} value={type.id}>{type.code} - {type.name}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={6} md={3}>
                <TextField fullWidth required size="small" type="date" label="From" InputLabelProps={{ shrink: true }}
                  value={request.startDate} sx={requiredFieldSx(!request.startDate || calculatedDays <= 0)}
                  onChange={(e) => setRequest({ ...request, startDate: e.target.value })} />
              </Grid>
              <Grid item xs={6} md={3}>
                <TextField fullWidth required size="small" type="date" label="To" InputLabelProps={{ shrink: true }}
                  value={request.endDate} sx={requiredFieldSx(!request.endDate || calculatedDays <= 0)}
                  onChange={(e) => setRequest({ ...request, endDate: e.target.value })} />
              </Grid>
              <Grid item xs={6} md={2}>
                <TextField fullWidth size="small" label="Days" value={calculatedDays || ""}
                  InputProps={{ readOnly: true }} error={!!request.startDate && !!request.endDate && calculatedDays === 0} />
              </Grid>
              <Grid item xs={6} md={2}>
                <TextField fullWidth size="small" type="date" label="Return" InputLabelProps={{ shrink: true }}
                  value={request.returnDate ?? ""} onChange={(e) => setRequest({ ...request, returnDate: e.target.value })} />
              </Grid>
              <Grid item xs={12} md={2}>
                <TextField select fullWidth size="small" label="Status" value={request.status ?? "DRAFT"}
                  onChange={(e) => setRequest({ ...request, status: e.target.value })}>
                  {["DRAFT", "SUBMITTED", "RETURNED"].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth size="small" label="Reason" value={request.reason ?? ""}
                  onChange={(e) => setRequest({ ...request, reason: e.target.value })} />
              </Grid>

              <Grid item xs={12}>
                <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, bgcolor: request.requiresTicket ? "#eff6ff" : "background.default" }}>
                  <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1}>
                    <Box>
                      <Typography variant="subtitle2">Ticket request</Typography>
                      <Typography variant="body2" color="text.secondary">Enable this only when the employee needs travel ticket processing.</Typography>
                    </Box>
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
                  </Stack>
                  {request.requiresTicket && (
                    <Grid container spacing={1.25} mt={0.25}>
                      <Grid item xs={12} md={3}><TextField required fullWidth size="small" label="Work airport" value={request.ticketFrom ?? selectedEmployee?.workAirportCode ?? ""} sx={requiredFieldSx(!(request.ticketFrom ?? selectedEmployee?.workAirportCode))} onChange={(e) => setRequest({ ...request, ticketFrom: e.target.value.toUpperCase() })} /></Grid>
                      <Grid item xs={12} md={3}><TextField required fullWidth size="small" label="Home airport" value={request.ticketTo ?? selectedEmployee?.homeAirportCode ?? ""} sx={requiredFieldSx(!(request.ticketTo ?? selectedEmployee?.homeAirportCode))} onChange={(e) => setRequest({ ...request, ticketTo: e.target.value.toUpperCase() })} /></Grid>
                      <Grid item xs={6} md={3}><TextField fullWidth size="small" type="date" label="Departure" InputLabelProps={{ shrink: true }} value={request.travelDate ?? ""} onChange={(e) => setRequest({ ...request, travelDate: e.target.value })} /></Grid>
                      <Grid item xs={6} md={3}><TextField fullWidth size="small" type="date" label="Return flight" InputLabelProps={{ shrink: true }} value={request.returnTravelDate ?? ""} onChange={(e) => setRequest({ ...request, returnTravelDate: e.target.value })} /></Grid>
                      <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Passport no." value={request.passportNumber ?? ""} onChange={(e) => setRequest({ ...request, passportNumber: e.target.value })} /></Grid>
                      <Grid item xs={12} md={2}><TextField fullWidth size="small" type="number" label="Dependents" value={request.dependentCount ?? 0} onChange={(e) => setRequest({ ...request, dependentCount: Math.max(0, Number(e.target.value) || 0) })} /></Grid>
                      <Grid item xs={12} md={7}><TextField fullWidth size="small" label="Route / destination" value={request.destination ?? ""} onChange={(e) => setRequest({ ...request, destination: e.target.value })} /></Grid>
                      <Grid item xs={12}><TextField fullWidth size="small" label="Ticket remarks" value={request.travelRemarks ?? ""} onChange={(e) => setRequest({ ...request, travelRemarks: e.target.value })} /></Grid>
                    </Grid>
                  )}
                </Paper>
              </Grid>

              <Grid item xs={12}>
                <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, bgcolor: "background.default" }}>
                  <Typography variant="subtitle2" mb={1}>Contact during leave</Typography>
                  <Grid container spacing={1.25}>
                    <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Travel phone" value={request.contactPhone ?? ""} onChange={(e) => setRequest({ ...request, contactPhone: e.target.value })} /></Grid>
                    <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Travel email" value={request.contactEmail ?? ""} onChange={(e) => setRequest({ ...request, contactEmail: e.target.value })} /></Grid>
                    <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Emergency contact" value={request.emergencyContactName ?? ""} onChange={(e) => setRequest({ ...request, emergencyContactName: e.target.value })} /></Grid>
                    <Grid item xs={12} md={3}><TextField fullWidth size="small" label="Emergency phone" value={request.emergencyContactPhone ?? ""} onChange={(e) => setRequest({ ...request, emergencyContactPhone: e.target.value })} /></Grid>
                    <Grid item xs={12}><TextField fullWidth size="small" label="Address during leave" value={request.addressDuringLeave ?? ""} onChange={(e) => setRequest({ ...request, addressDuringLeave: e.target.value })} /></Grid>
                  </Grid>
                </Paper>
              </Grid>

              <Grid item xs={12}>
                <Stack direction="row" spacing={1}>
                  <Button variant="contained" disabled={missingRequired.length > 0 || saveRequest.isPending} onClick={() => saveRequest.mutate(request)}>
                    {request.id ? "Update" : "Create"}
                  </Button>
                  {request.id && <Button onClick={() => setRequest(emptyRequest)}>Cancel</Button>}
                </Stack>
                {saveRequest.isError && <Alert severity="error" sx={{ mt: 1 }}>{(saveRequest.error as any)?.response?.data?.message ?? "Unable to save leave request."}</Alert>}
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        <Grid item xs={12} lg={4}>
          <Stack spacing={2}>
            <LeaveSummaryPanel
              projectId={projectId}
              projectLabel={selectedProject ? `${selectedProject.code} - ${selectedProject.name}` : ""}
              summaryRows={summaryRows}
              totals={summaryTotals}
            />
            <LeaveTypesPanel rows={leaveTypes} />
          </Stack>
        </Grid>
      </Grid>

      <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, my: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} md={3}>
            <TextField
              size="small"
              fullWidth
              label="Search requests"
              value={requestSearch}
              onChange={(e) => { setRequestSearch(e.target.value); setPage(0); }}
              InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
            />
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField select size="small" fullWidth label="Status" value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}>
              <MenuItem value="">All statuses</MenuItem>
              {["DRAFT", "SUBMITTED", "RETURNED", "APPROVED", "REJECTED"].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
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
        <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1} p={1.5}>
          <Box>
            <Typography variant="subtitle2">Leave requests</Typography>
            <Typography variant="body2" color="text.secondary">Requests shown only after selecting a project.</Typography>
          </Box>
          <Chip label={projectId ? `${requestRows.length} visible` : "Pick project"} />
        </Stack>
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
                  <TableCell>{row.requiresTicket ? `${row.ticketFrom ?? ""} -> ${row.ticketTo ?? ""}` : "-"}</TableCell>
                  <TableCell><Chip size="small" label={row.status ?? "-"} /></TableCell>
                  <TableCell align="right">
                    <Button size="small" onClick={() => setRequest(row)}>Edit</Button>
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

function GuideCard({ icon, title, text }: { icon: ReactNode; title: string; text: string }) {
  return (
    <Card variant="outlined" sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Stack direction="row" spacing={1} alignItems="center" mb={1}>
          {icon}
          <Typography variant="subtitle2">{title}</Typography>
        </Stack>
        <Typography variant="body2" color="text.secondary">{text}</Typography>
      </CardContent>
    </Card>
  );
}

function LeaveSummaryPanel({
  projectId,
  projectLabel,
  summaryRows,
  totals,
}: {
  projectId: string;
  projectLabel: string;
  summaryRows: { projectId: string; payGroup: string; total: number; pending: number; approved: number; rejected: number; approvedDays: number }[];
  totals: { total: number; pending: number; approved: number; rejected: number; approvedDays: number };
}) {
  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Stack direction="row" spacing={1} alignItems="center" mb={1}>
        <SummarizeIcon color="primary" />
        <Box>
          <Typography variant="subtitle2">Project leave summary</Typography>
          <Typography variant="body2" color="text.secondary">{projectId ? projectLabel : "Select a project to load totals."}</Typography>
        </Box>
      </Stack>
      {!projectId && <Alert severity="info" sx={{ mb: 1.5 }}>Select a project to view leave requests and totals.</Alert>}
      <Grid container spacing={1} mb={1.5}>
        <Grid item xs={6}><Metric label="Total" value={totals.total} /></Grid>
        <Grid item xs={6}><Metric label="Pending" value={totals.pending} /></Grid>
        <Grid item xs={6}><Metric label="Approved" value={totals.approved} /></Grid>
        <Grid item xs={6}><Metric label="Approved days" value={totals.approvedDays.toFixed(2)} /></Grid>
      </Grid>
      {projectId && summaryRows.length > 0 && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Pay</TableCell>
              <TableCell align="right">Total</TableCell>
              <TableCell align="right">Appr.</TableCell>
              <TableCell align="right">Days</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {summaryRows.map((row) => (
              <TableRow key={`${row.projectId}-${row.payGroup}`} hover>
                <TableCell>{row.payGroup}</TableCell>
                <TableCell align="right">{row.total}</TableCell>
                <TableCell align="right">{row.approved}</TableCell>
                <TableCell align="right">{Number(row.approvedDays ?? 0).toFixed(2)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Paper>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <Paper variant="outlined" sx={{ p: 1, borderRadius: 1.5 }}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="h6">{value}</Typography>
    </Paper>
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
  const missingType = !form.code.trim() || !form.name.trim() || !form.timeTypeId;

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Typography variant="subtitle2">Leave Types</Typography>
      <Typography variant="body2" color="text.secondary" mb={1.5}>
        Configure how each leave type maps to time types and balances.
      </Typography>
      {missingType && (
        <Alert severity="info" sx={{ mb: 1.5 }}>
          Code, name, and time type are required to save a leave type.
        </Alert>
      )}
      <Stack spacing={1.25} mb={1.5}>
        <TextField fullWidth required size="small" label="Code" value={form.code} sx={requiredFieldSx(!form.code.trim())} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
        <TextField fullWidth required size="small" label="Name" value={form.name} sx={requiredFieldSx(!form.name.trim())} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <TextField select fullWidth required size="small" label="Time type" value={form.timeTypeId} sx={requiredFieldSx(!form.timeTypeId)} onChange={(e) => setForm({ ...form, timeTypeId: e.target.value })}>
          {timeTypes.map((t) => <MenuItem key={t.id} value={t.id}>{t.code} - {t.name}</MenuItem>)}
        </TextField>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <FormControlLabel control={<Checkbox checked={form.deductsBalance} onChange={(e) => setForm({ ...form, deductsBalance: e.target.checked })} />} label="Deducts balance" />
          <FormControlLabel control={<Checkbox checked={form.requiresTicketDefault} onChange={(e) => setForm({ ...form, requiresTicketDefault: e.target.checked })} />} label="Ticket" />
        </Stack>
        <Button variant="contained" disabled={missingType || save.isPending} onClick={() => save.mutate(form)}>
          {form.id ? "Update type" : "Add type"}
        </Button>
      </Stack>
      <Stack spacing={0.75}>
        {rows.map((type) => (
          <Paper key={type.id} variant="outlined" sx={{ p: 1, borderRadius: 1.5 }}>
            <Stack direction="row" justifyContent="space-between" spacing={1}>
              <Box>
                <Typography variant="body2" fontWeight={700}>{type.code} - {type.name}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {type.timeTypeCode} - {type.deductsBalance ? "balance" : "no balance"}{type.requiresTicketDefault ? " - ticket" : ""}
                </Typography>
              </Box>
              <Button size="small" onClick={() => setForm(type)}>Edit</Button>
            </Stack>
          </Paper>
        ))}
      </Stack>
    </Paper>
  );
}
