import { useState } from "react";
import {
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
import { countryApi } from "../api/resources";
import type { Country } from "../api/types";

const EMPTY: Country = { code: "", name: "", defaultCurrencyCode: "", status: "ACTIVE" };

export default function CountriesPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["countries"], queryFn: countryApi.list });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Country>(EMPTY);

  const save = useMutation({
    mutationFn: (c: Country) => (c.id ? countryApi.update(c.id, c) : countryApi.create(c)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["countries"] });
      setOpen(false);
    },
  });

  const columns: GridColDef<Country>[] = [
    { field: "code", headerName: "Code", width: 90 },
    { field: "name", headerName: "Name", flex: 1 },
    { field: "defaultCurrencyCode", headerName: "Default Currency", width: 160 },
    { field: "status", headerName: "Status", width: 120 },
  ];

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Countries</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setForm(EMPTY); setOpen(true); }}>
          New Country
        </Button>
      </Stack>

      <div style={{ height: 560, width: "100%" }}>
        <DataGrid
          rows={data}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => r.id ?? r.code}
          onRowClick={(p) => { setForm(p.row as Country); setOpen(true); }}
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{form.id ? "Edit Country" : "New Country"}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Code (ISO alpha-2)" value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} inputProps={{ maxLength: 2 }} />
            <TextField label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Default Currency Code" value={form.defaultCurrencyCode ?? ""}
              onChange={(e) => setForm({ ...form, defaultCurrencyCode: e.target.value.toUpperCase() })} inputProps={{ maxLength: 3 }} />
            <TextField label="Status" value={form.status ?? "ACTIVE"} onChange={(e) => setForm({ ...form, status: e.target.value })} />
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
