import { useState } from "react";
import {
  Box,
  Button,
  Chip,
  FormControlLabel,
  Grid,
  IconButton,
  Paper,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import { overtimeCategoryApi } from "../api/resources";
import type { OvertimeCategory } from "../api/types";

const EMPTY: OvertimeCategory = { code: "", name: "", otEligible: true };

export default function OvertimeCategoriesPage() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["overtimeCategories"], queryFn: overtimeCategoryApi.list });
  const [form, setForm] = useState<OvertimeCategory>(EMPTY);

  const save = useMutation({
    mutationFn: (c: OvertimeCategory) => (c.id ? overtimeCategoryApi.update(c.id, c) : overtimeCategoryApi.create(c)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["overtimeCategories"] }); setForm(EMPTY); },
  });
  const del = useMutation({
    mutationFn: (id: string) => overtimeCategoryApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["overtimeCategories"] }),
  });

  return (
    <Box>
      <Typography variant="h5" mb={0.5}>Overtime Categories</Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Assign a category to each employee. If a category is not eligible, overtime worked is not counted.
      </Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit category" : "Add category"}</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={2}><TextField fullWidth size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} /></Grid>
          <Grid item xs={12} sm={5}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={12} sm={3}>
            <FormControlLabel
              control={<Switch checked={form.otEligible} onChange={(e) => setForm({ ...form, otEligible: e.target.checked })} />}
              label="OT eligible"
            />
          </Grid>
          <Grid item xs={12} sm={2}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm(EMPTY)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      {data.map((c) => (
        <Paper key={c.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>{c.code} — {c.name}</Typography>
              <Chip size="small" color={c.otEligible ? "success" : "warning"}
                label={c.otEligible ? "OT eligible" : "No overtime"} sx={{ mt: 0.5 }} />
            </Box>
            <Box>
              <Button size="small" onClick={() => setForm(c)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => c.id && del.mutate(c.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No categories yet. Add one above.</Typography>}
    </Box>
  );
}
