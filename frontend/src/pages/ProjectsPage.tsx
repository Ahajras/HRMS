import { useState } from "react";
import {
  Alert,
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
  Tooltip,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import { costCodeApi, projectApi, sponsorApi } from "../api/resources";
import type { CostCode, Project } from "../api/types";

const emptyProject: Project = { code: "", name: "", status: "ACTIVE" };

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

const errorMessage = (error: unknown, fallback: string) =>
  (error as any)?.response?.data?.message ?? fallback;

function CostCodesPanel({ project }: { project: Project }) {
  const projectId = project.id!;
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["costCodes", projectId], queryFn: () => costCodeApi.byProject(projectId) });
  const [form, setForm] = useState<CostCode>(emptyCostCode(projectId));

  const save = useMutation({
    mutationFn: (c: CostCode) => {
      const description = (c.description || c.name || "").trim();
      const payload: CostCode = {
        ...c,
        projectId,
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
          <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, bgcolor: "background.paper" }}>
            <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1}>
              <Box>
                <Typography variant="subtitle2">Cost code chart</Typography>
                <Typography variant="caption" color="text.secondary">
                  prjcode, accode, curcod, ldesc, and lactive from the legacy chart.
                </Typography>
              </Box>
              <Chip size="small" label={`${sorted.filter((c) => c.active !== false).length} active`} color="primary" variant="outlined" />
            </Stack>
            {sorted.length === 0 && <Typography variant="body2" color="text.secondary">No cost codes yet.</Typography>}
            {sorted.map((c) => (
              <Stack key={c.id} direction="row" alignItems="center" justifyContent="space-between" sx={{ py: 1, borderTop: 1, borderColor: "divider" }}>
                <Box>
                  <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap">
                    <Chip size="small" label={`PRJ ${project.code}`} variant="outlined" />
                    <Typography variant="body2" fontWeight={800}>{c.code}</Typography>
                    <Chip size="small" label={c.currencyCode || "QAR"} variant="outlined" />
                    <Chip size="small" label={c.active === false ? "Inactive" : "Active"} color={c.active === false ? "default" : "success"} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>{c.description || c.name}</Typography>
                </Box>
                <Box>
                  <Button size="small" onClick={() => setForm({ ...c, description: c.description || c.name, active: c.active !== false })}>Edit</Button>
                  <IconButton size="small" color="error" onClick={() => c.id && del.mutate(c.id)}><DeleteIcon fontSize="small" /></IconButton>
                </Box>
              </Stack>
            ))}
            {del.isError && <Alert severity="error" sx={{ mt: 1 }}>{errorMessage(del.error, "Could not delete cost code.")}</Alert>}
          </Paper>
        </Grid>
        <Grid item xs={12} md={5}>
          <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, bgcolor: "background.paper" }}>
            <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit cost code" : "Add cost code"}</Typography>
            <Stack spacing={1.5}>
              <TextField size="small" label="Project code (prjcode)" value={project.code} InputProps={{ readOnly: true }} />
              <TextField size="small" label="Account code (accode) *" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} sx={requiredFieldSx} inputProps={{ maxLength: 13 }} />
              <TextField size="small" label="Description (ldesc) *" value={form.description || form.name || ""} onChange={(e) => setForm({ ...form, description: e.target.value, name: e.target.value })} sx={requiredFieldSx} inputProps={{ maxLength: 40 }} />
              <TextField size="small" label="Currency code (curcod)" value={form.currencyCode || "QAR"} onChange={(e) => setForm({ ...form, currencyCode: e.target.value })} inputProps={{ maxLength: 10 }} />
              <FormControlLabel control={<Switch checked={form.active !== false} onChange={(e) => setForm({ ...form, active: e.target.checked, status: e.target.checked ? "ACTIVE" : "INACTIVE" })} />} label="Active (lactive)" />
              <Stack direction="row" spacing={1}>
                <Button variant="contained" size="small" disabled={!canSave || save.isPending} onClick={() => save.mutate(form)}>
                  {form.id ? "Update" : "Add"}
                </Button>
                {form.id && <Button size="small" onClick={() => setForm(emptyCostCode(projectId))}>Cancel</Button>}
              </Stack>
              {save.isError && <Alert severity="error">{errorMessage(save.error, "Could not save cost code.")}</Alert>}
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
  const [form, setForm] = useState<Project>(emptyProject);
  const [open, setOpen] = useState<string | null>(null);
  const { data: sponsors = [] } = useQuery({ queryKey: ["sponsors"], queryFn: sponsorApi.list });

  const save = useMutation({
    mutationFn: (p: Project) => {
      const payload: Project = { ...p, code: p.code.trim().toUpperCase(), status: p.status ?? "ACTIVE" };
      return payload.id ? projectApi.update(payload.id, payload) : projectApi.create(payload);
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["projects"] }); setForm(emptyProject); },
  });
  const del = useMutation({
    mutationFn: (id: string) => projectApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["projects"] }),
  });

  const activeCount = data.filter((p) => p.status !== "INACTIVE").length;

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} alignItems={{ xs: "stretch", md: "center" }} justifyContent="space-between" gap={1.5} mb={2}>
        <Box>
          <Typography variant="h5" fontWeight={800}>Projects</Typography>
          <Typography variant="body2" color="text.secondary">Create projects, maintain their WPS sponsor, and manage the legacy cost code chart.</Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <Chip label={`${data.length} projects`} />
          <Chip color="success" label={`${activeCount} active`} />
        </Stack>
      </Stack>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Stack direction="row" alignItems="center" justifyContent="space-between" mb={1.5}>
          <Box>
            <Typography variant="subtitle1" fontWeight={800}>{form.id ? "Edit project" : "Add project"}</Typography>
            <Typography variant="caption" color="text.secondary">Required fields are highlighted. Use Inactive instead of delete when project has history.</Typography>
          </Box>
          <FormControlLabel control={<Switch checked={form.status !== "INACTIVE"} onChange={(e) => setForm({ ...form, status: e.target.checked ? "ACTIVE" : "INACTIVE" })} />} label={form.status === "INACTIVE" ? "Inactive" : "Active"} />
        </Stack>
        <Grid container spacing={1.5}>
          <Grid item xs={12} sm={3}>
            <TextField fullWidth size="small" label="Project code *" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} sx={requiredFieldSx} inputProps={{ maxLength: 20 }} />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField fullWidth size="small" label="Project name *" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} sx={requiredFieldSx} />
          </Grid>
          <Grid item xs={12} sm={3}>
            <TextField fullWidth select size="small" label="Sponsor (WPS)" value={form.sponsorId ?? ""} onChange={(e) => setForm({ ...form, sponsorId: e.target.value || undefined })}>
              <MenuItem value="">(none)</MenuItem>
              {sponsors.map((s) => <MenuItem key={s.id} value={s.id}>{s.code}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={2}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm(emptyProject)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
        {save.isError && <Alert severity="error" sx={{ mt: 1.5 }}>{errorMessage(save.error, "Could not save project.")}</Alert>}
      </Paper>

      <Grid container spacing={1.5}>
        {data.map((p) => (
          <Grid item xs={12} key={p.id}>
            <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
              <Stack direction={{ xs: "column", md: "row" }} alignItems={{ xs: "stretch", md: "center" }} justifyContent="space-between" gap={1}>
                <Box>
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                    <Typography fontWeight={800}>{p.code}</Typography>
                    <Typography color="text.secondary">- {p.name}</Typography>
                    <Chip size="small" label={p.status ?? "ACTIVE"} color={p.status === "INACTIVE" ? "default" : "success"} />
                  </Stack>
                  <Typography variant="caption" color="text.secondary">Cost codes are managed under this project and keep the project code as prjcode.</Typography>
                </Box>
                <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                  <Button size="small" variant={open === p.id ? "contained" : "outlined"} onClick={() => setOpen(open === p.id ? null : (p.id ?? null))}>
                    {open === p.id ? "Hide cost codes" : "Cost codes"}
                  </Button>
                  <Button size="small" onClick={() => setForm({ ...p, status: p.status ?? "ACTIVE" })}>Edit</Button>
                  <Tooltip title="Delete works only when no assignments, cost codes, timesheets, payroll, shifts, or approvals reference this project. Otherwise set it Inactive.">
                    <span>
                      <IconButton size="small" color="error" onClick={() => p.id && window.confirm("Delete this project? This works only if no related data exists.") && del.mutate(p.id)}>
                        <DeleteIcon />
                      </IconButton>
                    </span>
                  </Tooltip>
                </Stack>
              </Stack>
              {open === p.id && p.id && <CostCodesPanel project={p} />}
            </Paper>
          </Grid>
        ))}
      </Grid>
      {del.isError && <Alert severity="error" sx={{ mt: 1.5 }}>{errorMessage(del.error, "Could not delete project.")}</Alert>}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No projects yet. Add one above.</Typography>}
    </Box>
  );
}
