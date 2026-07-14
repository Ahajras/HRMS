import { useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Grid,
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
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import AddIcon from "@mui/icons-material/Add";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { projectApi, projectApprovalRoleApi } from "../api/resources";
import type { ProjectApprovalRole } from "../api/types";

const APPROVAL_ROLES = [
  { code: "MANAGER", label: "Manager" },
  { code: "HR", label: "HR" },
  { code: "HR_MANAGER", label: "HR Manager" },
  { code: "PROJECT_MANAGER", label: "Project Manager" },
];

export default function ProjectApprovalRolesPage() {
  const qc = useQueryClient();
  const [projectId, setProjectId] = useState("");
  const [roleCode, setRoleCode] = useState("HR_MANAGER");
  const [employeeId, setEmployeeId] = useState("");

  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: rows = [] } = useQuery({ queryKey: ["projectApprovalRoles"], queryFn: projectApprovalRoleApi.list });
  const { data: candidates = [] } = useQuery({
    queryKey: ["approvalRoleCandidates", roleCode],
    queryFn: () => projectApprovalRoleApi.candidates(roleCode),
    enabled: !!roleCode,
  });

  useEffect(() => {
    if (!projectId && projects[0]?.id) setProjectId(projects[0].id);
  }, [projectId, projects]);

  useEffect(() => {
    setEmployeeId("");
  }, [roleCode]);

  const save = useMutation({
    mutationFn: () => projectApprovalRoleApi.create({ projectId, roleCode, employeeId, status: "ACTIVE" } as ProjectApprovalRole),
    onSuccess: () => {
      setEmployeeId("");
      qc.invalidateQueries({ queryKey: ["projectApprovalRoles"] });
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => projectApprovalRoleApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["projectApprovalRoles"] }),
  });

  const filtered = rows.filter((r) => !projectId || r.projectId === projectId);

  return (
    <Box>
      <Typography variant="h5" mb={2}>Project Approval Roles</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" mb={1}>Assign approvers by project</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} md={4}>
            <TextField select fullWidth size="small" label="Project" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField select fullWidth size="small" label="Approval role" value={roleCode} onChange={(e) => setRoleCode(e.target.value)}>
              {APPROVAL_ROLES.map((r) => <MenuItem key={r.code} value={r.code}>{r.label}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField select fullWidth size="small" label="Employee user" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
              {candidates.map((c) => (
                <MenuItem key={c.employeeId} value={c.employeeId}>
                  {c.employeeNumber} - {c.employeeName} ({c.username})
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid item xs={12} md={2}>
            <Button startIcon={<AddIcon />} variant="contained" disabled={!projectId || !roleCode || !employeeId || save.isPending} onClick={() => save.mutate()}>
              Assign
            </Button>
          </Grid>
        </Grid>
        {candidates.length === 0 && (
          <Alert severity="info" sx={{ mt: 1.5 }}>
            No linked users found for role {roleCode}. Create/update a user, link it to an employee, and assign this role first.
          </Alert>
        )}
        {save.isError && (
          <Alert severity="error" sx={{ mt: 1.5 }}>
            {(save.error as any)?.response?.data?.message ?? "Could not assign approval role."}
          </Alert>
        )}
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Project</TableCell>
              <TableCell>Role</TableCell>
              <TableCell>Employee</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filtered.map((row) => (
              <TableRow key={row.id}>
                <TableCell>{row.projectCode}</TableCell>
                <TableCell><Chip size="small" label={row.roleCode} /></TableCell>
                <TableCell>{row.employeeNumber} - {row.employeeName}</TableCell>
                <TableCell>{row.status ?? "ACTIVE"}</TableCell>
                <TableCell align="right">
                  <Button size="small" color="error" startIcon={<DeleteIcon />} disabled={remove.isPending} onClick={() => row.id && remove.mutate(row.id)}>
                    Delete
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {filtered.length === 0 && (
              <TableRow>
                <TableCell colSpan={5}>
                  <Typography variant="body2" color="text.secondary" p={1}>No approval roles assigned for this project.</Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      <Stack spacing={0.5} mt={2}>
        <Typography variant="caption" color="text.secondary">Demo resolver idea:</Typography>
        <Typography variant="caption" color="text.secondary">Timesheet step 1 = employee supervisor. Step 2 = project role HR_MANAGER or HR.</Typography>
      </Stack>
    </Box>
  );
}
