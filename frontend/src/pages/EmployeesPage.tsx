import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from "@mui/material";
import { DataGrid, type GridColDef, type GridPaginationModel } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import {
  bankApi,
  contractApi,
  countryApi,
  currencyApi,
  employeeApi,
  employeeBankAccountApi,
  employeeDocumentApi,
  lookupApi,
} from "../api/resources";
import type {
  Contract,
  Employee,
  EmployeeBankAccount,
  EmployeeDocument,
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

// Generic select bound to {value,label} options.
function SelectField(props: {
  label: string;
  value?: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
  allowEmpty?: boolean;
}) {
  const { label, value, onChange, options, allowEmpty = true } = props;
  return (
    <TextField select fullWidth label={label} value={value ?? ""} onChange={(e) => onChange(e.target.value)}>
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
  const countries = useCountries();

  const countryOpts = countries.map((c) => ({ value: c.code, label: c.name }));
  const lk = (rows: { code: string; label: string }[]) => rows.map((r) => ({ value: r.code, label: r.label }));

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
        <TextField fullWidth label="Hire Date" type="date" InputLabelProps={{ shrink: true }}
          value={form.hireDate} onChange={(e) => set("hireDate", e.target.value)} />
      </Grid>
      <Grid item xs={12} sm={4}>
        <TextField fullWidth label="Termination Date" type="date" InputLabelProps={{ shrink: true }}
          value={form.terminationDate ?? ""} onChange={(e) => set("terminationDate", e.target.value)} />
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
// Contracts tab
// =======================================================================
const EMPTY_CONTRACT = (employeeId: string): Contract => ({
  employeeId,
  contractType: "",
  effectiveFrom: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
});

function ContractsTab({ employeeId }: { employeeId: string }) {
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
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setForm(c)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => c.id && del.mutate(c.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
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
// Page
// =======================================================================
export default function EmployeesPage() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const { data, isLoading } = useQuery({
    queryKey: ["employees", pagination.page, pagination.pageSize],
    queryFn: () => employeeApi.list(pagination.page, pagination.pageSize),
  });
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
    { field: "status", headerName: "Status", width: 110 },
  ];

  const set = (k: keyof Employee, v: string) => setForm({ ...form, [k]: v });
  const openNew = () => { setForm(EMPTY); setTab(0); setOpen(true); };
  const openExisting = (e: Employee) => { setForm(e); setTab(0); setOpen(true); };

  const isSaved = Boolean(form.id);

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Employees</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={openNew}>New Employee</Button>
      </Stack>

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
        <DialogTitle>{isSaved ? `${form.firstName} ${form.lastName}` : "New Employee"}</DialogTitle>
        <DialogContent>
          <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" sx={{ borderBottom: 1, borderColor: "divider" }}>
            <Tab label="Personal" />
            <Tab label="Documents" />
            <Tab label="Bank" />
            <Tab label="Contracts" />
          </Tabs>

          {tab === 0 && <PersonalTab form={form} set={set} />}

          {tab > 0 && !isSaved && (
            <Alert severity="info" sx={{ mt: 2 }}>
              Save the employee first, then add documents, bank accounts, and contracts.
            </Alert>
          )}
          {tab === 1 && isSaved && <DocumentsTab employeeId={form.id!} />}
          {tab === 2 && isSaved && <BankTab employeeId={form.id!} />}
          {tab === 3 && isSaved && <ContractsTab employeeId={form.id!} />}

          {save.isError && <Alert severity="error" sx={{ mt: 2 }}>Could not save. Check the fields and try again.</Alert>}
          {save.isSuccess && tab === 0 && <Alert severity="success" sx={{ mt: 2 }}>Saved.</Alert>}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Close</Button>
          {tab === 0 && (
            <Button variant="contained" onClick={() => save.mutate(form)} disabled={save.isPending}>
              {isSaved ? "Update" : "Save"}
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
}
