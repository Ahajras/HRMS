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
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { DataGrid, type GridColDef } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import { orgUnitTypeApi } from "../api/resources";
import type { OrgUnitType } from "../api/types";

const EMPTY: OrgUnitType = { code: "", name: "", levelOrder: 1, mandatory: false, status: "ACTIVE" };

export default function OrgUnitTypesPage() {
  const qc = useQueryClient();
  const { data = [], isLoading } = useQuery({ queryKey: ["orgUnitTypes"], queryFn: orgUnitTypeApi.list });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<OrgUnitType>(EMPTY);

  const save = useMutation({
    mutationFn: (t: OrgUnitType) => (t.id ? orgUnitTypeApi.update(t.id, t) : orgUnitTypeApi.create(t)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["orgUnitTypes"] });
      setOpen(false);
    },
  });

  const columns: GridColDef<OrgUnitType>[] = [
    { field: "levelOrder", headerName: "Level", width: 90 },
    { field: "code", headerName: "Code", width: 180 },
    { field: "name", headerName: "Name", flex: 1 },
    { field: "mandatory", headerName: "Mandatory", width: 130, type: "boolean" },
    { field: "status", headerName: "Status", width: 120 },
  ];

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Organization Levels</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setForm(EMPTY); setOpen(true); }}>
          New Level
        </Button>
      </Stack>

      <Typography variant="body2" color="text.secondary" mb={2}>
        The organisation hierarchy is a configurable set of levels (FTDD Vol.2 Ch.32.7), not fixed named tiers.
      </Typography>

      <div style={{ height: 520, width: "100%" }}>
        <DataGrid
          rows={data}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => r.id ?? r.code}
          onRowClick={(p) => { setForm(p.row as OrgUnitType); setOpen(true); }}
          pageSizeOptions={[10, 25, 50]}
          initialState={{
            pagination: { paginationModel: { pageSize: 25 } },
            sorting: { sortModel: [{ field: "levelOrder", sort: "asc" }] },
          }}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{form.id ? "Edit Level" : "New Level"}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
            <TextField label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <TextField label="Level Order" type="number" value={form.levelOrder}
              onChange={(e) => setForm({ ...form, levelOrder: Number(e.target.value) })} />
            <FormControlLabel
              control={<Checkbox checked={form.mandatory} onChange={(e) => setForm({ ...form, mandatory: e.target.checked })} />}
              label="Mandatory"
            />
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
