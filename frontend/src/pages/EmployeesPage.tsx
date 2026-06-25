import { useState } from "react";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { DataGrid, type GridColDef, type GridPaginationModel } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import { employeeApi } from "../api/resources";
import type { Employee } from "../api/types";

const EMPTY: Employee = {
  employeeNumber: "",
  firstName: "",
  lastName: "",
  hireDate: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
};

export default function EmployeesPage() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const { data, isLoading } = useQuery({
    queryKey: ["employees", pagination.page, pagination.pageSize],
    queryFn: () => employeeApi.list(pagination.page, pagination.pageSize),
  });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Employee>(EMPTY);

  const save = useMutation({
    mutationFn: (e: Employee) => (e.id ? employeeApi.update(e.id, e) : employeeApi.create(e)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["employees"] });
      setOpen(false);
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

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Employees</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setForm(EMPTY); setOpen(true); }}>
          New Employee
        </Button>
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
          onRowClick={(p) => { setForm(p.row as Employee); setOpen(true); }}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>{form.id ? "Edit Employee" : "New Employee"}</DialogTitle>
        <DialogContent>
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
              <TextField fullWidth label="Nationality (ISO-2)" value={form.nationalityCountryCode ?? ""}
                onChange={(e) => set("nationalityCountryCode", e.target.value.toUpperCase())} inputProps={{ maxLength: 2 }} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth label="Date of Birth" type="date" InputLabelProps={{ shrink: true }}
                value={form.dateOfBirth ?? ""} onChange={(e) => set("dateOfBirth", e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField fullWidth label="Gender" value={form.gender ?? ""} onChange={(e) => set("gender", e.target.value)} />
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
              <TextField fullWidth label="Status" value={form.status ?? "ACTIVE"} onChange={(e) => set("status", e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Email" value={form.email ?? ""} onChange={(e) => set("email", e.target.value)} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField fullWidth label="Phone" value={form.phone ?? ""} onChange={(e) => set("phone", e.target.value)} />
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
