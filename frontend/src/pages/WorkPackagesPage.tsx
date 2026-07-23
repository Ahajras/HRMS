import { useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Grid,
  IconButton,
  InputAdornment,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import InventoryIcon from "@mui/icons-material/Inventory2Outlined";
import SearchIcon from "@mui/icons-material/Search";
import { costCodeApi, crewApi, lookupApi, projectApi, workPackageApi } from "../api/resources";
import type { CostCode, WorkPackage } from "../api/types";

const EMPTY: WorkPackage = { projectId: "", code: "", name: "", status: "PLANNED" };
const today = () => new Date().toISOString().slice(0, 10);

export default function WorkPackagesPage() {
  const qc = useQueryClient();
  const [projectFilter, setProjectFilter] = useState("");
  const [q, setQ] = useState("");
  const [form, setForm] = useState<WorkPackage>(EMPTY);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: packages = [] } = useQuery({
    queryKey: ["workPackages", projectFilter],
    queryFn: () => workPackageApi.list(projectFilter || undefined),
  });
  const { data: costCodes = [] } = useQuery({
    queryKey: ["costCodes", form.projectId],
    queryFn: () => form.projectId ? costCodeApi.byProject(form.projectId) : Promise.resolve([]),
  });

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    return packages.filter((wp) => !term || `${wp.code} ${wp.name} ${wp.projectCode ?? ""} ${wp.costCode ?? ""}`.toLowerCase().includes(term));
  }, [packages, q]);

  const save = useMutation({
    mutationFn: (payload: WorkPackage) => payload.id ? workPackageApi.update(payload.id, payload) : workPackageApi.create(payload),
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ["workPackages"] });
      setSelectedId(saved.id ?? null);
      setForm(EMPTY);
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => workPackageApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workPackages"] });
      setSelectedId(null);
    },
  });

  const previewCode = () => {
    if (form.id) return form.code || "-";
    const raw = (form.code || "").trim().toUpperCase().replace(/\s+/g, "-").replace(/-$/, "");
    if (!raw) return "WP-PREFIX-001";
    const prefix = raw.startsWith("WP-") ? raw : `WP-${raw}`;
    const next = Math.max(
      0,
      ...packages
        .filter((wp) => wp.projectId === form.projectId && wp.code?.startsWith(`${prefix}-`))
        .map((wp) => Number(wp.code?.slice(prefix.length + 1)) || 0),
    ) + 1;
    return /-\d{3,}$/.test(prefix) ? prefix : `${prefix}-${String(next).padStart(3, "0")}`;
  };

  const projectLabel = (id?: string) => {
    const p = projects.find((project) => project.id === id);
    return p ? `${p.code} - ${p.name}` : "-";
  };
  const costLabel = (c: CostCode) =>
    `${c.prjcode ? `${c.prjcode} / ` : ""}${c.code} - ${c.description || c.name}${c.currencyCode ? ` - ${c.currencyCode}` : ""}`;

  return (
    <Box>
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", md: "center" }} spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5" fontWeight={900}>Work Packages</Typography>
          <Typography variant="body2" color="text.secondary">
            Plan the work scope, required manpower, and crews assigned to each package.
          </Typography>
        </Box>
        <Chip icon={<InventoryIcon />} color="primary" variant="outlined" label={`${packages.length} package(s)`} />
      </Stack>

      <Grid container spacing={2} alignItems="flex-start">
        <Grid item xs={12} lg={6}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} mb={2}>
              <TextField select fullWidth size="small" label="Project" value={projectFilter} onChange={(e) => setProjectFilter(e.target.value)}>
                <MenuItem value="">All projects</MenuItem>
                {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
              </TextField>
              <TextField
                fullWidth
                size="small"
                placeholder="Search package, project, or cost code..."
                value={q}
                onChange={(e) => setQ(e.target.value)}
                InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
              />
            </Stack>

            <Stack spacing={1.5}>
              {filtered.map((wp) => (
                <Paper key={wp.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, bgcolor: selectedId === wp.id ? "primary.50" : "background.paper" }}>
                  <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1.5}>
                    <Box sx={{ minWidth: 0 }}>
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                        <Typography fontWeight={900}>{wp.code}</Typography>
                        <Typography fontWeight={700}>{wp.name}</Typography>
                        <Chip size="small" label={wp.status ?? "PLANNED"} color={(wp.status ?? "PLANNED") === "ACTIVE" ? "success" : "default"} />
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {wp.projectCode ?? projectLabel(wp.projectId)} - Cost: {wp.costCode ?? "-"}
                      </Typography>
                      <Stack direction="row" spacing={1} mt={1} flexWrap="wrap" useFlexGap>
                        <Chip size="small" variant="outlined" label={`${wp.requirementCount ?? 0} requirement(s)`} />
                        <Chip size="small" variant="outlined" label={`${wp.crewCount ?? 0} crew(s)`} />
                        {(wp.plannedStart || wp.plannedEnd) && <Chip size="small" variant="outlined" label={`${wp.plannedStart ?? "-"} -> ${wp.plannedEnd ?? "-"}`} />}
                      </Stack>
                    </Box>
                    <Stack direction="row" spacing={1} justifyContent="flex-end" flexWrap="wrap" useFlexGap>
                      <Button size="small" onClick={() => setSelectedId(selectedId === wp.id ? null : (wp.id ?? null))}>Details</Button>
                      <Button size="small" onClick={() => setForm(wp)}>Edit</Button>
                      <IconButton size="small" color="error" onClick={() => wp.id && remove.mutate(wp.id)}><DeleteIcon /></IconButton>
                    </Stack>
                  </Stack>
                  {selectedId === wp.id && wp.id && <WorkPackageDetails packageId={wp.id} projectId={wp.projectId} />}
                </Paper>
              ))}
              {filtered.length === 0 && (
                <Paper variant="outlined" sx={{ p: 3, textAlign: "center", borderStyle: "dashed", borderRadius: 2 }}>
                  <Typography variant="body2" color="text.secondary">No work packages match the current filter.</Typography>
                </Paper>
              )}
            </Stack>
          </Paper>
        </Grid>

        <Grid item xs={12} lg={6}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 3, position: { lg: "sticky" }, top: 88 }}>
            <Typography variant="subtitle1" fontWeight={900} gutterBottom>{form.id ? "Edit work package" : "Create work package"}</Typography>
            <Alert severity="info" sx={{ mb: 2 }}>
              Enter a prefix like ENG. The system saves the next code as <b>{previewCode()}</b> per project.
            </Alert>
            <Grid container spacing={1.5}>
              <Grid item xs={12} md={6}>
                <TextField required select fullWidth size="small" label="Project" value={form.projectId} onChange={(e) => setForm({ ...form, projectId: e.target.value, costCodeId: undefined })}>
                  <MenuItem value="">Select project</MenuItem>
                  {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField required fullWidth size="small" label={form.id ? "Code" : "Code prefix"} value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField fullWidth size="small" label="Generated code" value={previewCode()} InputProps={{ readOnly: true }} />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField required fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </Grid>
              <Grid item xs={12}>
                <TextField select fullWidth size="small" label="Cost code" value={form.costCodeId ?? ""} onChange={(e) => setForm({ ...form, costCodeId: e.target.value || undefined })}>
                  <MenuItem value="">No cost code</MenuItem>
                  {costCodes.map((c) => <MenuItem key={c.id} value={c.id}>{costLabel(c)}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField fullWidth size="small" type="date" label="Planned start" InputLabelProps={{ shrink: true }} value={form.plannedStart ?? ""} onChange={(e) => setForm({ ...form, plannedStart: e.target.value || undefined })} />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField fullWidth size="small" type="date" label="Planned end" InputLabelProps={{ shrink: true }} value={form.plannedEnd ?? ""} onChange={(e) => setForm({ ...form, plannedEnd: e.target.value || undefined })} />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField select fullWidth size="small" label="Status" value={form.status ?? "PLANNED"} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  {["PLANNED", "ACTIVE", "ON_HOLD", "CLOSED"].map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField fullWidth multiline minRows={3} size="small" label="Description" value={form.description ?? ""} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </Grid>
              <Grid item xs={12}>
                <Stack direction="row" spacing={1}>
                  <Button startIcon={<AddIcon />} variant="contained" disabled={!form.projectId || !form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                    {form.id ? "Update" : "Create"}
                  </Button>
                  {form.id && <Button onClick={() => setForm(EMPTY)}>Cancel</Button>}
                </Stack>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}

function WorkPackageDetails({ packageId, projectId }: { packageId: string; projectId: string }) {
  return (
    <Box sx={{ mt: 1.5, p: 1.5, bgcolor: "action.hover", borderRadius: 2, minWidth: 0 }}>
      <Stack spacing={1.5} sx={{ minWidth: 0 }}>
        <RequirementsPanel packageId={packageId} />
        <CrewAssignmentsPanel packageId={packageId} projectId={projectId} />
      </Stack>
    </Box>
  );
}

function RequirementsPanel({ packageId }: { packageId: string }) {
  const qc = useQueryClient();
  const { data: rows = [] } = useQuery({ queryKey: ["workPackageRequirements", packageId], queryFn: () => workPackageApi.requirements(packageId) });
  const { data: jobTitles = [] } = useQuery({ queryKey: ["lookup", "JOB_TITLE"], queryFn: () => lookupApi.byCategory("JOB_TITLE") });
  const [jobTitleCode, setJobTitleCode] = useState("");
  const [requiredCount, setRequiredCount] = useState(1);
  const selected = jobTitles.find((j) => j.code === jobTitleCode);

  const add = useMutation({
    mutationFn: () => workPackageApi.addRequirement(packageId, {
      jobTitleCode,
      jobTitleName: selected?.label ?? jobTitleCode,
      requiredCount,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workPackageRequirements", packageId] });
      qc.invalidateQueries({ queryKey: ["workPackages"] });
      setJobTitleCode("");
      setRequiredCount(1);
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => workPackageApi.removeRequirement(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workPackageRequirements", packageId] });
      qc.invalidateQueries({ queryKey: ["workPackages"] });
    },
  });

  return (
    <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
      <Typography variant="subtitle2" fontWeight={900}>Required manpower</Typography>
      <Typography variant="caption" color="text.secondary">Choose from job titles. Assigned count reads from crews linked to this package.</Typography>
      <Grid container spacing={1} mt={0.5}>
        <Grid item xs={12} md={4}>
        <TextField fullWidth select size="small" label="Job title" value={jobTitleCode} onChange={(e) => setJobTitleCode(e.target.value)}>
          <MenuItem value="">Select job title</MenuItem>
          {jobTitles.map((j) => <MenuItem key={j.id ?? j.code} value={j.code}>{j.label}</MenuItem>)}
        </TextField>
        </Grid>
        <Grid item xs={6} md={3}>
          <TextField fullWidth size="small" label="Code" value={jobTitleCode || "-"} InputProps={{ readOnly: true }} />
        </Grid>
        <Grid item xs={6} md={2}>
          <TextField fullWidth size="small" type="number" label="Required" value={requiredCount} onChange={(e) => setRequiredCount(Number(e.target.value))} />
        </Grid>
        <Grid item xs={12} md={2}>
          <Button fullWidth variant="contained" disabled={!jobTitleCode || requiredCount < 1 || add.isPending} onClick={() => add.mutate()}>Add</Button>
        </Grid>
      </Grid>
      <Divider sx={{ my: 1.5 }} />
      <Box sx={{ overflowX: "auto" }}>
        <Table size="small" sx={{ minWidth: 420 }}>
          <TableHead>
            <TableRow>
              <TableCell>Job title</TableCell>
              <TableCell width={90}>Required</TableCell>
              <TableCell width={90}>Assigned</TableCell>
              <TableCell width={52} align="right" />
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r) => {
              const ok = (r.assignedCount ?? 0) >= r.requiredCount;
              return (
                <TableRow key={r.id} sx={{ bgcolor: ok ? "#e8f5e9" : "#fff7ed" }}>
                  <TableCell sx={{ whiteSpace: "normal", wordBreak: "break-word" }}>{r.jobTitleName ?? r.jobTitleCode}<br /><Typography variant="caption" color="text.secondary">{r.jobTitleCode}</Typography></TableCell>
                  <TableCell>{r.requiredCount}</TableCell>
                  <TableCell sx={{ fontWeight: 800, color: ok ? "success.main" : "warning.dark" }}>{r.assignedCount ?? 0}</TableCell>
                  <TableCell align="right"><IconButton size="small" color="error" onClick={() => r.id && remove.mutate(r.id)}><DeleteIcon fontSize="small" /></IconButton></TableCell>
                </TableRow>
              );
            })}
            {rows.length === 0 && <TableRow><TableCell colSpan={4}><Typography variant="body2" color="text.secondary">No requirements yet.</Typography></TableCell></TableRow>}
          </TableBody>
        </Table>
      </Box>
    </Paper>
  );
}

function CrewAssignmentsPanel({ packageId, projectId }: { packageId: string; projectId: string }) {
  const qc = useQueryClient();
  const { data: rows = [] } = useQuery({ queryKey: ["workPackageCrews", packageId], queryFn: () => workPackageApi.crews(packageId) });
  const { data: crews = [] } = useQuery({ queryKey: ["crews"], queryFn: crewApi.list });
  const [crewId, setCrewId] = useState("");
  const [plannedStart, setPlannedStart] = useState(today());
  const [plannedEnd, setPlannedEnd] = useState("");
  const options = crews.filter((c) => !projectId || c.projectId === projectId);

  const add = useMutation({
    mutationFn: () => workPackageApi.addCrew(packageId, { crewId, plannedStart, plannedEnd: plannedEnd || undefined, status: "ACTIVE" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workPackageCrews", packageId] });
      qc.invalidateQueries({ queryKey: ["workPackageRequirements", packageId] });
      qc.invalidateQueries({ queryKey: ["workPackages"] });
      setCrewId("");
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => workPackageApi.removeCrew(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workPackageCrews", packageId] });
      qc.invalidateQueries({ queryKey: ["workPackageRequirements", packageId] });
      qc.invalidateQueries({ queryKey: ["workPackages"] });
    },
  });

  return (
    <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2 }}>
      <Typography variant="subtitle2" fontWeight={900}>Assigned crews</Typography>
      <Typography variant="caption" color="text.secondary">Only crews from the same project are available.</Typography>
      <Grid container spacing={1} mt={0.5}>
        <Grid item xs={12} md={5}>
        <TextField fullWidth select size="small" label="Crew" value={crewId} onChange={(e) => setCrewId(e.target.value)}>
          <MenuItem value="">Select crew</MenuItem>
          {options.map((c) => <MenuItem key={c.id} value={c.id}>{c.code} - {c.name}</MenuItem>)}
        </TextField>
        </Grid>
        <Grid item xs={6} md={3}>
          <TextField fullWidth size="small" type="date" label="Start" InputLabelProps={{ shrink: true }} value={plannedStart} onChange={(e) => setPlannedStart(e.target.value)} />
        </Grid>
        <Grid item xs={6} md={3}>
          <TextField fullWidth size="small" type="date" label="End" InputLabelProps={{ shrink: true }} value={plannedEnd} onChange={(e) => setPlannedEnd(e.target.value)} />
        </Grid>
        <Grid item xs={12} md={2}>
          <Button fullWidth variant="contained" disabled={!crewId || add.isPending} onClick={() => add.mutate()}>Assign</Button>
        </Grid>
      </Grid>
      <Divider sx={{ my: 1.5 }} />
      <Box sx={{ overflowX: "auto" }}>
        <Table size="small" sx={{ minWidth: 460 }}>
          <TableHead>
            <TableRow>
              <TableCell>Crew</TableCell>
              <TableCell width={150}>Dates</TableCell>
              <TableCell width={90}>Status</TableCell>
              <TableCell width={52} align="right" />
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.id}>
                <TableCell sx={{ whiteSpace: "normal", wordBreak: "break-word" }}>{r.crewCode} - {r.crewName}</TableCell>
                <TableCell>{r.plannedStart ?? "-"} {"->"} {r.plannedEnd ?? "-"}</TableCell>
                <TableCell><Chip size="small" label={r.status ?? "ACTIVE"} /></TableCell>
                <TableCell align="right"><IconButton size="small" color="error" onClick={() => r.id && remove.mutate(r.id)}><DeleteIcon fontSize="small" /></IconButton></TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && <TableRow><TableCell colSpan={4}><Typography variant="body2" color="text.secondary">No crews assigned yet.</Typography></TableCell></TableRow>}
          </TableBody>
        </Table>
      </Box>
    </Paper>
  );
}
