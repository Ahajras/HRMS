import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Grid,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import SaveIcon from "@mui/icons-material/Save";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { approvalWorkflowApi, employeeApi, projectApi } from "../api/resources";
import type { ApprovalWorkflow, ApprovalWorkflowStep } from "../api/types";

const PROCESS_OPTIONS = [
  { value: "TIMESHEET_SUBMIT", label: "Timesheet Submit" },
  { value: "LEAVE_REQUEST", label: "Leave Request" },
  { value: "PAYROLL_RUN", label: "Payroll Run" },
  { value: "DAY_ZERO_ADJUSTMENT", label: "Day Zero Adjustment" },
];

const APPROVER_TYPES = [
  { value: "SUPERVISOR", label: "Employee supervisor" },
  { value: "PROJECT_ROLE", label: "Project role" },
  { value: "SPECIFIC_EMPLOYEE", label: "Specific employee" },
];

const PROJECT_ROLES = ["MANAGER", "HR", "HR_MANAGER", "PROJECT_MANAGER"];
const PAY_GROUPS = ["ALL", "MONTHLY", "DAILY", "WEEKLY", "BIWEEKLY", "HOURLY"];

function processLabel(value: string) {
  return PROCESS_OPTIONS.find((p) => p.value === value)?.label ?? value;
}

function emptyWorkflow(projectId = ""): ApprovalWorkflow {
  return {
    processCode: "TIMESHEET_SUBMIT",
    projectId,
    payGroup: "ALL",
    name: "",
    status: "ACTIVE",
    steps: [
      { stepOrder: 1, name: "Direct supervisor", approverType: "SUPERVISOR", status: "ACTIVE" },
      { stepOrder: 2, name: "Project HR manager", approverType: "PROJECT_ROLE", approverRoleCode: "HR_MANAGER", status: "ACTIVE" },
    ],
  };
}

export default function ApprovalWorkflowsPage() {
  const qc = useQueryClient();
  const { data: workflows = [] } = useQuery({ queryKey: ["approvalWorkflows"], queryFn: approvalWorkflowApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: employees } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500) });
  const empList = employees?.content ?? [];
  const [selectedId, setSelectedId] = useState("");
  const [form, setForm] = useState<ApprovalWorkflow>(emptyWorkflow());

  useEffect(() => {
    if (!form.projectId && projects[0]?.id) setForm((f) => ({ ...f, projectId: projects[0].id! }));
  }, [form.projectId, projects]);

  const selected = useMemo(() => workflows.find((w) => w.id === selectedId), [workflows, selectedId]);
  const workflowsByProject = useMemo(() => projects.map((project) => ({
    project,
    workflows: workflows
      .filter((workflow) => workflow.projectId === project.id)
      .sort((a, b) => `${a.processCode}-${a.payGroup}`.localeCompare(`${b.processCode}-${b.payGroup}`)),
  })), [projects, workflows]);
  useEffect(() => {
    if (selected) setForm({ ...selected, steps: [...selected.steps].sort((a, b) => a.stepOrder - b.stepOrder) });
  }, [selected]);

  const save = useMutation({
    mutationFn: (payload: ApprovalWorkflow) => approvalWorkflowApi.save({
      ...payload,
      name: payload.name || `${payload.processCode} - ${projects.find((p) => p.id === payload.projectId)?.code ?? "Project"}`,
      steps: payload.steps.map((s, index) => ({ ...s, stepOrder: index + 1 })),
    }),
    onSuccess: (saved) => {
      setSelectedId(saved.id ?? "");
      setForm(saved);
      qc.invalidateQueries({ queryKey: ["approvalWorkflows"] });
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => approvalWorkflowApi.remove(id),
    onSuccess: () => {
      setSelectedId("");
      setForm(emptyWorkflow(projects[0]?.id));
      qc.invalidateQueries({ queryKey: ["approvalWorkflows"] });
    },
  });

  const setStep = (idx: number, patch: Partial<ApprovalWorkflowStep>) => {
    setForm((f) => ({
      ...f,
      steps: f.steps.map((step, i) => i === idx ? { ...step, ...patch } : step),
    }));
  };
  const addStep = () => setForm((f) => ({
    ...f,
    steps: [...f.steps, { stepOrder: f.steps.length + 1, name: "Project approver", approverType: "PROJECT_ROLE", approverRoleCode: "PROJECT_MANAGER", status: "ACTIVE" }],
  }));
  const removeStep = (idx: number) => setForm((f) => ({ ...f, steps: f.steps.filter((_s, i) => i !== idx).map((s, i) => ({ ...s, stepOrder: i + 1 })) }));

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1} flexWrap="wrap" mb={2}>
        <Typography variant="h5">Approval Workflows</Typography>
        <Button startIcon={<AddIcon />} variant="outlined" onClick={() => { setSelectedId(""); setForm(emptyWorkflow(projects[0]?.id)); }}>
          New workflow
        </Button>
      </Stack>

      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <Stack spacing={1.25}>
            {workflowsByProject.map(({ project, workflows: projectWorkflows }) => (
              <Paper key={project.id} variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
                <Box sx={{ px: 1.5, py: 1.25, bgcolor: "action.hover" }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
                    <Box>
                      <Typography fontWeight={800}>{project.code} - {project.name}</Typography>
                      <Typography variant="caption" color="text.secondary">Project approval workflow setup</Typography>
                    </Box>
                    <Chip size="small" label={`${projectWorkflows.length} workflow${projectWorkflows.length === 1 ? "" : "s"}`} />
                  </Stack>
                </Box>
                <Divider />
                <Stack spacing={0.75} sx={{ p: 1 }}>
                  {projectWorkflows.map((row) => (
                    <Paper
                      key={row.id}
                      variant="outlined"
                      onClick={() => setSelectedId(row.id ?? "")}
                      sx={{
                        p: 1,
                        borderRadius: 2,
                        cursor: "pointer",
                        borderColor: row.id === selectedId ? "primary.main" : "divider",
                        bgcolor: row.id === selectedId ? "rgba(37, 99, 235, 0.08)" : "background.paper",
                      }}
                    >
                      <Stack direction="row" justifyContent="space-between" alignItems="center" gap={1}>
                        <Box>
                          <Typography variant="body2" fontWeight={700}>{processLabel(row.processCode)}</Typography>
                          <Stack direction="row" spacing={0.75} mt={0.5} flexWrap="wrap">
                            <Chip size="small" label={row.payGroup ?? "ALL"} />
                            <Chip size="small" variant="outlined" label={`${row.steps?.length ?? 0} steps`} />
                          </Stack>
                        </Box>
                        <Typography variant="caption" color="text.secondary">{row.status ?? "ACTIVE"}</Typography>
                      </Stack>
                    </Paper>
                  ))}
                  {projectWorkflows.length === 0 && (
                    <Typography variant="body2" color="text.secondary" p={1}>No workflows for this project yet.</Typography>
                  )}
                </Stack>
              </Paper>
            ))}
            {projects.length === 0 && (
              <Paper variant="outlined" sx={{ borderRadius: 2, p: 2 }}>
                <Typography variant="body2" color="text.secondary">Create a project first, then add approval workflows.</Typography>
              </Paper>
            )}
          </Stack>
        </Grid>

        <Grid item xs={12} md={8}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
            <Grid container spacing={1.5}>
              <Grid item xs={12} sm={6}>
                <TextField select fullWidth label="Project" value={form.projectId} onChange={(e) => setForm({ ...form, projectId: e.target.value })}>
                  {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField select fullWidth label="Process" value={form.processCode} onChange={(e) => setForm({ ...form, processCode: e.target.value })}>
                  {PROCESS_OPTIONS.map((p) => <MenuItem key={p.value} value={p.value}>{p.label}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField select fullWidth label="Pay group" value={form.payGroup} onChange={(e) => setForm({ ...form, payGroup: e.target.value })}>
                  {PAY_GROUPS.map((g) => <MenuItem key={g} value={g}>{g}</MenuItem>)}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={8}>
                <TextField fullWidth label="Name" value={form.name ?? ""} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </Grid>
            </Grid>

            <Typography variant="subtitle2" mt={2} mb={1}>Approval steps</Typography>
            <Stack spacing={1}>
              {form.steps.map((step, idx) => (
                <Paper key={idx} variant="outlined" sx={{ p: 1.25, borderRadius: 2 }}>
                  <Grid container spacing={1} alignItems="center">
                    <Grid item xs={12} sm={1}><Typography fontWeight={700}>{idx + 1}</Typography></Grid>
                    <Grid item xs={12} sm={3}>
                      <TextField size="small" fullWidth label="Step name" value={step.name ?? ""} onChange={(e) => setStep(idx, { name: e.target.value })} />
                    </Grid>
                    <Grid item xs={12} sm={3}>
                      <TextField size="small" select fullWidth label="Approver type" value={step.approverType} onChange={(e) => setStep(idx, { approverType: e.target.value, approverRoleCode: undefined, approverEmployeeId: undefined })}>
                        {APPROVER_TYPES.map((t) => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}
                      </TextField>
                    </Grid>
                    {step.approverType === "PROJECT_ROLE" && (
                      <Grid item xs={12} sm={3}>
                        <TextField size="small" select fullWidth label="Project role" value={step.approverRoleCode ?? ""} onChange={(e) => setStep(idx, { approverRoleCode: e.target.value })}>
                          {PROJECT_ROLES.map((r) => <MenuItem key={r} value={r}>{r}</MenuItem>)}
                        </TextField>
                      </Grid>
                    )}
                    {step.approverType === "SPECIFIC_EMPLOYEE" && (
                      <Grid item xs={12} sm={3}>
                        <TextField size="small" select fullWidth label="Employee" value={step.approverEmployeeId ?? ""} onChange={(e) => setStep(idx, { approverEmployeeId: e.target.value })}>
                          {empList.map((e) => <MenuItem key={e.id} value={e.id}>{e.employeeNumber} - {e.firstName} {e.lastName}</MenuItem>)}
                        </TextField>
                      </Grid>
                    )}
                    <Grid item xs={12} sm={2}>
                      <Button color="error" startIcon={<DeleteIcon />} disabled={form.steps.length <= 1} onClick={() => removeStep(idx)}>Remove</Button>
                    </Grid>
                  </Grid>
                </Paper>
              ))}
            </Stack>

            {save.isError && <Alert severity="error" sx={{ mt: 2 }}>{(save.error as any)?.response?.data?.message ?? "Could not save workflow."}</Alert>}
            {remove.isError && <Alert severity="error" sx={{ mt: 2 }}>{(remove.error as any)?.response?.data?.message ?? "Could not delete workflow."}</Alert>}
            <Stack direction="row" spacing={1} mt={2}>
              <Button startIcon={<AddIcon />} onClick={addStep}>Add step</Button>
              <Button variant="contained" startIcon={<SaveIcon />} disabled={save.isPending || !form.projectId || form.steps.length === 0} onClick={() => save.mutate(form)}>
                Save workflow
              </Button>
              {form.id && <Button color="error" startIcon={<DeleteIcon />} disabled={remove.isPending} onClick={() => remove.mutate(form.id!)}>Delete</Button>}
            </Stack>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
