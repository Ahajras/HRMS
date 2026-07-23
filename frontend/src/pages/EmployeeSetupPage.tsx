import { useEffect, useMemo, useState } from "react";
import {
  Autocomplete,
  Box,
  Button,
  Chip,
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
import SaveIcon from "@mui/icons-material/Save";
import { roleApi } from "../api/auth";
import { lookupApi } from "../api/resources";
import type { LookupValue, Role } from "../api/types";

const EMPLOYEE_ALERT_SETTING_CATEGORY = "EMPLOYEE_DOCUMENT_ALERT_SETTING";
const ALERT_DAYS_CODE = "ALERT_DAYS";
const ALERT_ROLES_CODE = "ALERT_ROLES";

const EMPTY_JOB_TITLE: LookupValue = {
  category: "JOB_TITLE",
  code: "",
  label: "",
  sortOrder: 100,
  status: "ACTIVE",
};

export default function EmployeeSetupPage() {
  const qc = useQueryClient();
  const { data: jobTitles = [] } = useQuery({ queryKey: ["lookup", "JOB_TITLE"], queryFn: () => lookupApi.byCategory("JOB_TITLE") });
  const { data: alertSettings = [] } = useQuery({
    queryKey: ["lookup", EMPLOYEE_ALERT_SETTING_CATEGORY],
    queryFn: () => lookupApi.byCategory(EMPLOYEE_ALERT_SETTING_CATEGORY),
  });
  const { data: roles = [] } = useQuery({ queryKey: ["roles"], queryFn: roleApi.list });
  const [jobForm, setJobForm] = useState<LookupValue>(EMPTY_JOB_TITLE);
  const [alertDaysText, setAlertDaysText] = useState("60,30,7");
  const [alertRoleCodes, setAlertRoleCodes] = useState<string[]>([]);

  const selectedRoles = useMemo(
    () => roles.filter((r) => alertRoleCodes.includes(r.code)),
    [roles, alertRoleCodes],
  );

  useEffect(() => {
    const days = alertSettings.find((s) => s.code === ALERT_DAYS_CODE)?.label;
    const roleText = alertSettings.find((s) => s.code === ALERT_ROLES_CODE)?.label;
    if (days) setAlertDaysText(days);
    if (roleText) setAlertRoleCodes(roleText.split(",").map((v) => v.trim()).filter(Boolean));
  }, [alertSettings]);

  const saveSetting = (code: string, label: string, sortOrder: number) => {
    const existing = alertSettings.find((s) => s.code === code);
    const payload: LookupValue = {
      ...existing,
      category: EMPLOYEE_ALERT_SETTING_CATEGORY,
      code,
      label,
      sortOrder,
      status: "ACTIVE",
    };
    return existing?.id ? lookupApi.update(existing.id, payload) : lookupApi.create(payload);
  };

  const saveAlertSettings = useMutation({
    mutationFn: async () => {
      await saveSetting(ALERT_DAYS_CODE, alertDaysText.trim() || "60,30,7", 10);
      await saveSetting(ALERT_ROLES_CODE, alertRoleCodes.join(","), 20);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lookup", EMPLOYEE_ALERT_SETTING_CATEGORY] }),
  });

  const saveJobTitle = useMutation({
    mutationFn: (payload: LookupValue) => {
      const dto = {
        ...payload,
        category: "JOB_TITLE",
        code: payload.code.trim().toUpperCase(),
        label: payload.label.trim(),
        sortOrder: Number(payload.sortOrder ?? 100),
        status: payload.status || "ACTIVE",
      };
      return dto.id ? lookupApi.update(dto.id, dto) : lookupApi.create(dto);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["lookup", "JOB_TITLE"] });
      setJobForm(EMPTY_JOB_TITLE);
    },
  });

  const deleteJobTitle = useMutation({
    mutationFn: (id: string) => lookupApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["lookup", "JOB_TITLE"] }),
  });

  const canSaveJobTitle = !!jobForm.code.trim() && !!jobForm.label.trim();

  return (
    <Box>
      <Typography variant="h5" mb={0.5}>Employee Setup</Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Configure employee master-data references and document expiry notification settings.
      </Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle1" fontWeight={900} mb={1}>Document expiry notifications</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              label="Alert days"
              value={alertDaysText}
              onChange={(e) => setAlertDaysText(e.target.value)}
              helperText="Example: 60,30,7 means two months, one month, one week."
            />
          </Grid>
          <Grid item xs={12} md={8}>
            <Autocomplete
              multiple
              options={roles}
              value={selectedRoles}
              getOptionLabel={(r: Role) => `${r.code} - ${r.name}`}
              onChange={(_e, value) => setAlertRoleCodes(value.map((r) => r.code))}
              renderTags={(value, getTagProps) =>
                value.map((option, index) => (
                  <Chip label={option.code} {...getTagProps({ index })} key={option.code} />
                ))
              }
              renderInput={(params) => (
                <TextField {...params} label="Notify roles" helperText="Admins with these roles will receive document expiry alerts." />
              )}
            />
          </Grid>
          <Grid item xs={12}>
            <Button startIcon={<SaveIcon />} variant="contained" onClick={() => saveAlertSettings.mutate()} disabled={saveAlertSettings.isPending}>
              Save notification settings
            </Button>
          </Grid>
        </Grid>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle1" fontWeight={900} mb={1}>Job titles</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={3}>
            <TextField
              fullWidth
              required
              label="Job title code"
              value={jobForm.code}
              onChange={(e) => setJobForm({ ...jobForm, code: e.target.value.toUpperCase() })}
            />
          </Grid>
          <Grid item xs={12} sm={5}>
            <TextField
              fullWidth
              required
              label="Job title"
              value={jobForm.label}
              onChange={(e) => setJobForm({ ...jobForm, label: e.target.value })}
            />
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField
              fullWidth
              label="Sort"
              type="number"
              value={jobForm.sortOrder ?? 100}
              onChange={(e) => setJobForm({ ...jobForm, sortOrder: Number(e.target.value) })}
            />
          </Grid>
          <Grid item xs={6} sm={2}>
            <Stack direction="row" spacing={1}>
              <Button
                startIcon={jobForm.id ? <SaveIcon /> : <AddIcon />}
                variant="contained"
                disabled={!canSaveJobTitle || saveJobTitle.isPending}
                onClick={() => saveJobTitle.mutate(jobForm)}
              >
                {jobForm.id ? "Save" : "Add"}
              </Button>
              {jobForm.id && <Button onClick={() => setJobForm(EMPTY_JOB_TITLE)}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
        {jobTitles.map((item) => (
          <Stack
            key={item.id ?? item.code}
            direction="row"
            alignItems="center"
            justifyContent="space-between"
            sx={{ px: 2, py: 1.25, borderBottom: "1px solid", borderColor: "divider" }}
          >
            <Box>
              <Typography fontWeight={800}>{item.code} - {item.label}</Typography>
              <Typography variant="caption" color="text.secondary">Sort {item.sortOrder ?? 0} · {item.status ?? "ACTIVE"}</Typography>
            </Box>
            <Stack direction="row" spacing={0.5}>
              <Button size="small" onClick={() => setJobForm(item)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => item.id && deleteJobTitle.mutate(item.id)}>
                <DeleteIcon />
              </IconButton>
            </Stack>
          </Stack>
        ))}
        {jobTitles.length === 0 && (
          <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>
            No job titles yet. Add the first code above.
          </Typography>
        )}
      </Paper>
    </Box>
  );
}
