import { useState } from "react";
import {
  Autocomplete,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { DataGrid, type GridColDef } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import { roleApi } from "../api/auth";
import type { Role } from "../api/types";

const EMPTY: Role = { code: "", name: "", description: "", permissions: [] };

export default function RolesPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["roles"], queryFn: roleApi.list });
  const { data: allPermissions = [] } = useQuery({
    queryKey: ["permissions"],
    queryFn: roleApi.permissions,
  });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Role>(EMPTY);

  const save = useMutation({
    mutationFn: (r: Role) => (r.id ? roleApi.update(r.id, r) : roleApi.create(r)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["roles"] });
      setOpen(false);
    },
  });

  const columns: GridColDef<Role>[] = [
    { field: "code", headerName: "Code", width: 200 },
    { field: "name", headerName: "Name", width: 220 },
    { field: "description", headerName: "Description", flex: 1 },
    {
      field: "permissions",
      headerName: "Permissions",
      width: 130,
      valueGetter: (_v, row) => row.permissions?.length ?? 0,
    },
    {
      field: "companyId",
      headerName: "Scope",
      width: 120,
      valueGetter: (_v, row) => (row.companyId ? "Company" : "Global"),
    },
  ];

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Roles &amp; Permissions</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setForm(EMPTY); setOpen(true); }}>
          New Role
        </Button>
      </Stack>

      <div style={{ height: 520, width: "100%" }}>
        <DataGrid
          rows={data}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => r.id ?? r.code}
          onRowClick={(p) => { setForm(p.row as Role); setOpen(true); }}
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{form.id ? "Edit Role" : "New Role"}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Code" value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
            <TextField label="Name" value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Description" value={form.description ?? ""}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
            <Autocomplete
              multiple
              options={allPermissions}
              value={form.permissions ?? []}
              onChange={(_e, value) => setForm({ ...form, permissions: value })}
              renderInput={(params) => <TextField {...params} label="Permissions" placeholder="Add permission" />}
            />
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
