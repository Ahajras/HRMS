import { useState } from "react";
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { DataGrid, type GridColDef } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import { employeeApi, projectApi, projectApprovalRoleApi } from "../api/resources";
import { roleApi, userApi } from "../api/auth";
import { getCompanyId } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import type { AuthUser, ProjectApprovalRole, UserPayload } from "../api/types";

const EMPTY: UserPayload = {
  username: "",
  email: "",
  password: "",
  fullName: "",
  status: "ACTIVE",
  roles: [],
};

const STATUSES = ["ACTIVE", "DISABLED", "LOCKED"];
const APPROVAL_ROLE_CODES = ["MANAGER", "HR", "HR_MANAGER", "PROJECT_MANAGER"];
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export default function UsersPage() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const { data = [], isLoading } = useQuery({ queryKey: ["users"], queryFn: userApi.list });
  const { data: roles = [] } = useQuery({ queryKey: ["roles"], queryFn: roleApi.list });
  const { data: employees } = useQuery({ queryKey: ["employeesAll"], queryFn: () => employeeApi.list(0, 500) });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: projectApprovalRows = [] } = useQuery({ queryKey: ["projectApprovalRoles"], queryFn: projectApprovalRoleApi.list });
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<UserPayload>(EMPTY);
  const [approvalProjectId, setApprovalProjectId] = useState("");
  const [approvalRoleCode, setApprovalRoleCode] = useState("HR_MANAGER");
  const editing = !!form.id;
  const companyInvalid = !!form.companyId && !UUID_RE.test(form.companyId);
  const empList = employees?.content ?? [];

  const save = useMutation({
    mutationFn: (u: UserPayload) => (u.id ? userApi.update(u.id, u) : userApi.create(u)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["users"] });
      setOpen(false);
    },
  });

  const openEdit = (u: AuthUser) => {
    setForm({
      id: u.id,
      companyId: u.companyId,
      employeeId: u.employeeId,
      username: u.username,
      email: u.email,
      password: "",
      fullName: u.fullName,
      status: u.status ?? "ACTIVE",
      roles: u.roles ?? [],
    });
    setApprovalProjectId(projects[0]?.id ?? "");
    setApprovalRoleCode((u.roles ?? []).find((r) => APPROVAL_ROLE_CODES.includes(r)) ?? "HR_MANAGER");
    setOpen(true);
  };

  const columns: GridColDef<AuthUser>[] = [
    { field: "username", headerName: "Username", width: 180 },
    { field: "fullName", headerName: "Full name", flex: 1 },
    { field: "email", headerName: "Email", width: 220 },
    {
      field: "roles",
      headerName: "Roles",
      width: 200,
      valueGetter: (_v, row) => (row.roles ?? []).join(", "),
    },
    { field: "status", headerName: "Status", width: 120 },
  ];

  const roleCodes = roles.map((r) => r.code);
  const userApprovalRoles = (form.roles ?? []).filter((r) => APPROVAL_ROLE_CODES.includes(r));
  const currentProjectAccess = projectApprovalRows.filter((row) => row.employeeId === form.employeeId);
  const canAssignProjectAccess = editing && !!form.employeeId && !!approvalProjectId && !!approvalRoleCode && userApprovalRoles.includes(approvalRoleCode);

  const addProjectAccess = useMutation({
    mutationFn: () => projectApprovalRoleApi.create({
      projectId: approvalProjectId,
      roleCode: approvalRoleCode,
      employeeId: form.employeeId,
      status: "ACTIVE",
    } as ProjectApprovalRole),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["projectApprovalRoles"] }),
  });

  const removeProjectAccess = useMutation({
    mutationFn: (id: string) => projectApprovalRoleApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["projectApprovalRoles"] }),
  });

  return (
    <Box>
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        gap={1}
        flexWrap="wrap"
        mb={2}
      >
        <Typography variant="h5">Users</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => {
          setForm({ ...EMPTY, companyId: user?.companyId ?? (getCompanyId() || undefined) });
          setApprovalProjectId(projects[0]?.id ?? "");
          setApprovalRoleCode("HR_MANAGER");
          setOpen(true);
        }}>
          New User
        </Button>
      </Stack>

      <div style={{ height: 520, width: "100%" }}>
        <DataGrid
          rows={data}
          columns={columns}
          loading={isLoading}
          getRowId={(r) => r.id ?? r.username}
          onRowClick={(p) => openEdit(p.row as AuthUser)}
          pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        />
      </div>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? "Edit User" : "New User"}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <TextField label="Username" value={form.username} disabled={editing}
              onChange={(e) => setForm({ ...form, username: e.target.value })} />
            <TextField label="Full name" value={form.fullName ?? ""}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            <TextField label="Email" value={form.email ?? ""}
              onChange={(e) => setForm({ ...form, email: e.target.value })} />
            <TextField
              label={editing ? "New password (leave blank to keep)" : "Password"}
              type="password"
              value={form.password ?? ""}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
            <TextField label="Company ID (UUID; blank uses selected company)" value={form.companyId ?? ""}
              error={companyInvalid}
              helperText={companyInvalid ? "Company ID must be a UUID, not an employee/project number." : "For the default demo company use 00000000-0000-0000-0000-0000000000c1."}
              onChange={(e) => setForm({ ...form, companyId: e.target.value.trim() || undefined })} />
            <Autocomplete
              options={empList}
              value={empList.find((emp) => emp.id === form.employeeId) ?? null}
              getOptionLabel={(emp) => `${emp.employeeNumber} - ${emp.firstName} ${emp.lastName}`}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              onChange={(_event, employee) => setForm({ ...form, employeeId: employee?.id })}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Linked employee"
                  placeholder="Search by employee number or name"
                  helperText="Type an employee number or name to search. Required for TIMEKEEPER users."
                />
              )}
            />
            <Autocomplete
              multiple
              options={roleCodes}
              value={form.roles ?? []}
              onChange={(_e, value) => {
                setForm({ ...form, roles: value });
                const nextApprovalRole = value.find((r) => APPROVAL_ROLE_CODES.includes(r));
                if (nextApprovalRole && !value.includes(approvalRoleCode)) setApprovalRoleCode(nextApprovalRole);
              }}
              renderInput={(params) => <TextField {...params} label="Roles" placeholder="Assign role" />}
            />
            <Box sx={{ border: 1, borderColor: "divider", borderRadius: 2, p: 1.5 }}>
              <Typography variant="subtitle2" mb={1}>Project approval access</Typography>
              {!editing && (
                <Alert severity="info" sx={{ mb: 1.5 }}>
                  Save the user first, then reopen it to assign project approval access.
                </Alert>
              )}
              {!form.employeeId && (
                <Alert severity="info" sx={{ mb: 1.5 }}>
                  Link this user to an employee first. Approval access is stored by employee and project.
                </Alert>
              )}
              {form.employeeId && userApprovalRoles.length === 0 && (
                <Alert severity="warning" sx={{ mb: 1.5 }}>
                  Assign MANAGER, HR, HR_MANAGER, or PROJECT_MANAGER before adding project approval access.
                </Alert>
              )}
              <Stack direction={{ xs: "column", sm: "row" }} spacing={1} alignItems={{ sm: "center" }}>
                <TextField
                  select
                  size="small"
                  label="Project"
                  value={approvalProjectId}
                  onChange={(e) => setApprovalProjectId(e.target.value)}
                  sx={{ minWidth: 220, flex: 1 }}
                >
                  {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
                </TextField>
                <TextField
                  select
                  size="small"
                  label="Approval role"
                  value={approvalRoleCode}
                  onChange={(e) => setApprovalRoleCode(e.target.value)}
                  sx={{ minWidth: 180 }}
                >
                  {APPROVAL_ROLE_CODES.map((r) => (
                    <MenuItem key={r} value={r} disabled={!userApprovalRoles.includes(r)}>{r}</MenuItem>
                  ))}
                </TextField>
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  disabled={!canAssignProjectAccess || addProjectAccess.isPending}
                  onClick={() => addProjectAccess.mutate()}
                >
                  Add
                </Button>
              </Stack>
              {addProjectAccess.isError && (
                <Alert severity="error" sx={{ mt: 1.5 }}>
                  {(addProjectAccess.error as any)?.response?.data?.message ?? "Could not add project approval access."}
                </Alert>
              )}
              <Stack direction="row" spacing={1} mt={1.5} flexWrap="wrap" useFlexGap>
                {currentProjectAccess.map((row) => (
                  <Chip
                    key={row.id}
                    label={`${row.projectCode ?? "Project"} - ${row.roleCode}`}
                    onDelete={row.id ? () => removeProjectAccess.mutate(row.id!) : undefined}
                    deleteIcon={<DeleteIcon />}
                    disabled={removeProjectAccess.isPending}
                  />
                ))}
                {form.employeeId && currentProjectAccess.length === 0 && (
                  <Typography variant="caption" color="text.secondary">No project approval access assigned yet.</Typography>
                )}
              </Stack>
            </Box>
            <TextField select label="Status" value={form.status ?? "ACTIVE"}
              onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {STATUSES.map((s) => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => save.mutate(form)} disabled={save.isPending || companyInvalid}>Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
