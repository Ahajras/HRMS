import { useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { DataGrid, type GridColDef } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import { payrollComponentApi } from "../api/resources";
import type { PayrollComponent } from "../api/types";

const CATEGORIES = ["SALARY", "ALLOWANCE", "DEDUCTION", "PROVISION", "BONUS", "OVERTIME", "LEAVE", "LOAN", "GOVERNMENT", "TAX", "INSURANCE"];
const TYPES = ["EARNING", "DEDUCTION"];
const METHODS = ["FIXED", "PERCENTAGE", "FORMULA"];
const FREQ = ["MONTHLY", "WEEKLY", "DAILY", "ONE_TIME", "ANNUAL"];

const EMPTY: PayrollComponent = {
  code: "",
  name: "",
  category: "ALLOWANCE",
  componentType: "EARNING",
  paymentFrequency: "MONTHLY",
  calculationMethod: "FIXED",
  roundingMethod: "HALF_UP",
  roundingScale: 2,
  priority: 100,
  taxable: false,
  insurable: false,
  wpsIncluded: true,
  eosIncluded: false,
  provisionIncluded: false,
  leaveIncluded: false,
  visibleOnPayslip: true,
  visibleOnReports: true,
  costAllocationRequired: false,
  approvalRequired: false,
  effectiveFrom: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
};

export default function PayrollComponentsPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["payrollComponents"], queryFn: () => payrollComponentApi.list() });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<PayrollComponent>(EMPTY);

  const save = useMutation({
    mutationFn: (c: PayrollComponent) => (c.id ? payrollComponentApi.update(c.id, c) : payrollComponentApi.create(c)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["payrollComponents"] });
      setOpen(false);
    },
  });

  const columns: GridColDef<PayrollComponent>[] = [
    { field: "code", headerName: "Code", width: 130 },
    { field: "name", headerName: "Name", flex: 1 },
    { field: "category", headerName: "Category", width: 130 },
    { field: "componentType", headerName: "Type", width: 120 },
    { field: "calculationMethod", headerName: "Calc", width: 120 },
    { field: "priority", headerName: "Priority", width: 100 },
    { field: "status", headerName: "Status", width: 110 },
  ];

  const set = <K extends keyof PayrollComponent>(k: K, v: PayrollComponent[K]) => setForm({ ...form, [k]: v });
  const flag = (k: keyof PayrollComponent, label: string) => (
    <Grid item xs={6} sm={4}>
      <FormControlLabel
        control={<Checkbox checked={Boolean(form[k])} onChange={(e) => set(k, e.target.checked as never)} />}
        label={label}
      />
    </Grid>
  );

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Payroll Components</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setForm(EMPTY); setOpen(true); }}>
          New Component
        </Button>
      </Stack>

      <Typography variant="body2" color="text.secondary" mb={2}>
        Salary is a collection of components (FTDD Vol.1 Ch.6). Calculation semantics are resolved by the Rule Engine (Phase 3).
      </Typography>

      <div style={{ height: 560, width: "100%" }}>
        <DataGrid
          rows={data}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => r.id ?? r.code}
          onRowClick={(p) => { setForm(p.row as PayrollComponent); setOpen(true); }}
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>{form.id ? "Edit Component" : "New Component"}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} mt={0}>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth label="Code" value={form.code} onChange={(e) => set("code", e.target.value.toUpperCase())} />
            </Grid>
            <Grid item xs={12} sm={8}>
              <TextField fullWidth label="Name" value={form.name} onChange={(e) => set("name", e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField select fullWidth label="Category" value={form.category} onChange={(e) => set("category", e.target.value)}>
                {CATEGORIES.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField select fullWidth label="Component Type" value={form.componentType} onChange={(e) => set("componentType", e.target.value)}>
                {TYPES.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField select fullWidth label="Payment Frequency" value={form.paymentFrequency} onChange={(e) => set("paymentFrequency", e.target.value)}>
                {FREQ.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField select fullWidth label="Calculation Method" value={form.calculationMethod} onChange={(e) => set("calculationMethod", e.target.value)}>
                {METHODS.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth label="Rounding Method" value={form.roundingMethod} onChange={(e) => set("roundingMethod", e.target.value)} />
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField fullWidth label="Scale" type="number" value={form.roundingScale} onChange={(e) => set("roundingScale", Number(e.target.value))} />
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField fullWidth label="Priority" type="number" value={form.priority} onChange={(e) => set("priority", Number(e.target.value))} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth label="Currency Code" value={form.currencyCode ?? ""}
                onChange={(e) => set("currencyCode", e.target.value.toUpperCase())} inputProps={{ maxLength: 3 }} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth label="Effective From" type="date" InputLabelProps={{ shrink: true }}
                value={form.effectiveFrom} onChange={(e) => set("effectiveFrom", e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth label="Status" value={form.status ?? "ACTIVE"} onChange={(e) => set("status", e.target.value)} />
            </Grid>

            {flag("taxable", "Taxable")}
            {flag("insurable", "Insurable")}
            {flag("wpsIncluded", "WPS Included")}
            {flag("eosIncluded", "EOS Included")}
            {flag("provisionIncluded", "Provision Included")}
            {flag("leaveIncluded", "Leave Included")}
            {flag("visibleOnPayslip", "On Payslip")}
            {flag("visibleOnReports", "On Reports")}
            {flag("costAllocationRequired", "Cost Allocation")}
            {flag("approvalRequired", "Approval Required")}

            <Grid item xs={12}>
              <TextField fullWidth label="Remarks" multiline rows={2} value={form.remarks ?? ""} onChange={(e) => set("remarks", e.target.value)} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => save.mutate(form)} disabled={save.isPending}>Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
