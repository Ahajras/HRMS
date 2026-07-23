import { useState } from "react";
import {
  Box,
  Button,
  Chip,
  FormControlLabel,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import { costCodeApi, projectApi, sponsorApi } from "../api/resources";
import type { CostCode, Project } from "../api/types";

const emptyCostCode = (projectId: string): CostCode => ({
  projectId,
  code: "",
  name: "",
  description: "",
  currencyCode: "QAR",
  active: true,
  status: "ACTIVE",
});

const requiredFieldSx = {
  "& .MuiOutlinedInput-notchedOutline": { borderColor: "primary.light" },
  "& .MuiInputLabel-root": { color: "primary.main", fontWeight: 700 },
};

function CostCodesPanel({ projectId }: { projectId: string }) {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["costCodes", projectId], queryFn: () => costCodeApi.byProject(projectId) });
  const [form, setForm] = useState<CostCode>(emptyCostCode(projectId));

  const save = useMutation({
    mutationFn: (c: CostCode) => {
      const description = (c.description || c.name || "").trim();
      const payload: CostCode = {
        ...c,
        code: c.code.trim().toUpperCase(),
        name: description,
        description,
        currencyCode: (c.currencyCode || "QAR").trim().toUpperCase(),
        status: c.active === false ? "INACTIVE" : "ACTIVE",
      };
      return payload.id ? costCodeApi.update(payload.id, payload) : costCodeApi.create(payload);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["costCodes", projectId] });
      setForm(emptyCostCode(projectId));
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => costCodeApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["costCodes", projectId] }),
  });

  const sorted = [...data].sort((a, b) => a.code.localeCompare(b.code));
  const canSave = Boolean(form.code?.trim() && (form.description || form.name)?.trim());

  return (
    <Box sx={{ mt: 1.5 }}>
      <Grid container spacing={1.5}>
        <Grid item xs={12} md={7}>
          <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
            <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
              <Box>
                <Typography variant="subtitle2">Cost codes</Typography>
                <Typography variant="caption" color="text.secondary">Legacy chart: account code, currency, description, and active flag.</Typography>
              </Box>
              <Chip size="small" label={`${sorted.filter((c) => c.active !== false).length} active`} color="primary" variant="outlined" />
            </Stack>
            {sorted.length === 0 && <Typography variant="body2" color="text.secondary">No cost codes yet.</Typography>}
            {sorted.map((c) => (
              <Stack key={c.id} direction="row" alignItems="center" justifyContent="space-between" sx={{ py: 1, borderTop: 1, borderColor: "divider" }}>
                <Box>
                  <Stack direction="row" alignItems="center" spacing={1}>
                    <Typography variant="body2" fontWeight={800}>{c.code}</Typography>
                    <Chip size="small" label={c.currencyCode || "QAR"} variant="outlined" />
                    <Chip size="small" label={c.active === false ? "Inactive" : "Active"} color={c.active === false ? "default" : "success"} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">{c.description || c.name}</Typography>
                </Box>
                <Box>
                  <Button size="small" onClick={() => setForm({ ...c, description: c.description || c.name, active: c.active !== false })}>Edit</Button>
                  <IconButton size="small" color="error" onClick={() => c.id && del.mutate(c.id)}><DeleteIcon fontSize="small" /></IconButton>
                </Box>
              </Stack>
            ))}
          </Paper>
        </Grid>
        <Grid item xs={12} md={5}>
          <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
            <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit cost code" : "Add cost code"}</Typography>
            <Stack spacing={1.5}>
              <TextField size="small" label="Account code *" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} sx={requiredFieldSx} />
              <TextField size="small" label="Description *" value={form.description || form.name || ""} onChange={(e) => setForm({ ...form, description: e.target.value, name: e.target.value })} sx={requiredFieldSx} />
              <TextField size="small" label="Currency code" value={form.currencyCode || "QAR"} onChange={(e) => setForm({ ...form, currencyCode: e.target.value })} inputProps={{ maxLength: 10 }} />
              <FormControlLabel control={<Switch checked={form.active !== false} onChange={(e) => setForm({ ...form, active: e.target.checked, status: e.target.checked ? "ACTIVE" : "INACTIVE" })} />} label="Active" />
              <Stack direction="row" spacing={1}>
                <Button variant="contained" size="small" disabled={!canSave || save.isPending} onClick={() => save.mutate(form)}>
                  {form.id ? "Update" : "Add"}
                </Button>
                {form.id && <Button size="small" onClick={() => setForm(emptyCostCode(projectId))}>Cancel</Button>}
              </Stack>
            </Stack>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}

export default function ProjectsPage() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const [form, setForm] = useState<Project>({ code: "", name: "" });
  const [open, setOpen] = useState<string | null>(null);
  const { data: sponsors = [] } = useQuery({ queryKey: ["sponsors"], queryFn: sponsorApi.list });

  const save = useMutation({
    mutationFn: (p: Project) => (p.id ? projectApi.update(p.id, p) : projectApi.create(p)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["projects"] }); setForm({ code: "", name: "" }); },
  });
  const del = useMutation({
    mutationFn: (id: string) => projectApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["projects"] }),
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Projects</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit project" : "Add project"}</Typography>
        <Grid container spacing={1.5}>
          <Grid item xs={12} sm={3}><TextField fullWidth size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} /></Grid>
          <Grid item xs={12} sm={4}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={12} sm={2}>
            <TextField fullWidth select size="small" label="Sponsor (WPS)" value={form.sponsorId ?? ""} onChange={(e) => setForm({ ...form, sponsorId: e.target.value || undefined })}>
              <MenuItem value="">(none)</MenuItem>
              {sponsors.map((s) => <MenuItem key={s.id} value={s.id}>{s.code}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={3}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm({ code: "", name: "" })}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      {data.map((p) => (
        <Paper key={p.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1.5 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>{p.code} - {p.name}</Typography>
              <Typography variant="caption" color="text.secondary">{p.status}</Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => setOpen(open === p.id ? null : (p.id ?? null))}>
                {open === p.id ? "Hide cost codes" : "Cost codes"}
              </Button>
              <Button size="small" onClick={() => setForm(p)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => p.id && del.mutate(p.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
          {open === p.id && p.id && <CostCodesPanel projectId={p.id} />}
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No projects yet. Add one above.</Typography>}
    </Box>
  );
}
