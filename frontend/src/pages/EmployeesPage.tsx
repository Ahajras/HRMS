import { useEffect, useState } from "react";
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  Divider,
  Grid,
  IconButton,
  InputAdornment,
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
import { DataGrid, type GridColDef, type GridPaginationModel } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import PrintIcon from "@mui/icons-material/Print";
import SearchIcon from "@mui/icons-material/Search";
import PhotoCameraIcon from "@mui/icons-material/PhotoCamera";
import {
  assignmentApi,
  bankApi,
  contractApi,
  contractPayItemApi,
  costCodeApi,
  countryApi,
  crewApi,
  currencyApi,
  employeeApi,
  employeeBankAccountApi,
  employeeDocumentApi,
  employeeShiftApi,
  leaveApi,
  legacyRawApi,
  lookupApi,
  overtimeCategoryApi,
  organizationUnitApi,
  payrollComponentApi,
  projectApi,
  projectApprovalRoleApi,
  shiftApi,
  timekeeperApi,
} from "../api/resources";
import type {
  Assignment,
  Contract,
  ContractPayItem,
  Employee,
  EmployeeBankAccount,
  EmployeeDocument,
  EmployeeTimeTypeUsageRow,
  LeaveAdjustment,
} from "../api/types";

const EMPTY: Employee = {
  employeeNumber: "",
  firstName: "",
  lastName: "",
  hireDate: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
};

// --- shared hooks for dropdown sources ---------------------------------
function useLookup(category: string) {
  const { data = [] } = useQuery({
    queryKey: ["lookup", category],
    queryFn: () => lookupApi.byCategory(category),
    staleTime: 5 * 60 * 1000,
  });
  return data;
}

function useCountries() {
  const { data = [] } = useQuery({
    queryKey: ["countries"],
    queryFn: countryApi.list,
    staleTime: 5 * 60 * 1000,
  });
  return data;
}

function useCurrencies() {
  const { data = [] } = useQuery({
    queryKey: ["currencies"],
    queryFn: currencyApi.list,
    staleTime: 5 * 60 * 1000,
  });
  return data;
}

function useBanks() {
  const { data = [] } = useQuery({ queryKey: ["banks"], queryFn: bankApi.list, staleTime: 5 * 60 * 1000 });
  return data;
}

function usePayComponents() {
  const { data = [] } = useQuery({
    queryKey: ["payComponents"],
    queryFn: () => payrollComponentApi.list(),
    staleTime: 5 * 60 * 1000,
  });
  return data;
}

// Generic select bound to {value,label} options.
function SelectField(props: {
  label: string;
  value?: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
  allowEmpty?: boolean;
  required?: boolean;
}) {
  const { label, value, onChange, options, allowEmpty = true, required = false } = props;
  return (
    <TextField select fullWidth required={required} label={label} value={value ?? ""} onChange={(e) => onChange(e.target.value)}>
      {allowEmpty && (
        <MenuItem value="">
          <em>—</em>
        </MenuItem>
      )}
      {options.map((o) => (
        <MenuItem key={o.value} value={o.value}>
          {o.label}
        </MenuItem>
      ))}
    </TextField>
  );
}

// =======================================================================
// Personal details tab
// =======================================================================
function PersonalTab({ form, set }: { form: Employee; set: (k: keyof Employee, v: string) => void }) {
  const genders = useLookup("GENDER");
  const maritals = useLookup("MARITAL_STATUS");
  const statuses = useLookup("EMPLOYEE_STATUS");
  const payStatuses = useLookup("PAY_STATUS");
  const paymentMethods = useLookup("PAYMENT_METHOD");
  const bands = useLookup("BAND");
  const { data: otCategories = [] } = useQuery({ queryKey: ["overtimeCategories"], queryFn: overtimeCategoryApi.list });
  const otCategoryOpts = otCategories.map((c) => ({
    value: c.code,
    label: `${c.code} — ${c.name}${c.otEligible ? "" : " (no OT)"}`,
  }));
  const countries = useCountries();

  const countryOpts = countries.map((c) => ({ value: c.code, label: c.name }));
  const lk = (rows: { code: string; label: string }[]) => rows.map((r) => ({ value: r.code, label: r.label }));

  // Supervisor candidates + the employee's current crew (read-only).
  const { data: empPage } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500) });
  const { data: managerCandidates = [] } = useQuery({
    queryKey: ["approvalRoleCandidates", "MANAGER"],
    queryFn: () => projectApprovalRoleApi.candidates("MANAGER"),
  });
  const { data: timekeeperRows = [] } = useQuery({ queryKey: ["timekeeperProjects"], queryFn: timekeeperApi.list });
  const fallbackSupervisorOpts = (empPage?.content ?? [])
    .filter((e) => e.id !== form.id)
    .map((e) => ({ value: e.id!, label: `${e.employeeNumber} — ${e.firstName} ${e.lastName}` }));
  const managerOpts = managerCandidates
    .filter((c) => c.employeeId !== form.id)
    .map((c) => ({ value: c.employeeId!, label: `${c.employeeNumber} - ${c.employeeName}` }));
  const supervisorOpts = managerOpts.length ? managerOpts : fallbackSupervisorOpts;
  const timekeeperOpts = Array.from(new Map(timekeeperRows.map((t) => [
    t.employeeId,
    { value: t.employeeId, label: `${t.employeeNumber ?? ""} - ${t.employeeName ?? ""}` },
  ])).values());
  const effectiveTimekeeperOpts = timekeeperOpts.length ? timekeeperOpts : supervisorOpts;
  const { data: crew } = useQuery({
    queryKey: ["crewByEmp", form.id],
    queryFn: () => crewApi.byEmployee(form.id!),
    enabled: !!form.id,
  });

  return (
    <Grid container spacing={2} mt={0}>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Employee Number" value={form.employeeNumber}
          onChange={(e) => set("employeeNumber", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="First Name" value={form.firstName} onChange={(e) => set("firstName", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Last Name" value={form.lastName} onChange={(e) => set("lastName", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Middle Name" value={form.middleName ?? ""} onChange={(e) => set("middleName", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Nationality" value={form.nationalityCountryCode}
          onChange={(v) => set("nationalityCountryCode", v)} options={countryOpts} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Date of Birth" type="date" InputLabelProps={{ shrink: true }}
          value={form.dateOfBirth ?? ""} onChange={(e) => set("dateOfBirth", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Gender" value={form.gender} onChange={(v) => set("gender", v)} options={lk(genders)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Marital Status" value={form.maritalStatus} onChange={(v) => set("maritalStatus", v)} options={lk(maritals)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Status" value={form.status} onChange={(v) => set("status", v)} options={lk(statuses)} allowEmpty={false} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Pay Status" value={form.payStatus} onChange={(v) => set("payStatus", v)} options={lk(payStatuses)} allowEmpty={false} required />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Payment Method" value={form.paymentMethodCode ?? "BANK"} onChange={(v) => set("paymentMethodCode", v)} options={lk(paymentMethods)} allowEmpty={false} required />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Band" value={form.band} onChange={(v) => set("band", v)} options={lk(bands)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Overtime Category" value={form.overtimeCategoryCode}
          onChange={(v) => set("overtimeCategoryCode", v)} options={otCategoryOpts} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Hire Date" type="date" InputLabelProps={{ shrink: true }}
          value={form.hireDate} onChange={(e) => set("hireDate", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Termination Date" type="date" InputLabelProps={{ shrink: true }}
          value={form.terminationDate ?? ""} onChange={(e) => set("terminationDate", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Work airport" value={form.workAirportCode ?? ""}
          onChange={(e) => set("workAirportCode", e.target.value.toUpperCase())} helperText="Used for ticket fare route" />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Home airport" value={form.homeAirportCode ?? ""}
          onChange={(e) => set("homeAirportCode", e.target.value.toUpperCase())} helperText="Used for ticket fare route" />
      </Grid>

      <Grid item xs={12}><Divider textAlign="left"><Typography variant="caption">Employment Classification</Typography></Divider></Grid>

      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Job Title" value={form.jobTitle ?? ""} onChange={(e) => set("jobTitle", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Job Title Code" value={form.jobTitleCode ?? ""} onChange={(e) => set("jobTitleCode", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Arabic Name" value={form.arabicName ?? ""} onChange={(e) => set("arabicName", e.target.value)} />
      </Grid>

      <Grid item xs={12}><Divider textAlign="left"><Typography variant="caption">Crew & Supervisor</Typography></Divider></Grid>

      <Grid item xs={12} sm={4}>
        <SelectField label="Supervisor (for approvals)" value={form.supervisorEmployeeId}
          onChange={(v) => set("supervisorEmployeeId", v)} options={supervisorOpts} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <SelectField label="Timekeeper" value={form.timekeeperEmployeeId}
          onChange={(v) => set("timekeeperEmployeeId", v)} options={effectiveTimekeeperOpts} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Crew" value={crew ? `${crew.code} — ${crew.name}` : "—"}
          InputProps={{ readOnly: true }} helperText="Assigned in the Crews screen" />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Foreman" value={crew?.foremanName ?? "—"} InputProps={{ readOnly: true }} />
      </Grid>

      <Grid item xs={12}><Divider textAlign="left"><Typography variant="caption">Contact & Address</Typography></Divider></Grid>

      <Grid item xs={12} sm={6}>
        <TextField fullWidth label="Email" value={form.email ?? ""} onChange={(e) => set("email", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={6}>
        <TextField fullWidth label="Phone" value={form.phone ?? ""} onChange={(e) => set("phone", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={6}>
        <TextField fullWidth label="Address" value={form.addressLine ?? ""} onChange={(e) => set("addressLine", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={3}>
        <TextField fullWidth label="City" value={form.city ?? ""} onChange={(e) => set("city", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={3}>
        <SelectField label="Country of Residence" value={form.countryOfResidenceCode}
          onChange={(v) => set("countryOfResidenceCode", v)} options={countryOpts} />
      </Grid>
    </Grid>
  );
}

// =======================================================================
// Documents tab
// =======================================================================
const EMPTY_DOC = (employeeId: string): EmployeeDocument => ({
  employeeId,
  documentType: "",
  documentNumber: "",
  status: "ACTIVE",
});

function DocumentsTab({ employeeId }: { employeeId: string }) {
  const qc = useQueryClient();
  const docTypes = useLookup("DOCUMENT_TYPE");
  const countries = useCountries();
  const typeLabel = (code: string) => docTypes.find((d) => d.code === code)?.label ?? code;
  const { data = [] } = useQuery({ queryKey: ["docs", employeeId], queryFn: () => employeeDocumentApi.byEmployee(employeeId) });
  const [form, setForm] = useState<EmployeeDocument>(EMPTY_DOC(employeeId));

  const save = useMutation({
    mutationFn: (d: EmployeeDocument) => (d.id ? employeeDocumentApi.update(d.id, d) : employeeDocumentApi.create(d)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["docs", employeeId] }); setForm(EMPTY_DOC(employeeId)); },
  });
  const del = useMutation({
    mutationFn: (id: string) => employeeDocumentApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["docs", employeeId] }),
  });

  const set = (k: keyof EmployeeDocument, v: string) => setForm({ ...form, [k]: v });

  return (
    <Stack spacing={2} mt={1}>
      {data.map((d) => (
        <Paper key={d.id} variant="outlined" sx={{ p: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>{typeLabel(d.documentType)} — {d.documentNumber}</Typography>
              <Typography variant="caption" color="text.secondary">
                {d.issuingCountryCode ? `${d.issuingCountryCode} · ` : ""}
                {d.issueDate ? `issued ${d.issueDate}` : ""}{d.expiryDate ? ` · expires ${d.expiryDate}` : ""}
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setForm(d)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => d.id && del.mutate(d.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No documents yet. Add one below.</Typography>}

      <Divider textAlign="left"><Typography variant="caption">{form.id ? "Edit document" : "Add document"}</Typography></Divider>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={4}>
          <SelectField label="Document Type" value={form.documentType} onChange={(v) => set("documentType", v)}
            options={docTypes.map((d) => ({ value: d.code, label: d.label }))} allowEmpty={false} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Document Number" value={form.documentNumber} onChange={(e) => set("documentNumber", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <SelectField label="Issuing Country" value={form.issuingCountryCode} onChange={(v) => set("issuingCountryCode", v)}
            options={countries.map((c) => ({ value: c.code, label: c.name }))} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Issue Date" type="date" InputLabelProps={{ shrink: true }}
            value={form.issueDate ?? ""} onChange={(e) => set("issueDate", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Expiry Date" type="date" InputLabelProps={{ shrink: true }}
            value={form.expiryDate ?? ""} onChange={(e) => set("expiryDate", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Issuing Authority" value={form.issuingAuthority ?? ""} onChange={(e) => set("issuingAuthority", e.target.value)} />
        </Grid>
        <Grid item xs={12}>
          <Stack direction="row" spacing={1}>
            <Button variant="contained" disabled={!form.documentType || !form.documentNumber || save.isPending}
              onClick={() => save.mutate(form)}>{form.id ? "Update" : "Add"}</Button>
            {form.id && <Button onClick={() => setForm(EMPTY_DOC(employeeId))}>Cancel</Button>}
          </Stack>
        </Grid>
      </Grid>
    </Stack>
  );
}

// =======================================================================
// Bank accounts tab
// =======================================================================
const EMPTY_ACC = (employeeId: string): EmployeeBankAccount => ({
  employeeId,
  primary: true,
  status: "ACTIVE",
});

function BankTab({ employeeId }: { employeeId: string }) {
  const qc = useQueryClient();
  const banks = useBanks();
  const currencies = useCurrencies();
  const bankName = (id?: string) => banks.find((b) => b.id === id)?.name ?? "—";
  const { data = [] } = useQuery({ queryKey: ["accts", employeeId], queryFn: () => employeeBankAccountApi.byEmployee(employeeId) });
  const [form, setForm] = useState<EmployeeBankAccount>(EMPTY_ACC(employeeId));

  const save = useMutation({
    mutationFn: (a: EmployeeBankAccount) => (a.id ? employeeBankAccountApi.update(a.id, a) : employeeBankAccountApi.create(a)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["accts", employeeId] }); setForm(EMPTY_ACC(employeeId)); },
  });
  const del = useMutation({
    mutationFn: (id: string) => employeeBankAccountApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["accts", employeeId] }),
  });

  const set = (k: keyof EmployeeBankAccount, v: string | boolean) => setForm({ ...form, [k]: v } as EmployeeBankAccount);

  return (
    <Stack spacing={2} mt={1}>
      {data.map((a) => (
        <Paper key={a.id} variant="outlined" sx={{ p: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>
                {bankName(a.bankId)} {a.primary ? "· Primary" : ""}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {a.iban ? `IBAN ${a.iban}` : a.accountNumber ? `Acc ${a.accountNumber}` : ""}
                {a.currencyCode ? ` · ${a.currencyCode}` : ""}
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setForm(a)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => a.id && del.mutate(a.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No bank accounts yet. Add one below.</Typography>}

      <Divider textAlign="left"><Typography variant="caption">{form.id ? "Edit account" : "Add account"}</Typography></Divider>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <SelectField label="Bank" value={form.bankId} onChange={(v) => set("bankId", v)}
            options={banks.map((b) => ({ value: b.id!, label: b.name }))} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField fullWidth label="Account Holder Name" value={form.accountHolderName ?? ""} onChange={(e) => set("accountHolderName", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={5}>
          <TextField fullWidth label="IBAN" value={form.iban ?? ""} onChange={(e) => set("iban", e.target.value.toUpperCase())} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Account Number" value={form.accountNumber ?? ""} onChange={(e) => set("accountNumber", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={3}>
          <SelectField label="Currency" value={form.currencyCode} onChange={(v) => set("currencyCode", v)}
            options={currencies.map((c) => ({ value: c.code, label: c.code }))} />
        </Grid>
        <Grid item xs={12} sm={3}>
          <SelectField label="Primary?" value={form.primary ? "Y" : "N"} onChange={(v) => set("primary", v === "Y")}
            options={[{ value: "Y", label: "Yes" }, { value: "N", label: "No" }]} allowEmpty={false} />
        </Grid>
        <Grid item xs={12}>
          <Stack direction="row" spacing={1}>
            <Button variant="contained" disabled={save.isPending} onClick={() => save.mutate(form)}>
              {form.id ? "Update" : "Add"}
            </Button>
            {form.id && <Button onClick={() => setForm(EMPTY_ACC(employeeId))}>Cancel</Button>}
          </Stack>
        </Grid>
      </Grid>
    </Stack>
  );
}

// =======================================================================
// Time type usage tab
// =======================================================================
function TimeUsageTab({ employeeId }: { employeeId: string }) {
  const [year, setYear] = useState(new Date().getFullYear());
  const { data, isLoading } = useQuery({
    queryKey: ["employeeTimeUsage", employeeId, year],
    queryFn: () => employeeApi.timeTypeUsage(employeeId, year),
  });
  const rows = data?.rows ?? [];
  const totalDays = rows.reduce((sum, row) => sum + Number(row.usedDays ?? 0), 0);
  const totalHours = rows.reduce((sum, row) => sum + Number(row.usedHours ?? 0), 0);

  return (
    <Stack spacing={2} mt={2}>
      <Stack direction="row" spacing={1.5} alignItems="center">
        <TextField
          size="small"
          type="number"
          label="Year"
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
          sx={{ width: 140 }}
        />
        <Chip label={`${totalDays.toFixed(2)} days`} />
        <Chip label={`${totalHours.toFixed(2)} hours`} />
      </Stack>
      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Time type</TableCell>
              <TableCell>Category</TableCell>
              <TableCell align="right">Used days</TableCell>
              <TableCell align="right">Used hours</TableCell>
              <TableCell align="right">Occurrences</TableCell>
              <TableCell>First</TableCell>
              <TableCell>Last</TableCell>
              <TableCell>Threshold</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row) => <TimeUsageRow key={row.timeTypeCode} row={row} />)}
            {!isLoading && rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={8}>
                  <Typography variant="body2" color="text.secondary">No leave or absence time types recorded for this year.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Stack>
  );
}

function TimeUsageRow({ row }: { row: EmployeeTimeTypeUsageRow }) {
  const threshold = row.thresholdDays > 0
    ? `After ${row.thresholdDays}d (${row.thresholdScope ?? "CONSECUTIVE"})`
    : "None";
  return (
    <TableRow hover>
      <TableCell>
        <Typography variant="body2" fontWeight={700}>{row.timeTypeCode}</Typography>
        <Typography variant="caption" color="text.secondary">{row.timeTypeName}</Typography>
      </TableCell>
      <TableCell>{row.category}</TableCell>
      <TableCell align="right">{Number(row.usedDays ?? 0).toFixed(2)}</TableCell>
      <TableCell align="right">{Number(row.usedHours ?? 0).toFixed(2)}</TableCell>
      <TableCell align="right">{row.occurrences}</TableCell>
      <TableCell>{row.firstDate ?? ""}</TableCell>
      <TableCell>{row.lastDate ?? ""}</TableCell>
      <TableCell>{threshold}</TableCell>
    </TableRow>
  );
}

function LeaveBalanceTab({ employeeId }: { employeeId: string }) {
  const qc = useQueryClient();
  const asOfDate = new Date().toISOString().slice(0, 10);
  const { data: balances = [] } = useQuery({ queryKey: ["leaveBalances", employeeId, asOfDate], queryFn: () => leaveApi.balances(employeeId, asOfDate) });
  const { data: leaveTypes = [] } = useQuery({ queryKey: ["leaveTypes"], queryFn: leaveApi.types });
  const { data: adjustments = [] } = useQuery({ queryKey: ["leaveAdjustments", employeeId], queryFn: () => leaveApi.adjustments(employeeId) });
  const [form, setForm] = useState<LeaveAdjustment>({
    employeeId,
    leaveTypeId: "",
    adjustmentType: "OPENING_USED",
    days: 0,
    effectiveDate: asOfDate,
  });
  const save = useMutation({
    mutationFn: (payload: LeaveAdjustment) => leaveApi.saveAdjustment(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["leaveAdjustments", employeeId] });
      qc.invalidateQueries({ queryKey: ["leaveBalances", employeeId, asOfDate] });
      setForm({ employeeId, leaveTypeId: "", adjustmentType: "OPENING_USED", days: 0, effectiveDate: asOfDate });
    },
  });
  return (
    <Stack spacing={2} mt={2}>
      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Leave type</TableCell>
              <TableCell align="right">Annual rate</TableCell>
              <TableCell align="right">Entitled</TableCell>
              <TableCell align="right">Adjustments</TableCell>
              <TableCell align="right">Approved used</TableCell>
              <TableCell align="right">Timesheet used</TableCell>
              <TableCell align="right">Pending</TableCell>
              <TableCell align="right">Balance</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {balances.map((b) => (
              <TableRow key={b.leaveTypeId}>
                <TableCell>{b.leaveTypeCode} - {b.leaveTypeName}</TableCell>
                <TableCell align="right">{Number(b.annualRate ?? 0).toFixed(2)}</TableCell>
                <TableCell align="right">{Number(b.entitledToDate ?? 0).toFixed(2)}</TableCell>
                <TableCell align="right">{Number(b.adjustments ?? 0).toFixed(2)}</TableCell>
                <TableCell align="right">{Number(b.usedApproved ?? 0).toFixed(2)}</TableCell>
                <TableCell align="right">{Number(b.usedTimesheet ?? 0).toFixed(2)}</TableCell>
                <TableCell align="right">{Number(b.pending ?? 0).toFixed(2)}</TableCell>
                <TableCell align="right"><b>{Number(b.balance ?? 0).toFixed(2)}</b></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Typography variant="subtitle2" gutterBottom>Balance adjustment</Typography>
        <Stack direction={{ xs: "column", md: "row" }} spacing={1.5}>
          <TextField select size="small" label="Leave type" value={form.leaveTypeId} onChange={(e) => setForm({ ...form, leaveTypeId: e.target.value })} sx={{ minWidth: 220 }}>
            {leaveTypes.filter((t) => t.deductsBalance).map((t) => <MenuItem key={t.id} value={t.id}>{t.code} - {t.name}</MenuItem>)}
          </TextField>
          <TextField select size="small" label="Type" value={form.adjustmentType} onChange={(e) => setForm({ ...form, adjustmentType: e.target.value })} sx={{ minWidth: 180 }}>
            <MenuItem value="OPENING_USED">Opening used</MenuItem>
            <MenuItem value="MANUAL_DEBIT">Manual debit</MenuItem>
            <MenuItem value="MANUAL_CREDIT">Manual credit</MenuItem>
          </TextField>
          <TextField size="small" type="number" label="Days" value={form.days} onChange={(e) => setForm({ ...form, days: Number(e.target.value) })} />
          <TextField size="small" type="date" label="Effective" InputLabelProps={{ shrink: true }} value={form.effectiveDate} onChange={(e) => setForm({ ...form, effectiveDate: e.target.value })} />
          <TextField size="small" label="Reason" value={form.reason ?? ""} onChange={(e) => setForm({ ...form, reason: e.target.value })} sx={{ flex: 1 }} />
          <Button variant="contained" disabled={!form.leaveTypeId || save.isPending} onClick={() => save.mutate(form)}>Add</Button>
        </Stack>
      </Paper>

      {adjustments.map((a) => (
        <Typography key={a.id} variant="body2">
          <b>{a.leaveTypeCode}</b> · {a.adjustmentType} · {Number(a.days).toFixed(2)}d · {a.effectiveDate}{a.reason ? ` · ${a.reason}` : ""}
        </Typography>
      ))}
    </Stack>
  );
}

// =======================================================================
// Contract pay items panel (effective-dated salary structure)
// =======================================================================
const EMPTY_ITEM = (contractId: string, employeeId: string, currency?: string): ContractPayItem => ({
  contractId,
  employeeId,
  payComponentId: "",
  amount: 0,
  currencyCode: currency,
  effectiveFrom: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
  actionSheetNo: "",
});

interface SheetGroup {
  key: string;
  date: string;
  items: ContractPayItem[];
}

function PayItemsPanel({ contractId, employeeId, employeeName, employeeNumber, defaultCurrency, contractLabel }: {
  contractId: string;
  employeeId: string;
  employeeName?: string;
  employeeNumber?: string;
  defaultCurrency?: string;
  contractLabel?: string;
}) {
  const qc = useQueryClient();
  const components = usePayComponents();
  const compById = (id: string) => components.find((c) => c.id === id);
  const compLabel = (id: string) => {
    const c = compById(id);
    return c ? `${c.name} (${c.category})` : id;
  };
  const { data = [] } = useQuery({
    queryKey: ["payitems", contractId],
    queryFn: () => contractPayItemApi.byContract(contractId),
  });
  const [form, setForm] = useState<ContractPayItem>(EMPTY_ITEM(contractId, employeeId, defaultCurrency));
  const [showHistory, setShowHistory] = useState(false);

  const save = useMutation({
    mutationFn: (i: ContractPayItem) => (i.id ? contractPayItemApi.update(i.id, i) : contractPayItemApi.create(i)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["payitems", contractId] });
      setForm(EMPTY_ITEM(contractId, employeeId, defaultCurrency));
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => contractPayItemApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["payitems", contractId] }),
  });

  const today = new Date().toISOString().slice(0, 10);
  // Current value of each component = its latest active row (handles legacy
  // imports that leave an older row ACTIVE, so a component is never double-counted).
  const latestByComp = new Map<string, ContractPayItem>();
  for (const i of data) {
    if (i.status !== "ACTIVE") continue;
    if (i.effectiveTo && i.effectiveTo < today) continue;
    const cur = latestByComp.get(i.payComponentId);
    if (!cur || i.effectiveFrom > cur.effectiveFrom) latestByComp.set(i.payComponentId, i);
  }
  const active = Array.from(latestByComp.values());

  const net = active.reduce((sum, i) => {
    const type = compById(i.payComponentId)?.componentType;
    return sum + (type === "DEDUCTION" ? -Number(i.amount) : Number(i.amount));
  }, 0);

  const fmt = (n: number) => n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const set = (k: keyof ContractPayItem, v: string | number) => setForm({ ...form, [k]: v } as ContractPayItem);
  const sheetOf = (i: ContractPayItem) => i.actionSheetNo?.trim() || i.remarks?.trim() || "—";

  // The action sheet that owns the current values = the one with the latest active effective date.
  const currentSheet = active
    .slice()
    .sort((a, b) => (a.effectiveFrom < b.effectiveFrom ? 1 : -1))[0];
  const currentSheetKey = currentSheet ? sheetOf(currentSheet) : null;

  // Previous amount of the same component (most recent superseded row before this one).
  const priorAmount = (i: ContractPayItem) => {
    const prior = data
      .filter((x) => x.payComponentId === i.payComponentId && x.status !== "ACTIVE" && x.effectiveFrom < i.effectiveFrom)
      .sort((a, b) => (a.effectiveFrom < b.effectiveFrom ? 1 : -1))[0];
    return prior ? Number(prior.amount) : null;
  };

  // Group every row by action sheet, newest first.
  const groups: SheetGroup[] = Object.values(
    data.reduce<Record<string, SheetGroup>>((acc, i) => {
      const k = sheetOf(i);
      if (!acc[k]) acc[k] = { key: k, date: i.effectiveFrom, items: [] };
      acc[k].items.push(i);
      if (i.effectiveFrom < acc[k].date) acc[k].date = i.effectiveFrom;
      return acc;
    }, {}),
  ).sort((a, b) => (a.date < b.date ? 1 : -1));

  const printActionSheet = () => {
    const html = `
      <html>
        <head>
          <title>Action Sheet ${escapeHtml(employeeNumber ?? employeeId)}</title>
          <style>
            body { font-family: Arial, sans-serif; margin: 24px; color: #111827; }
            h1,h2,h3,p { margin: 0; }
            .header { display:flex; justify-content:space-between; margin-bottom:18px; }
            .muted { color:#6b7280; font-size:12px; }
            .section { margin-top:20px; }
            .card { border:1px solid #d1d5db; border-radius:10px; padding:14px; margin-top:12px; }
            table { width:100%; border-collapse: collapse; margin-top:10px; }
            th, td { border:1px solid #d1d5db; padding:8px; text-align:left; vertical-align:top; }
            th { background:#f3f4f6; }
            .right { text-align:right; }
          </style>
        </head>
        <body>
          <div class="header">
            <div>
              <h1>Employee Action Sheet</h1>
              <p>${escapeHtml(employeeName ?? "")}</p>
              <p class="muted">${escapeHtml(employeeNumber ?? "")}${contractLabel ? ` · ${escapeHtml(contractLabel)}` : ""}</p>
            </div>
            <div class="right">
              <p class="muted">Current salary</p>
              <h2>${fmt(net)} ${escapeHtml(defaultCurrency ?? "")}</h2>
            </div>
          </div>

          <div class="section">
            <h2>Current pay items</h2>
            <table>
              <thead>
                <tr><th>Component</th><th>Effective from</th><th>Action sheet</th><th class="right">Amount</th></tr>
              </thead>
              <tbody>
                ${active.map((i) => `
                  <tr>
                    <td>${escapeHtml(compLabel(i.payComponentId))}</td>
                    <td>${escapeHtml(i.effectiveFrom)}</td>
                    <td>${escapeHtml(sheetOf(i))}</td>
                    <td class="right">${fmt(Number(i.amount))}</td>
                  </tr>
                `).join("")}
              </tbody>
            </table>
          </div>

          <div class="section">
            <h2>Action sheet history</h2>
            ${groups.map((g) => `
              <div class="card">
                <h3>${escapeHtml(g.key)}</h3>
                <p class="muted">Effective from ${escapeHtml(g.date)}${g.key === currentSheetKey ? " · current" : ""}</p>
                <table>
                  <thead>
                    <tr><th>Component</th><th>Status</th><th class="right">Amount</th></tr>
                  </thead>
                  <tbody>
                    ${g.items.map((i) => `
                      <tr>
                        <td>${escapeHtml(compLabel(i.payComponentId))}</td>
                        <td>${escapeHtml(i.status ?? "")}</td>
                        <td class="right">${fmt(Number(i.amount))} ${escapeHtml(i.currencyCode ?? defaultCurrency ?? "")}</td>
                      </tr>
                    `).join("")}
                  </tbody>
                </table>
              </div>
            `).join("")}
          </div>
        </body>
      </html>
    `;
    openPrintWindow(html);
  };

  return (
    <Box sx={{ mt: 1.5 }}>
      {/* Current salary snapshot */}
      <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1.5 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="baseline" mb={1}>
          <Typography variant="subtitle2" color="text.secondary">Current salary</Typography>
          <Stack direction="row" spacing={1} alignItems="center">
            <Button size="small" startIcon={<PrintIcon />} onClick={printActionSheet}>Print action sheet</Button>
            <Typography variant="h6" color="primary">{fmt(net)} <Typography component="span" variant="caption" color="text.secondary">{defaultCurrency ?? ""}/mo</Typography></Typography>
          </Stack>
        </Stack>
        {active.length === 0 && <Typography variant="body2" color="text.secondary">No active pay items. Add one below.</Typography>}
        {active.map((i) => (
          <Stack key={i.id} direction="row" alignItems="center" justifyContent="space-between" sx={{ py: 0.75, borderTop: 1, borderColor: "divider" }}>
            <Box>
              <Typography variant="body2" fontWeight={600}>{compLabel(i.payComponentId)}</Typography>
              <Typography variant="caption" color="text.secondary">since {i.effectiveFrom}</Typography>
            </Box>
            <Stack direction="row" alignItems="center" spacing={1}>
              <Chip size="small" label={sheetOf(i)}
                color={sheetOf(i) === currentSheetKey ? "primary" : "default"}
                variant={sheetOf(i) === currentSheetKey ? "filled" : "outlined"}
                sx={{ fontFamily: "monospace", fontSize: 11 }} />
              <Typography variant="body2" fontWeight={600} sx={{ minWidth: 90, textAlign: "right" }}>
                {fmt(Number(i.amount))}
              </Typography>
              <Button size="small" onClick={() => setForm(i)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => i.id && del.mutate(i.id)}><DeleteIcon fontSize="small" /></IconButton>
            </Stack>
          </Stack>
        ))}
        <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1 }}>
          Each line shows the action sheet it currently comes from. Unchanged lines keep their original action sheet.
        </Typography>
      </Paper>

      {/* Action sheet history timeline */}
      <Button size="small" onClick={() => setShowHistory((s) => !s)} sx={{ mb: 1 }}>
        {showHistory ? "Hide action sheet history" : `Action sheet history (${groups.length})`}
      </Button>
      {showHistory && (
        <Box sx={{ position: "relative", pl: 3, mb: 1 }}>
          <Box sx={{ position: "absolute", left: 7, top: 6, bottom: 6, width: "2px", bgcolor: "divider" }} />
          {groups.map((g) => {
            const isCurrent = g.key === currentSheetKey;
            return (
              <Box key={g.key} sx={{ position: "relative", mb: 1.5 }}>
                <Box sx={{ position: "absolute", left: -24, top: 6, width: 14, height: 14, borderRadius: "50%",
                  bgcolor: isCurrent ? "success.main" : "grey.400", border: 2, borderColor: "background.paper" }} />
                <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 2, bgcolor: isCurrent ? "transparent" : "action.hover" }}>
                  <Stack direction="row" alignItems="center" spacing={1} mb={0.5} flexWrap="wrap">
                    <Typography variant="body2" fontWeight={600} sx={{ fontFamily: "monospace", color: isCurrent ? "primary.main" : "text.secondary" }}>{g.key}</Typography>
                    <Typography variant="caption" color="text.secondary">· {g.date}</Typography>
                    <Chip size="small" label={isCurrent ? "Current" : "History"} color={isCurrent ? "success" : "default"}
                      variant="outlined" sx={{ ml: "auto", fontSize: 10, height: 20 }} />
                  </Stack>
                  {g.items.map((i) => {
                    const prev = priorAmount(i);
                    return (
                      <Stack key={i.id} direction="row" alignItems="center" justifyContent="space-between" sx={{ py: 0.5, borderTop: 1, borderColor: "divider" }}>
                        <Typography variant="caption">{compLabel(i.payComponentId)}</Typography>
                        <Stack direction="row" alignItems="center" spacing={1}>
                          {prev !== null && (
                            <Typography variant="caption" color="text.secondary" sx={{ textDecoration: "line-through" }}>{fmt(prev)}</Typography>
                          )}
                          <Typography variant="caption" fontWeight={600}>{fmt(Number(i.amount))} {i.currencyCode ?? defaultCurrency ?? ""}</Typography>
                          <Button size="small" onClick={() => setForm(i)}>Edit</Button>
                          <IconButton size="small" color="error" onClick={() => i.id && del.mutate(i.id)}><DeleteIcon fontSize="small" /></IconButton>
                        </Stack>
                      </Stack>
                    );
                  })}
                </Paper>
              </Box>
            );
          })}
        </Box>
      )}

      {/* Add / change a pay item */}
      <Divider sx={{ my: 1.5 }} textAlign="left"><Typography variant="caption">Add / change a pay item</Typography></Divider>
      <Grid container spacing={1.5}>
        <Grid item xs={12} sm={4}>
          <SelectField label="Component" value={form.payComponentId} onChange={(v) => set("payComponentId", v)}
            options={components.map((c) => ({ value: c.id!, label: `${c.name} (${c.category})` }))} allowEmpty={false} />
        </Grid>
        <Grid item xs={6} sm={3}>
          <TextField fullWidth label="Amount" type="number" value={form.amount || ""} onChange={(e) => set("amount", Number(e.target.value))} />
        </Grid>
        <Grid item xs={6} sm={2}>
          <TextField fullWidth label="Currency" value={form.currencyCode ?? ""} onChange={(e) => set("currencyCode", e.target.value.toUpperCase())} inputProps={{ maxLength: 3 }} />
        </Grid>
        <Grid item xs={12} sm={3}>
          <TextField fullWidth label="Effective From" type="date" InputLabelProps={{ shrink: true }}
            value={form.effectiveFrom} onChange={(e) => set("effectiveFrom", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={5}>
          <TextField fullWidth label="Action Sheet No" value={form.actionSheetNo ?? ""} onChange={(e) => set("actionSheetNo", e.target.value)} inputProps={{ maxLength: 40 }} />
        </Grid>
        <Grid item xs={12} sm={7}>
          <TextField fullWidth label="Remarks (optional note)" value={form.remarks ?? ""} onChange={(e) => set("remarks", e.target.value)} />
        </Grid>
        <Grid item xs={12}>
          <Button variant="contained" size="small"
            disabled={!form.payComponentId || !form.amount || save.isPending}
            onClick={() => save.mutate(form)}>{form.id ? "Update" : "Apply"}</Button>
          {form.id && <Button size="small" sx={{ ml: 1 }} onClick={() => setForm(EMPTY_ITEM(contractId, employeeId, defaultCurrency))}>Cancel</Button>}
          {save.isError && <Typography variant="caption" color="error" sx={{ ml: 2 }}>
            Could not apply. The new effective date must be after the current item's date.
          </Typography>}
        </Grid>
      </Grid>
      <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1 }}>
        New rows supersede by effective date. Editing a selected row updates that row in place.
      </Typography>
    </Box>
  );
}

// =======================================================================
// Contracts tab
// =======================================================================
const EMPTY_CONTRACT = (employeeId: string): Contract => ({
  employeeId,
  contractType: "",
  effectiveFrom: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
});

function ContractsTab({ employeeId, employeeName, employeeNumber }: { employeeId: string; employeeName?: string; employeeNumber?: string }) {
  const qc = useQueryClient();
  const types = useLookup("CONTRACT_TYPE");
  const statuses = useLookup("CONTRACT_STATUS");
  const currencies = useCurrencies();
  const typeLabel = (code: string) => types.find((t) => t.code === code)?.label ?? code;
  const { data = [] } = useQuery({ queryKey: ["contracts", employeeId], queryFn: () => contractApi.byEmployee(employeeId) });
  const [form, setForm] = useState<Contract>(EMPTY_CONTRACT(employeeId));

  const save = useMutation({
    mutationFn: (c: Contract) => (c.id ? contractApi.update(c.id, c) : contractApi.create(c)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["contracts", employeeId] }); setForm(EMPTY_CONTRACT(employeeId)); },
  });
  const del = useMutation({
    mutationFn: (id: string) => contractApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["contracts", employeeId] }),
  });

  const set = (k: keyof Contract, v: string) => setForm({ ...form, [k]: v });
  const setNum = (k: keyof Contract, v: string) => setForm({ ...form, [k]: v === "" ? undefined : Number(v) });
  const [openItems, setOpenItems] = useState<string | null>(null);

  return (
    <Stack spacing={2} mt={1}>
      {data.map((c) => (
        <Paper key={c.id} variant="outlined" sx={{ p: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>{typeLabel(c.contractType)} {c.contractNumber ? `· ${c.contractNumber}` : ""}</Typography>
              <Typography variant="caption" color="text.secondary">
                {c.effectiveFrom}{c.effectiveTo ? ` → ${c.effectiveTo}` : " → open"}
                {c.baseCurrencyCode ? ` · ${c.baseCurrencyCode}` : ""} · {c.status}
                {c.workingHoursPerWeek != null ? ` · ${c.workingHoursPerWeek}h/wk` : ""}
                {c.workingDaysPerWeek != null ? ` · ${c.workingDaysPerWeek}d/wk` : ""}
                {c.overtimeCategory ? ` · OT ${c.overtimeCategory}` : ""}
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setOpenItems(openItems === c.id ? null : (c.id ?? null))}>
                {openItems === c.id ? "Hide pay items" : "Pay items"}
              </Button>
              <Button size="small" onClick={() => setForm(c)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => c.id && del.mutate(c.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
          {openItems === c.id && c.id && (
            <PayItemsPanel
              contractId={c.id}
              employeeId={employeeId}
              employeeName={employeeName}
              employeeNumber={employeeNumber}
              defaultCurrency={c.baseCurrencyCode}
              contractLabel={`${typeLabel(c.contractType)}${c.contractNumber ? ` · ${c.contractNumber}` : ""}`}
            />
          )}
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No contracts yet. Add one below.</Typography>}

      <Divider textAlign="left"><Typography variant="caption">{form.id ? "Edit contract" : "Add contract"}</Typography></Divider>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={4}>
          <SelectField label="Contract Type" value={form.contractType} onChange={(v) => set("contractType", v)}
            options={types.map((t) => ({ value: t.code, label: t.label }))} allowEmpty={false} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Contract Number" value={form.contractNumber ?? ""} onChange={(e) => set("contractNumber", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <SelectField label="Base Currency" value={form.baseCurrencyCode} onChange={(v) => set("baseCurrencyCode", v)}
            options={currencies.map((c) => ({ value: c.code, label: c.code }))} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Effective From" type="date" InputLabelProps={{ shrink: true }}
            value={form.effectiveFrom} onChange={(e) => set("effectiveFrom", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Effective To" type="date" InputLabelProps={{ shrink: true }}
            value={form.effectiveTo ?? ""} onChange={(e) => set("effectiveTo", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <SelectField label="Status" value={form.status} onChange={(v) => set("status", v)}
            options={statuses.map((s) => ({ value: s.code, label: s.label }))} allowEmpty={false} />
        </Grid>
        <Grid item xs={12}>
          <Divider textAlign="left"><Typography variant="caption">Reference terms (نظري فقط — الساعات الفعلية من التايم شيت)</Typography></Divider>
        </Grid>
        <Grid item xs={12} sm={3}>
          <TextField fullWidth label="Working Hours / Week" type="number" value={form.workingHoursPerWeek ?? ""}
            onChange={(e) => setNum("workingHoursPerWeek", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={3}>
          <TextField fullWidth label="Working Days / Week" type="number" value={form.workingDaysPerWeek ?? ""}
            onChange={(e) => setNum("workingDaysPerWeek", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={3}>
          <TextField fullWidth label="Overtime Category" value={form.overtimeCategory ?? ""}
            onChange={(e) => set("overtimeCategory", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={3}>
          <TextField fullWidth label="Overtime Category Desc" value={form.overtimeCategoryDesc ?? ""}
            onChange={(e) => set("overtimeCategoryDesc", e.target.value)} />
        </Grid>
        <Grid item xs={12}>
          <Stack direction="row" spacing={1}>
            <Button variant="contained" disabled={!form.contractType || save.isPending} onClick={() => save.mutate(form)}>
              {form.id ? "Update" : "Add"}
            </Button>
            {form.id && <Button onClick={() => setForm(EMPTY_CONTRACT(employeeId))}>Cancel</Button>}
          </Stack>
        </Grid>
      </Grid>
    </Stack>
  );
}

// =======================================================================
// Legacy Data — faithful snapshot of the old system (every column, even blank)
// =======================================================================
function LegacyTab({ employeeId }: { employeeId: string }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["legacy-raw", employeeId],
    queryFn: () => legacyRawApi.byEmployee(employeeId),
    retry: false,
  });

  if (isLoading) {
    return <Typography sx={{ mt: 2 }}>Loading…</Typography>;
  }
  if (isError || !data) {
    return (
      <Alert severity="info" sx={{ mt: 2 }}>
        No legacy snapshot for this employee. Run an import from the legacy system to populate it.
      </Alert>
    );
  }

  const headerEntries = Object.entries(data.header ?? {});
  const detailCols = data.detail && data.detail.length > 0 ? Object.keys(data.detail[0]) : [];

  return (
    <Stack spacing={3} sx={{ mt: 2 }}>
      <Alert severity="info">
        نسخة خام من النظام القديم — كل الحقول كما هي حتى الفاضية. للأرشيف فقط (المحركات تقرأ من الجداول العادية).
        {data.importedAt ? ` · Imported: ${new Date(data.importedAt).toLocaleString()}` : ""}
        {data.source ? ` · Source: ${data.source}` : ""}
      </Alert>

      <Box>
        <Typography variant="subtitle1" sx={{ mb: 1 }}>
          Header ({headerEntries.length} fields)
        </Typography>
        <Paper variant="outlined">
          <Grid container>
            {headerEntries.map(([k, v]) => (
              <Grid item xs={12} sm={6} md={4} key={k} sx={{ p: 1, borderBottom: 1, borderColor: "divider" }}>
                <Typography variant="caption" color="text.secondary" sx={{ display: "block" }}>
                  {k}
                </Typography>
                <Typography variant="body2" sx={{ wordBreak: "break-word" }}>
                  {v === "" ? "—" : v}
                </Typography>
              </Grid>
            ))}
          </Grid>
        </Paper>
      </Box>

      <Box>
        <Typography variant="subtitle1" sx={{ mb: 1 }}>
          Detail / pay lines ({data.detail?.length ?? 0})
        </Typography>
        {detailCols.length === 0 ? (
          <Alert severity="info">No detail lines.</Alert>
        ) : (
          <div style={{ width: "100%", overflowX: "auto" }}>
            <DataGrid
              autoHeight
              density="compact"
              rows={data.detail.map((row, i) => ({ id: i, ...row }))}
              columns={detailCols.map((c) => ({ field: c, headerName: c, minWidth: 120, flex: 1 }))}
              hideFooterSelectedRowCount
              pageSizeOptions={[5, 10, 25]}
              initialState={{ pagination: { paginationModel: { pageSize: 10, page: 0 } } }}
            />
          </div>
        )}
      </Box>
    </Stack>
  );
}

// =======================================================================
// Assignment tab (org unit / position / supervisor / project / cost code)
// =======================================================================
const EMPTY_ASSIGNMENT = (employeeId: string): Assignment => ({
  employeeId,
  organizationUnitId: "",
  primaryAssignment: true,
  effectiveFrom: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
});

function AssignmentTab({ employeeId }: { employeeId: string }) {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["assignments", employeeId], queryFn: () => assignmentApi.byEmployee(employeeId) });
  const { data: orgUnits = [] } = useQuery({ queryKey: ["orgUnits"], queryFn: organizationUnitApi.list, staleTime: 5 * 60 * 1000 });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list, staleTime: 5 * 60 * 1000 });
  const [form, setForm] = useState<Assignment>(EMPTY_ASSIGNMENT(employeeId));
  const { data: costCodes = [] } = useQuery({
    queryKey: ["costCodes", form.projectId],
    queryFn: () => costCodeApi.byProject(form.projectId!),
    enabled: Boolean(form.projectId),
  });

  const orgName = (id: string) => orgUnits.find((o) => o.id === id)?.name ?? id;
  const projName = (id?: string) => projects.find((p) => p.id === id)?.name ?? "—";

  const save = useMutation({
    mutationFn: (a: Assignment) => (a.id ? assignmentApi.update(a.id, a) : assignmentApi.create(a)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["assignments", employeeId] });
      qc.invalidateQueries({ queryKey: ["employees"] });
      qc.invalidateQueries({ queryKey: ["employees-summary"] });
      setForm(EMPTY_ASSIGNMENT(employeeId));
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => assignmentApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["assignments", employeeId] });
      qc.invalidateQueries({ queryKey: ["employees"] });
      qc.invalidateQueries({ queryKey: ["employees-summary"] });
    },
  });

  const set = (k: keyof Assignment, v: string | boolean) => setForm({ ...form, [k]: v } as Assignment);

  return (
    <Stack spacing={2} mt={1}>
      {data.map((a) => (
        <Paper key={a.id} variant="outlined" sx={{ p: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>
                {orgName(a.organizationUnitId)}{a.positionTitle ? ` · ${a.positionTitle}` : ""} {a.primaryAssignment ? "· Primary" : ""}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Project: {projName(a.projectId)} · {a.effectiveFrom}{a.effectiveTo ? ` → ${a.effectiveTo}` : " → open"} · {a.status}
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setForm(a)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => a.id && del.mutate(a.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No assignments yet. Add one below.</Typography>}

      <Divider textAlign="left"><Typography variant="caption">{form.id ? "Edit assignment" : "Add assignment"}</Typography></Divider>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <SelectField label="Organization Unit" value={form.organizationUnitId} onChange={(v) => set("organizationUnitId", v)}
            options={orgUnits.map((o) => ({ value: o.id!, label: o.name }))} allowEmpty={false} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <TextField fullWidth label="Position" value={form.positionTitle ?? ""} onChange={(e) => set("positionTitle", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <SelectField label="Project" value={form.projectId} onChange={(v) => setForm({ ...form, projectId: v, costCodeId: undefined })}
            options={projects.map((p) => ({ value: p.id!, label: `${p.code} — ${p.name}` }))} />
        </Grid>
        <Grid item xs={12} sm={6}>
          <SelectField label="Cost Code" value={form.costCodeId} onChange={(v) => set("costCodeId", v)}
            options={costCodes.map((c) => ({ value: c.id!, label: `${c.code} — ${c.name}` }))} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <TextField fullWidth label="Effective From" type="date" InputLabelProps={{ shrink: true }}
            value={form.effectiveFrom} onChange={(e) => set("effectiveFrom", e.target.value)} />
        </Grid>
        <Grid item xs={12} sm={4}>
          <SelectField label="Primary?" value={form.primaryAssignment ? "Y" : "N"} onChange={(v) => set("primaryAssignment", v === "Y")}
            options={[{ value: "Y", label: "Yes" }, { value: "N", label: "No" }]} allowEmpty={false} />
        </Grid>
        <Grid item xs={12}>
          <Stack direction="row" spacing={1}>
            <Button variant="contained" disabled={!form.organizationUnitId || save.isPending} onClick={() => save.mutate(form)}>
              {form.id ? "Update" : "Add"}
            </Button>
            {form.id && <Button onClick={() => setForm(EMPTY_ASSIGNMENT(employeeId))}>Cancel</Button>}
          </Stack>
        </Grid>
      </Grid>
    </Stack>
  );
}

// =======================================================================
// Page
// =======================================================================
export default function EmployeesPage() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [search, setSearch] = useState("");
  const [debounced, setDebounced] = useState("");
  const [payFilter, setPayFilter] = useState("");
  const [projectId, setProjectId] = useState(""); // "" = all projects
  const [unassignedOnly, setUnassignedOnly] = useState(false);
  useEffect(() => {
    const t = setTimeout(() => {
      setDebounced(search);
      setPagination((p) => ({ ...p, page: 0 }));
    }, 350);
    return () => clearTimeout(t);
  }, [search]);
  const { data: projects = [] } = useQuery({
    queryKey: ["projects"],
    queryFn: projectApi.list,
    staleTime: 5 * 60 * 1000,
  });
  const payStatuses = useLookup("PAY_STATUS");
  const { data, isLoading } = useQuery({
    queryKey: ["employees", pagination.page, pagination.pageSize, debounced, payFilter, projectId, unassignedOnly],
    queryFn: () =>
      employeeApi.list(
        pagination.page,
        pagination.pageSize,
        debounced || undefined,
        payFilter || undefined,
        projectId || undefined,
        { unassigned: unassignedOnly },
      ),
  });
  const { data: summary } = useQuery({
    queryKey: ["employees-summary", debounced, projectId],
    queryFn: () => employeeApi.summary(debounced || undefined, projectId || undefined),
  });
  const { data: projectSummary = [] } = useQuery({
    queryKey: ["employees-project-summary"],
    queryFn: employeeApi.projectSummary,
  });
  const { data: projectEmployees } = useQuery({
    queryKey: ["employees-project-validation", projectId],
    queryFn: () => employeeApi.list(0, 500, undefined, undefined, projectId || undefined, { assignedOnly: true }),
    enabled: !!projectId,
  });
  const { data: projectShifts = [] } = useQuery({
    queryKey: ["shifts", projectId || "all"],
    queryFn: () => shiftApi.list(projectId || undefined),
    enabled: !!projectId,
  });
  const { data: roster = [] } = useQuery({
    queryKey: ["roster"],
    queryFn: () => employeeShiftApi.list(),
    enabled: !!projectId,
  });
  const projectShiftIds = new Set(projectShifts.map((s) => s.id).filter(Boolean));
  const employeesMissingShift = projectId
    ? (projectEmployees?.content ?? []).filter((emp) => emp.id && !roster.some((r) =>
        r.employeeId === emp.id
        && r.status !== "INACTIVE"
        && r.shiftId
        && projectShiftIds.has(r.shiftId)))
    : [];
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState(0);
  const [form, setForm] = useState<Employee>(EMPTY);

  const save = useMutation({
    mutationFn: (e: Employee) => (e.id ? employeeApi.update(e.id, e) : employeeApi.create(e)),
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ["employees"] });
      setForm(saved); // keep dialog open; now has an id so sub-tabs unlock
    },
  });

  const columns: GridColDef<Employee>[] = [
    { field: "employeeNumber", headerName: "Emp #", width: 120 },
    { field: "firstName", headerName: "First Name", flex: 1 },
    { field: "lastName", headerName: "Last Name", flex: 1 },
    { field: "hireDate", headerName: "Hire Date", width: 140 },
    { field: "email", headerName: "Email", flex: 1 },
    { field: "payStatus", headerName: "Pay Status", width: 130 },
    { field: "status", headerName: "Status", width: 110 },
  ];

  const set = (k: keyof Employee, v: string) => setForm({ ...form, [k]: v });
  const openNew = () => { setForm(EMPTY); setTab(0); setOpen(true); };
  const openExisting = (e: Employee) => { setForm(e); setTab(0); setOpen(true); };

  const isSaved = Boolean(form.id);

  const { data: headerCrew } = useQuery({
    queryKey: ["crewByEmp", form.id],
    queryFn: () => crewApi.byEmployee(form.id!),
    enabled: open && isSaved,
  });
  const onPhoto = (file?: File) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setForm((f) => ({ ...f, photoUrl: String(reader.result) }));
    reader.readAsDataURL(file);
  };
  const initials = `${form.firstName?.[0] ?? ""}${form.lastName?.[0] ?? ""}`.toUpperCase();

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Employees</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={openNew}>New Employee</Button>
      </Stack>

      <TextField
        select
        size="small"
        label="Project"
        value={projectId}
        onChange={(e) => { setProjectId(e.target.value); setUnassignedOnly(false); setPagination((p) => ({ ...p, page: 0 })); }}
        sx={{ minWidth: 260, mb: 2 }}
      >
        <MenuItem value="">All projects</MenuItem>
        {projects.map((p) => (
          <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>
        ))}
      </TextField>

      <Grid container spacing={2} mb={2}>
        {[
          { label: "Total", value: summary?.total, color: "text.primary" },
          { label: "Active", value: summary?.active, color: "success.main" },
          { label: "Not Active", value: summary?.notActive, color: "error.main" },
          { label: "Monthly", value: summary?.monthly, color: "primary.main" },
          { label: "Daily", value: summary?.daily, color: "warning.main" },
          { label: "Without Project", value: summary?.withoutProject, color: "secondary.main", action: "unassigned" },
          ...(projectId ? [{ label: "Need Shift", value: employeesMissingShift.length, color: "warning.dark" }] : []),
        ].map((c) => (
          <Grid item xs={6} sm={4} md={2} key={c.label}>
            <Paper variant="outlined"
              onClick={() => {
                if (c.action === "unassigned") {
                  setUnassignedOnly((v) => !v);
                  setProjectId("");
                  setPagination((p) => ({ ...p, page: 0 }));
                }
              }}
              sx={{
                p: 2,
                textAlign: "center",
                cursor: c.action ? "pointer" : "default",
                borderColor: c.action === "unassigned" && unassignedOnly ? "secondary.main" : "divider",
                bgcolor: c.action === "unassigned" && unassignedOnly ? "action.hover" : "background.paper",
              }}>
              <Typography variant="h5" sx={{ color: c.color, fontWeight: 600 }}>
                {c.value ?? "—"}
              </Typography>
              <Typography variant="body2" color="text.secondary">{c.label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Paper variant="outlined" sx={{ mb: 2 }}>
        <Box sx={{ p: 1.5 }}>
          <Typography variant="subtitle2">Project employee summary</Typography>
        </Box>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Project</TableCell>
              <TableCell align="right">Total</TableCell>
              <TableCell align="right">Active</TableCell>
              <TableCell align="right">Monthly</TableCell>
              <TableCell align="right">Daily</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {projectSummary.map((row) => (
              <TableRow key={row.projectId} hover onClick={() => { setProjectId(row.projectId); setPagination((p) => ({ ...p, page: 0 })); }} sx={{ cursor: "pointer" }}>
                <TableCell>{row.projectCode} — {row.projectName}</TableCell>
                <TableCell align="right">{row.total}</TableCell>
                <TableCell align="right">{row.active}</TableCell>
                <TableCell align="right">{row.monthly}</TableCell>
                <TableCell align="right">{row.daily}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      {projectId && employeesMissingShift.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {employeesMissingShift.length} employee(s) on this project do not have a matching shift roster. Assign a shared shift or a shift for this project before generating timesheets.
        </Alert>
      )}

      <Tabs
        value={payFilter}
        onChange={(_, v) => { setPayFilter(v); setPagination((p) => ({ ...p, page: 0 })); }}
        sx={{ mb: 1, borderBottom: 1, borderColor: "divider" }}
      >
        <Tab label="All" value="" />
        {payStatuses.map((status) => (
          <Tab key={status.code} label={status.label || status.code} value={status.code} />
        ))}
      </Tabs>

      <TextField
        fullWidth
        size="small"
        placeholder={
          payFilter === "MONTHLY" ? "Search monthly employees…"
            : payFilter === "DAILY" ? "Search daily employees…"
            : "Search by employee number, name, or action sheet…"
        }
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        sx={{ mb: 2 }}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment>
          ),
          endAdornment: search ? (
            <InputAdornment position="end">
              <IconButton size="small" onClick={() => setSearch("")}><DeleteIcon fontSize="small" /></IconButton>
            </InputAdornment>
          ) : null,
        }}
      />

      <div style={{ height: 600, width: "100%" }}>
        <DataGrid
          rows={data?.content ?? []}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => r.id ?? r.employeeNumber}
          rowCount={data?.totalElements ?? 0}
          paginationMode="server"
          paginationModel={pagination}
          onPaginationModelChange={setPagination}
          pageSizeOptions={[10, 20, 50]}
          onRowClick={(p) => openExisting(p.row as Employee)}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="md">
        <Box sx={{
          p: 2.5,
          display: "flex",
          alignItems: "center",
          gap: 2,
          color: "common.white",
          background: "linear-gradient(120deg, #1565c0 0%, #1e88e5 60%, #42a5f5 100%)",
        }}>
          <Box sx={{ position: "relative" }}>
            <Avatar src={form.photoUrl || undefined} sx={{ width: 76, height: 76, fontSize: 28, bgcolor: "rgba(255,255,255,0.25)", border: "2px solid rgba(255,255,255,0.7)" }}>
              {initials || "?"}
            </Avatar>
            <IconButton component="label" size="small"
              sx={{ position: "absolute", right: -6, bottom: -6, bgcolor: "common.white", boxShadow: 1, "&:hover": { bgcolor: "grey.100" } }}>
              <PhotoCameraIcon fontSize="small" color="primary" />
              <input hidden type="file" accept="image/*" onChange={(e) => onPhoto(e.target.files?.[0])} />
            </IconButton>
          </Box>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="h6" noWrap>{isSaved ? `${form.firstName} ${form.lastName}` : "New Employee"}</Typography>
            {isSaved && <Typography variant="body2" sx={{ opacity: 0.85 }}>#{form.employeeNumber}{form.jobTitle ? ` · ${form.jobTitle}` : ""}</Typography>}
            <Stack direction="row" spacing={0.75} mt={0.75} flexWrap="wrap" useFlexGap>
              {headerCrew?.code && <Chip size="small" label={`Crew ${headerCrew.code}`} sx={{ bgcolor: "#7b1fa2", color: "#fff", fontWeight: 600 }} />}
              {headerCrew?.foremanName && <Chip size="small" label={`Foreman: ${headerCrew.foremanName}`} sx={{ bgcolor: "rgba(255,255,255,0.22)", color: "#fff" }} />}
              {form.supervisorName && <Chip size="small" label={`Supervisor: ${form.supervisorName}`} sx={{ bgcolor: "#00897b", color: "#fff", fontWeight: 600 }} />}
              {form.payStatus && <Chip size="small" label={form.payStatus} sx={{ bgcolor: "rgba(255,255,255,0.22)", color: "#fff" }} />}
              {form.status && <Chip size="small" label={form.status} sx={{ bgcolor: form.status === "ACTIVE" ? "#2e7d32" : "#c62828", color: "#fff" }} />}
            </Stack>
          </Box>
        </Box>
        <DialogContent>
          <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" sx={{ borderBottom: 1, borderColor: "divider" }}>
            <Tab label="Personal" />
            <Tab label="Documents" />
            <Tab label="Bank" />
            <Tab label="Contracts" />
            <Tab label="Assignment" />
            <Tab label="Leave Balance" />
            <Tab label="Time Usage" />
            <Tab label="Legacy Data" />
          </Tabs>

          {tab === 0 && <PersonalTab form={form} set={set} />}

          {tab > 0 && !isSaved && (
            <Alert severity="info" sx={{ mt: 2 }}>
              Save the employee first, then add documents, bank accounts, and contracts.
            </Alert>
          )}
          {tab === 1 && isSaved && <DocumentsTab employeeId={form.id!} />}
          {tab === 2 && isSaved && <BankTab employeeId={form.id!} />}
          {tab === 3 && isSaved && <ContractsTab employeeId={form.id!} employeeName={`${form.firstName ?? ""} ${form.lastName ?? ""}`.trim()} employeeNumber={form.employeeNumber} />}
          {tab === 4 && isSaved && <AssignmentTab employeeId={form.id!} />}
          {tab === 5 && isSaved && <LeaveBalanceTab employeeId={form.id!} />}
          {tab === 6 && isSaved && <TimeUsageTab employeeId={form.id!} />}
          {tab === 7 && isSaved && <LegacyTab employeeId={form.id!} />}

          {save.isError && <Alert severity="error" sx={{ mt: 2 }}>Could not save. Check the fields and try again.</Alert>}
          {save.isSuccess && tab === 0 && <Alert severity="success" sx={{ mt: 2 }}>Saved.</Alert>}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Close</Button>
          {tab === 0 && (
            <Button variant="contained" onClick={() => save.mutate(form)} disabled={save.isPending || !form.payStatus}>
              {isSaved ? "Update" : "Save"}
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
}

function openPrintWindow(html: string) {
  const win = window.open("", "_blank", "width=1100,height=900");
  if (!win) return;
  win.document.open();
  win.document.write(html);
  win.document.close();
  win.focus();
  win.onload = () => win.print();
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
