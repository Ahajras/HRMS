import { useState } from "react";
import {
  Box,
  Button,
  Grid,
  IconButton,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import { costCodeApi, projectApi } from "../api/resources";
import type { CostCode, Project } from "../api/types";

function CostCodesPanel({ projectId }: { projectId: string }) {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["costCodes", projectId], queryFn: () => costCodeApi.byProject(projectId) });
  const [form, setForm] = useState<CostCode>({ projectId, code: "", name: "" });

  const save = useMutation({
    mutationFn: (c: CostCode) => (c.id ? costCodeApi.update(c.id, c) : costCodeApi.create(c)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["costCodes", projectId] }); setForm({ projectId, code: "", name: "" }); },
  });
  const del = useMutation({
    mutationFn: (id: string) => costCodeApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["costCodes", projectId] }),
  });

  return (
    <Box sx={{ mt: 1.5, p: 1.5, bgcolor: "action.hover", borderRadius: 1 }}>
      <Typography variant="subtitle2" gutterBottom>Cost codes</Typography>
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No cost codes yet.</Typography>}
      {data.map((c) => (
        <Stack key={c.id} direction="row" alignItems="center" justifyContent="space-between" sx={{ py: 0.5, borderTop: 1, borderColor: "divider" }}>
          <Typography variant="body2"><b>{c.code}</b> — {c.name}</Typography>
          <Box>
            <Button size="small" onClick={() => setForm(c)}>Edit</Button>
            <IconButton size="small" color="error" onClick={() => c.id && del.mutate(c.id)}><DeleteIcon fontSize="small" /></IconButton>
          </Box>
        </Stack>
      ))}
      <Stack direction="row" spacing={1.5} sx={{ mt: 1.5 }}>
        <TextField size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} sx={{ width: 140 }} />
        <TextField size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} sx={{ flex: 1 }} />
        <Button variant="contained" size="small" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
          {form.id ? "Update" : "Add"}
        </Button>
        {form.id && <Button size="small" onClick={() => setForm({ projectId, code: "", name: "" })}>Cancel</Button>}
      </Stack>
    </Box>
  );
}

export default function ProjectsPage() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const [form, setForm] = useState<Project>({ code: "", name: "" });
  const [open, setOpen] = useState<string | null>(null);

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
          <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
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
              <Typography fontWeight={600}>{p.code} — {p.name}</Typography>
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
