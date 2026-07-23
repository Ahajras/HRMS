import { useMemo, useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  Chip,
  FormControlLabel,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
import EditIcon from "@mui/icons-material/Edit";
import EventBusyIcon from "@mui/icons-material/EventBusy";
import PaymentsIcon from "@mui/icons-material/Payments";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import SaveIcon from "@mui/icons-material/Save";
import { payrollComponentApi, publicHolidayApi, timeTypeApi, timeTypePayrollRuleApi } from "../api/resources";
import type { PayrollComponent, PublicHoliday, TimeType, TimeTypePayrollRule } from "../api/types";

const EMPTY_TYPE: TimeType = {
  code: "",
  name: "",
  category: "",
  paid: true,
  countsAsWorked: true,
  affectsLeave: false,
  factor: 1,
  sortOrder: 0,
};

const EMPTY_RULE: TimeTypePayrollRule = {
  timeTypeId: "",
  payrollComponentId: "",
  action: "PAY",
  percent: 100,
  basis: "HOURS",
  thresholdDays: 0,
  thresholdScope: "NONE",
  yearBasis: "CALENDAR",
  affectsOvertime: false,
  processSeparately: false,
  sortOrder: 100,
};

const actionColor = (action: string) => {
  if (action === "DEDUCT") return "error" as const;
  if (action === "PAY") return "success" as const;
  if (action === "SUSPEND") return "warning" as const;
  return "default" as const;
};

function TimeTypesSection() {
  const qc = useQueryClient();
  const { data: timeTypes = [] } = useQuery({ queryKey: ["timeTypes"], queryFn: timeTypeApi.list });
  const { data: components = [] } = useQuery({ queryKey: ["payrollComponents"], queryFn: () => payrollComponentApi.list() });
  const [form, setForm] = useState<TimeType>(EMPTY_TYPE);
  const [ruleForm, setRuleForm] = useState<TimeTypePayrollRule>(EMPTY_RULE);
  const [search, setSearch] = useState("");

  const filteredTypes = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return timeTypes;
    return timeTypes.filter((t) => `${t.code} ${t.name} ${t.category ?? ""}`.toLowerCase().includes(q));
  }, [search, timeTypes]);

  const selectedTimeTypeId = form.id ?? "";
  const { data: rules = [] } = useQuery({
    queryKey: ["timeTypePayrollRules", selectedTimeTypeId],
    queryFn: () => timeTypePayrollRuleApi.list(selectedTimeTypeId),
    enabled: !!selectedTimeTypeId,
  });

  const save = useMutation({
    mutationFn: (t: TimeType) => (t.id ? timeTypeApi.update(t.id, t) : timeTypeApi.create(t)),
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: ["timeTypes"] });
      setForm(saved);
      setRuleForm({ ...EMPTY_RULE, timeTypeId: saved.id! });
    },
  });
  const del = useMutation({
    mutationFn: (id: string) => timeTypeApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timeTypes"] });
      setForm(EMPTY_TYPE);
      setRuleForm(EMPTY_RULE);
    },
  });
  const saveRule = useMutation({
    mutationFn: (rule: TimeTypePayrollRule) => timeTypePayrollRuleApi.save(selectedTimeTypeId, rule),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["timeTypePayrollRules", selectedTimeTypeId] });
      setRuleForm({ ...EMPTY_RULE, timeTypeId: selectedTimeTypeId });
    },
  });
  const deleteRule = useMutation({
    mutationFn: (componentId: string) => timeTypePayrollRuleApi.remove(selectedTimeTypeId, componentId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["timeTypePayrollRules", selectedTimeTypeId] }),
  });
  const initializeDefaults = useMutation({
    mutationFn: () => timeTypePayrollRuleApi.initializeDefaults(selectedTimeTypeId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["timeTypePayrollRules", selectedTimeTypeId] }),
  });

  const selectTimeType = (timeType: TimeType) => {
    setForm(timeType);
    setRuleForm({ ...EMPTY_RULE, timeTypeId: timeType.id! });
  };

  const resetForm = () => {
    setForm(EMPTY_TYPE);
    setRuleForm(EMPTY_RULE);
  };

  return (
    <Grid container spacing={2}>
      <Grid item xs={12} lg={3.4}>
        <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden", position: { lg: "sticky" }, top: 86 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ p: 1.5, borderBottom: "1px solid", borderColor: "divider" }}>
            <Box>
              <Typography fontWeight={900}>Time types</Typography>
              <Typography variant="caption" color="text.secondary">{timeTypes.length} configured</Typography>
            </Box>
            <Button size="small" startIcon={<AddIcon />} onClick={resetForm}>New</Button>
          </Stack>
          <Box sx={{ p: 1.5 }}>
            <TextField fullWidth size="small" label="Search" value={search} onChange={(e) => setSearch(e.target.value)} />
          </Box>
          <Stack sx={{ maxHeight: { lg: "calc(100vh - 230px)" }, overflow: "auto", pb: 1 }}>
            {filteredTypes.map((timeType) => {
              const selected = form.id === timeType.id;
              return (
                <Box
                  key={timeType.id}
                  onClick={() => selectTimeType(timeType)}
                  sx={{
                    mx: 1,
                    mb: 0.75,
                    p: 1.25,
                    borderRadius: 1.5,
                    cursor: "pointer",
                    border: "1px solid",
                    borderColor: selected ? "primary.main" : "divider",
                    bgcolor: selected ? "primary.50" : "background.paper",
                    "&:hover": { borderColor: "primary.main", bgcolor: "action.hover" },
                  }}
                >
                  <Stack direction="row" justifyContent="space-between" spacing={1}>
                    <Box sx={{ minWidth: 0 }}>
                      <Typography fontWeight={850} noWrap>{timeType.code} - {timeType.name}</Typography>
                      <Typography variant="caption" color="text.secondary" noWrap>
                        {timeType.category || "No category"} · factor {timeType.factor ?? 1}
                      </Typography>
                    </Box>
                    <Chip size="small" color={timeType.paid ? "success" : "warning"} label={timeType.paid ? "Paid" : "Unpaid"} />
                  </Stack>
                </Box>
              );
            })}
            {filteredTypes.length === 0 && (
              <Typography variant="body2" color="text.secondary" sx={{ px: 2, py: 1 }}>
                No time types found.
              </Typography>
            )}
          </Stack>
        </Paper>
      </Grid>

      <Grid item xs={12} lg={8.6}>
        <Stack spacing={2}>
          <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
            <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
              <Box>
                <Typography variant="h6">{form.id ? `Edit ${form.code}` : "New time type"}</Typography>
                <Typography variant="body2" color="text.secondary">
                  Define how a day is classified before connecting it to payroll components.
                </Typography>
              </Box>
              <Stack direction="row" spacing={1}>
                {form.id && (
                  <Button color="error" startIcon={<DeleteIcon />} onClick={() => form.id && del.mutate(form.id)} disabled={del.isPending}>
                    Delete
                  </Button>
                )}
                {form.id && <Button startIcon={<RestartAltIcon />} onClick={resetForm}>Clear</Button>}
                <Button startIcon={<SaveIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                  {form.id ? "Update" : "Add"}
                </Button>
              </Stack>
            </Stack>
            <Grid container spacing={1.5}>
              <Grid item xs={6} md={2}>
                <TextField fullWidth label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField fullWidth label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </Grid>
              <Grid item xs={6} md={2}>
                <TextField fullWidth label="Category" value={form.category ?? ""} onChange={(e) => setForm({ ...form, category: e.target.value.toUpperCase() })} />
              </Grid>
              <Grid item xs={6} md={2}>
                <TextField fullWidth type="number" label="Factor" value={form.factor ?? 1} onChange={(e) => setForm({ ...form, factor: Number(e.target.value) })} />
              </Grid>
              <Grid item xs={6} md={2}>
                <TextField fullWidth type="number" label="Sort" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: Number(e.target.value) })} />
              </Grid>
              <Grid item xs={12}>
                <Stack direction="row" spacing={2} flexWrap="wrap">
                  <FormControlLabel control={<Checkbox checked={form.paid} onChange={(e) => setForm({ ...form, paid: e.target.checked })} />} label="Paid" />
                  <FormControlLabel control={<Checkbox checked={form.countsAsWorked} onChange={(e) => setForm({ ...form, countsAsWorked: e.target.checked })} />} label="Counts as worked" />
                  <FormControlLabel control={<Checkbox checked={form.affectsLeave} onChange={(e) => setForm({ ...form, affectsLeave: e.target.checked })} />} label="Affects leave" />
                </Stack>
              </Grid>
            </Grid>
          </Paper>

          {form.id ? (
            <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
              <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" spacing={1.5} mb={2}>
                <Box>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <PaymentsIcon color="primary" />
                    <Typography variant="h6">Payroll effect</Typography>
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    Select one payroll component, choose PAY/DEDUCT/IGNORE, then save. Existing rules are listed below.
                  </Typography>
                </Box>
                <Button variant="outlined" disabled={initializeDefaults.isPending} onClick={() => initializeDefaults.mutate()}>
                  Initialize default rules
                </Button>
              </Stack>

              <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 1.5, bgcolor: "background.default", mb: 2 }}>
                <Grid container spacing={1.5}>
                  <Grid item xs={12} md={4}>
                    <TextField
                      select
                      fullWidth
                      label="Payroll Component"
                      value={ruleForm.payrollComponentId}
                      onChange={(e) => setRuleForm({ ...ruleForm, timeTypeId: form.id!, payrollComponentId: e.target.value })}
                    >
                      {components.map((component: PayrollComponent) => (
                        <MenuItem key={component.id} value={component.id!}>
                          {component.code} - {component.name}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={6} md={2}>
                    <TextField select fullWidth label="Action" value={ruleForm.action} onChange={(e) => setRuleForm({ ...ruleForm, action: e.target.value })}>
                      <MenuItem value="PAY">Pay</MenuItem>
                      <MenuItem value="DEDUCT">Deduct</MenuItem>
                      <MenuItem value="SUSPEND">Suspend</MenuItem>
                      <MenuItem value="IGNORE">Ignore</MenuItem>
                    </TextField>
                  </Grid>
                  <Grid item xs={6} md={2}>
                    <TextField fullWidth type="number" label="Percent" value={ruleForm.percent} onChange={(e) => setRuleForm({ ...ruleForm, percent: Number(e.target.value) })} />
                  </Grid>
                  <Grid item xs={6} md={2}>
                    <TextField select fullWidth label="Basis" value={ruleForm.basis} onChange={(e) => setRuleForm({ ...ruleForm, basis: e.target.value })}>
                      <MenuItem value="HOURS">Hours</MenuItem>
                      <MenuItem value="PLANNED_SHIFT">Planned shift</MenuItem>
                      <MenuItem value="SHORTAGE">Shortage</MenuItem>
                      <MenuItem value="DAYS">Days</MenuItem>
                      <MenuItem value="FIXED">Fixed</MenuItem>
                    </TextField>
                  </Grid>
                  <Grid item xs={6} md={2}>
                    <TextField fullWidth type="number" label="Sort" value={ruleForm.sortOrder} onChange={(e) => setRuleForm({ ...ruleForm, sortOrder: Number(e.target.value) })} />
                  </Grid>
                  <Grid item xs={6} md={2}>
                    <TextField fullWidth type="number" label="After N days" value={ruleForm.thresholdDays ?? 0} onChange={(e) => setRuleForm({ ...ruleForm, thresholdDays: Number(e.target.value) })} helperText="0 = immediate" />
                  </Grid>
                  <Grid item xs={6} md={2}>
                    <TextField select fullWidth label="Count" value={ruleForm.thresholdScope ?? "NONE"} onChange={(e) => setRuleForm({ ...ruleForm, thresholdScope: e.target.value })}>
                      <MenuItem value="NONE">No threshold</MenuItem>
                      <MenuItem value="CONSECUTIVE">Consecutive</MenuItem>
                      <MenuItem value="ANNUAL">Annual</MenuItem>
                    </TextField>
                  </Grid>
                  <Grid item xs={6} md={2}>
                    <TextField select fullWidth label="Year from" value={ruleForm.yearBasis ?? "CALENDAR"} onChange={(e) => setRuleForm({ ...ruleForm, yearBasis: e.target.value })}>
                      <MenuItem value="CALENDAR">January</MenuItem>
                      <MenuItem value="HIRE_DATE">Hire date</MenuItem>
                    </TextField>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Stack direction="row" spacing={2} flexWrap="wrap">
                      <FormControlLabel control={<Checkbox checked={ruleForm.affectsOvertime} onChange={(e) => setRuleForm({ ...ruleForm, affectsOvertime: e.target.checked })} />} label="Affects overtime" />
                      <FormControlLabel control={<Checkbox checked={ruleForm.processSeparately} onChange={(e) => setRuleForm({ ...ruleForm, processSeparately: e.target.checked })} />} label="Process separately" />
                    </Stack>
                  </Grid>
                  <Grid item xs={12}>
                    <TextField fullWidth label="Remarks" value={ruleForm.remarks ?? ""} onChange={(e) => setRuleForm({ ...ruleForm, remarks: e.target.value })} />
                  </Grid>
                  <Grid item xs={12}>
                    <Stack direction="row" spacing={1}>
                      <Button variant="contained" disabled={!ruleForm.payrollComponentId || saveRule.isPending} onClick={() => saveRule.mutate(ruleForm)}>
                        {ruleForm.id ? "Update rule" : "Save rule"}
                      </Button>
                      {ruleForm.id && <Button onClick={() => setRuleForm({ ...EMPTY_RULE, timeTypeId: selectedTimeTypeId })}>Cancel edit</Button>}
                    </Stack>
                  </Grid>
                </Grid>
              </Paper>

              <Stack spacing={1}>
                {rules.map((rule) => (
                  <Paper key={rule.id ?? rule.payrollComponentId} variant="outlined" sx={{ p: 1.25, borderRadius: 1.5 }}>
                    <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", sm: "center" }} spacing={1}>
                      <Box>
                        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                          <Typography fontWeight={850}>{rule.payrollComponentCode} - {rule.payrollComponentName}</Typography>
                          <Chip size="small" color={actionColor(rule.action)} label={rule.action} />
                          <Chip size="small" label={`${rule.percent}%`} />
                          <Chip size="small" label={rule.basis} />
                        </Stack>
                        <Typography variant="caption" color="text.secondary">
                          {rule.thresholdDays ? `after ${rule.thresholdDays} day(s), ${rule.thresholdScope}` : "immediate"}
                          {rule.affectsOvertime ? " · affects OT" : ""}{rule.processSeparately ? " · separate" : ""}
                        </Typography>
                      </Box>
                      <Stack direction="row" spacing={0.5}>
                        <Button size="small" startIcon={<EditIcon />} onClick={() => setRuleForm({ ...rule, timeTypeId: selectedTimeTypeId })}>Edit</Button>
                        <IconButton size="small" color="error" onClick={() => deleteRule.mutate(rule.payrollComponentId)}><DeleteIcon /></IconButton>
                      </Stack>
                    </Stack>
                  </Paper>
                ))}
                {rules.length === 0 && (
                  <Typography variant="body2" color="text.secondary">No payroll rules for this time type yet.</Typography>
                )}
              </Stack>
            </Paper>
          ) : (
            <Paper variant="outlined" sx={{ p: 3, borderRadius: 2, textAlign: "center" }}>
              <EventBusyIcon color="disabled" sx={{ fontSize: 42, mb: 1 }} />
              <Typography fontWeight={850}>Save or select a time type to manage payroll rules.</Typography>
              <Typography variant="body2" color="text.secondary">Payroll effects stay hidden until a time type is selected.</Typography>
            </Paper>
          )}
        </Stack>
      </Grid>
    </Grid>
  );
}

function HolidaysSection() {
  const qc = useQueryClient();
  const { data = [] } = useQuery({ queryKey: ["publicHolidays"], queryFn: publicHolidayApi.list });
  const [form, setForm] = useState<PublicHoliday>({ holidayDate: "", name: "" });

  const save = useMutation({
    mutationFn: (h: PublicHoliday) => (h.id ? publicHolidayApi.update(h.id, h) : publicHolidayApi.create(h)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["publicHolidays"] }); setForm({ holidayDate: "", name: "" }); },
  });
  const del = useMutation({
    mutationFn: (id: string) => publicHolidayApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["publicHolidays"] }),
  });

  return (
    <Stack spacing={2}>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Typography variant="h6" mb={1}>Public holidays</Typography>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} md={3}><TextField fullWidth type="date" label="Date" InputLabelProps={{ shrink: true }} value={form.holidayDate} onChange={(e) => setForm({ ...form, holidayDate: e.target.value })} /></Grid>
          <Grid item xs={12} md={6}><TextField fullWidth label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={12} md={3}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<SaveIcon />} variant="contained" disabled={!form.holidayDate || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm({ holidayDate: "", name: "" })}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "hidden" }}>
        {data.map((h) => (
          <Stack key={h.id} direction="row" alignItems="center" justifyContent="space-between" sx={{ p: 1.5, borderBottom: "1px solid", borderColor: "divider" }}>
            <Box>
              <Typography fontWeight={800}>{h.holidayDate}</Typography>
              <Typography variant="body2" color="text.secondary">{h.name}</Typography>
            </Box>
            <Stack direction="row" spacing={0.5}>
              <Button size="small" startIcon={<EditIcon />} onClick={() => setForm(h)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => h.id && del.mutate(h.id)}><DeleteIcon /></IconButton>
            </Stack>
          </Stack>
        ))}
        {data.length === 0 && <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>No holidays yet.</Typography>}
      </Paper>
    </Stack>
  );
}

export default function TimeSetupPage() {
  const [tab, setTab] = useState(0);
  return (
    <Box>
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" spacing={1.5} mb={1.5}>
        <Box>
          <Typography variant="h5">Time Setup</Typography>
          <Typography variant="body2" color="text.secondary">
            Configure day types and public holidays without mixing payroll rules into the main list.
          </Typography>
        </Box>
      </Stack>
      <Paper variant="outlined" sx={{ borderRadius: 2, mb: 2 }}>
        <Tabs value={tab} onChange={(_e, value) => setTab(value)} variant="scrollable" scrollButtons="auto">
          <Tab label="Time Types" />
          <Tab label="Public Holidays" />
        </Tabs>
      </Paper>
      {tab === 0 && <TimeTypesSection />}
      {tab === 1 && <HolidaysSection />}
    </Box>
  );
}
