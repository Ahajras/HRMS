import { useState } from "react";
import {
  Autocomplete,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { DataGrid, type GridColDef } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import { roleApi, userApi } from "../api/auth";
import type { AuthUser, UserPayload } from "../api/types";

const EMPTY: UserPayload = {
  username: "",
  email: "",
  password: "",
  fullName: "",
  status: "ACTIVE",
  roles: [],
};

const STATUSES = ["ACTIVE", "DISABLED", "LOCKED"];

export default function UsersPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["users"], queryFn: userApi.list });
  const { data: roles = [] } = useQuery({ queryKey: ["roles"], queryFn: roleApi.list });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<UserPayload>(EMPTY);
  const editing = !!form.id;

  const save = useMutation({
    mutationFn: (u: UserPayload) => (u.id ? userApi.update(u.id, u) : userApi.create(u)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["users"] });
      setOpen(false);
    },
  });

  const openEdit = (u: AuthUser) => {
    setForm({
      id: u.id,
      companyId: u.companyId,
      employeeId: u.employeeId,
      username: u.username,
      email: u.email,
      password: "",
      fullName: u.fullName,
      status: u.status ?? "ACTIVE",
      roles: u.roles ?? [],
    });
    setOpen(true);
  };

  const columns: GridColDef<AuthUser>[] = [
    { field: "username", headerName: "Username", width: 180 },
    { field: "fullName", headerName: "Full name", flex: 1 },
    { field: "email", headerName: "Email", width: 220 },
    {
      field: "roles",
      headerName: "Roles",
      width: 200,
      valueGetter: (_v, row) => (row.roles ?? []).join(", "),
    },
    { field: "status", headerName: "Status", width: 120 },
  ];

  const roleCodes = roles.map((r) => r.code);

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Users</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setForm(EMPTY); setOpen(true); }}>
          New User
        </Button>
      </Stack>

      <div style={{ height: 520, width: "100%" }}>
        <DataGrid
          rows={data}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => r.id ?? r.username}
          onRowClick={(p) => openEdit(p.row as AuthUser)}
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? "Edit User" : "New User"}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Username" value={form.username} disabled={editing}
              onChange={(e) => setForm({ ...form, username: e.target.value })} />
            <TextField label="Full name" value={form.fullName ?? ""}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            <TextField label="Email" value={form.email ?? ""}
              onChange={(e) => setForm({ ...form, email: e.target.value })} />
            <TextField
              label={editing ? "New password (leave blank to keep)" : "Password"}
              type="password"
              value={form.password ?? ""}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
            <TextField label="Company ID (blank = platform admin)" value={form.companyId ?? ""}
              onChange={(e) => setForm({ ...form, companyId: e.target.value || undefined })} />
            <Autocomplete
              multiple
              options={roleCodes}
              value={form.roles ?? []}
              onChange={(_e, value) => setForm({ ...form, roles: value })}
              renderInput={(params) => <TextField {...params} label="Roles" placeholder="Assign role" />}
            />
            <TextField select label="Status" value={form.status ?? "ACTIVE"}
              onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => save.mutate(form)} disabled={save.isPending}>Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
