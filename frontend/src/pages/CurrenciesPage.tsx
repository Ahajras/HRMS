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
import { currencyApi } from "../api/resources";
import type { Currency } from "../api/types";

const EMPTY: Currency = { code: "", name: "", symbol: "", minorUnits: 2, status: "ACTIVE" };

export default function CurrenciesPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["currencies"], queryFn: currencyApi.list });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Currency>(EMPTY);

  const save = useMutation({
    mutationFn: (c: Currency) => (c.id ? currencyApi.update(c.id, c) : currencyApi.create(c)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["currencies"] });
      setOpen(false);
    },
  });

  const columns: GridColDef<Currency>[] = [
    { field: "code", headerName: "Code", width: 90 },
    { field: "name", headerName: "Name", flex: 1 },
    { field: "symbol", headerName: "Symbol", width: 100 },
    { field: "minorUnits", headerName: "Minor Units", width: 130 },
    { field: "status", headerName: "Status", width: 120 },
  ];

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Currencies</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setForm(EMPTY); setOpen(true); }}>
          New Currency
        </Button>
      </Stack>

      <div style={{ height: 560, width: "100%" }}>
        <DataGrid
          rows={data}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => r.id ?? r.code}
          onRowClick={(p) => { setForm(p.row as Currency); setOpen(true); }}
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{form.id ? "Edit Currency" : "New Currency"}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Code (ISO 4217)" value={form.code}
              onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} inputProps={{ maxLength: 3 }} />
            <TextField label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Symbol" value={form.symbol ?? ""} onChange={(e) => setForm({ ...form, symbol: e.target.value })} />
            <TextField label="Minor Units" type="number" value={form.minorUnits}
              onChange={(e) => setForm({ ...form, minorUnits: Number(e.target.value) })} />
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
