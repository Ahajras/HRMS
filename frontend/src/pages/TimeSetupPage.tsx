import { useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  Divider,
  FormControlLabel,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/DeleteOutline";
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

function TimeTypesSection() {
  const qc = useQueryClient();
  const { data: timeTypes = [] } = useQuery({ queryKey: ["timeTypes"], queryFn: timeTypeApi.list });
  const { data: components = [] } = useQuery({ queryKey: ["payrollComponents"], queryFn: () => payrollComponentApi.list() });
  const [form, setForm] = useState<TimeType>(EMPTY_TYPE);
  const [ruleForm, setRuleForm] = useState<TimeTypePayrollRule>(EMPTY_RULE);

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

  const selectedTimeTypeId = form.id ?? "";
  const { data: rules = [] } = useQuery({
    queryKey: ["timeTypePayrollRules", selectedTimeTypeId],
    queryFn: () => timeTypePayrollRuleApi.list(selectedTimeTypeId),
    enabled: !!selectedTimeTypeId,
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

  return (
    <Box mb={4}>
      <Typography variant="h6" mb={1.5}>Time Types</Typography>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle2" gutterBottom>{form.id ? "Edit time type" : "Add time type"}</Typography>
        <Grid container spacing={1.5}>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} />
          </Grid>
          <Grid item xs={6} sm={3}>
            <TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" label="Category" value={form.category ?? ""} onChange={(e) => setForm({ ...form, category: e.target.value.toUpperCase() })} />
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" type="number" label="Factor" value={form.factor ?? 1} onChange={(e) => setForm({ ...form, factor: Number(e.target.value) })} />
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField fullWidth size="small" type="number" label="Sort" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: Number(e.target.value) })} />
          </Grid>
          <Grid item xs={12}>
            <Stack direction="row" spacing={2} flexWrap="wrap">
              <FormControlLabel control={<Checkbox checked={form.paid} onChange={(e) => setForm({ ...form, paid: e.target.checked })} />} label="Paid" />
              <FormControlLabel control={<Checkbox checked={form.countsAsWorked} onChange={(e) => setForm({ ...form, countsAsWorked: e.target.checked })} />} label="Counts as worked" />
              <FormControlLabel control={<Checkbox checked={form.affectsLeave} onChange={(e) => setForm({ ...form, affectsLeave: e.target.checked })} />} label="Affects leave" />
            </Stack>
          </Grid>
          <Grid item xs={12}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.code || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => { setForm(EMPTY_TYPE); setRuleForm(EMPTY_RULE); }}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      {form.id && (
        <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
          <Typography variant="subtitle2" gutterBottom>Payroll Effect for {form.code}</Typography>
          <Typography variant="body2" color="text.secondary" mb={1.5}>
            Tell the system which payroll component this time type pays, deducts, or ignores.
          </Typography>
          <Stack direction="row" spacing={1} mb={1.5}>
            <Button variant="outlined" disabled={initializeDefaults.isPending} onClick={() => initializeDefaults.mutate()}>
              Initialize default rules
            </Button>
            <Typography variant="caption" color="text.secondary" sx={{ alignSelf: "center" }}>
              Adds missing payroll components as PAY, except U is initialized as DEDUCT. Review and adjust special cases before payroll calculation.
            </Typography>
          </Stack>
          <Grid container spacing={1.5}>
            <Grid item xs={12} sm={4}>
              <TextField
                select
                fullWidth
                size="small"
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
            <Grid item xs={6} sm={2}>
              <TextField select fullWidth size="small" label="Action" value={ruleForm.action} onChange={(e) => setRuleForm({ ...ruleForm, action: e.target.value })}>
                <MenuItem value="PAY">Pay</MenuItem>
                <MenuItem value="DEDUCT">Deduct</MenuItem>
                <MenuItem value="SUSPEND">Suspend (stop)</MenuItem>
                <MenuItem value="IGNORE">Ignore</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField fullWidth size="small" type="number" label="Percent" value={ruleForm.percent} onChange={(e) => setRuleForm({ ...ruleForm, percent: Number(e.target.value) })} />
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField select fullWidth size="small" label="Basis" value={ruleForm.basis} onChange={(e) => setRuleForm({ ...ruleForm, basis: e.target.value })}>
                <MenuItem value="HOURS">Hours</MenuItem>
                <MenuItem value="SHORTAGE">Shortage (planned - worked)</MenuItem>
                <MenuItem value="DAYS">Days</MenuItem>
                <MenuItem value="FIXED">Fixed</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField fullWidth size="small" type="number" label="Sort" value={ruleForm.sortOrder} onChange={(e) => setRuleForm({ ...ruleForm, sortOrder: Number(e.target.value) })} />
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField fullWidth size="small" type="number" label="After N days" value={ruleForm.thresholdDays ?? 0} onChange={(e) => setRuleForm({ ...ruleForm, thresholdDays: Number(e.target.value) })} helperText="0 = immediate" />
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField select fullWidth size="small" label="Count" value={ruleForm.thresholdScope ?? "NONE"} onChange={(e) => setRuleForm({ ...ruleForm, thresholdScope: e.target.value })}>
                <MenuItem value="NONE">No threshold</MenuItem>
                <MenuItem value="CONSECUTIVE">Consecutive</MenuItem>
                <MenuItem value="ANNUAL">Annual</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={6} sm={2}>
              <TextField select fullWidth size="small" label="Year from" value={ruleForm.yearBasis ?? "CALENDAR"} onChange={(e) => setRuleForm({ ...ruleForm, yearBasis: e.target.value })}>
                <MenuItem value="CALENDAR">January</MenuItem>
                <MenuItem value="HIRE_DATE">Hire date</MenuItem>
              </TextField>
            </Grid>
            <Grid item xs={12}>
              <Stack direction="row" spacing={2} flexWrap="wrap">
                <FormControlLabel control={<Checkbox checked={ruleForm.affectsOvertime} onChange={(e) => setRuleForm({ ...ruleForm, affectsOvertime: e.target.checked })} />} label="Affects overtime" />
                <FormControlLabel control={<Checkbox checked={ruleForm.processSeparately} onChange={(e) => setRuleForm({ ...ruleForm, processSeparately: e.target.checked })} />} label="Process separately" />
              </Stack>
            </Grid>
            <Grid item xs={12}>
              <TextField fullWidth size="small" label="Remarks" value={ruleForm.remarks ?? ""} onChange={(e) => setRuleForm({ ...ruleForm, remarks: e.target.value })} />
            </Grid>
            <Grid item xs={12}>
              <Stack direction="row" spacing={1}>
                <Button variant="contained" disabled={!ruleForm.payrollComponentId || saveRule.isPending} onClick={() => saveRule.mutate(ruleForm)}>
                  {ruleForm.id ? "Update Payroll Rule" : "Save Payroll Rule"}
                </Button>
                {ruleForm.id && (
                  <Button onClick={() => setRuleForm({ ...EMPTY_RULE, timeTypeId: selectedTimeTypeId })}>
                    Cancel edit
                  </Button>
                )}
              </Stack>
            </Grid>
          </Grid>
          <Divider sx={{ my: 2 }} />
          <Stack spacing={1}>
            {rules.map((rule) => (
              <Paper key={rule.id ?? rule.payrollComponentId} variant="outlined" sx={{ p: 1.25, borderRadius: 2 }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={2}>
                  <Box>
                    <Typography fontWeight={600}>{rule.payrollComponentCode} - {rule.payrollComponentName}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {rule.action} · {rule.percent}% · {rule.basis}{rule.thresholdDays ? ` · after ${rule.thresholdDays}d (${rule.thresholdScope})` : ""}{rule.affectsOvertime ? " · affects OT" : ""}{rule.processSeparately ? " · separate" : ""}
                    </Typography>
                  </Box>
                  <Stack direction="row" spacing={0.5}>
                    <Button size="small" onClick={() => setRuleForm({ ...rule, timeTypeId: selectedTimeTypeId })}>Edit</Button>
                    <IconButton size="small" color="error" onClick={() => deleteRule.mutate(rule.payrollComponentId)}>
                      <DeleteIcon />
                    </IconButton>
                  </Stack>
                </Stack>
              </Paper>
            ))}
            {rules.length === 0 && <Typography variant="body2" color="text.secondary">No payroll rules for this time type yet.</Typography>}
          </Stack>
        </Paper>
      )}

      {timeTypes.map((timeType) => (
        <Paper key={timeType.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography fontWeight={600}>{timeType.code} - {timeType.name}</Typography>
              <Typography variant="caption" color="text.secondary">
                factor {timeType.factor} · {timeType.paid ? "paid" : "unpaid"} · {timeType.countsAsWorked ? "worked" : "not worked"}{timeType.affectsLeave ? " · affects leave" : ""}
              </Typography>
            </Box>
            <Box>
              <Button size="small" onClick={() => { setForm(timeType); setRuleForm({ ...EMPTY_RULE, timeTypeId: timeType.id! }); }}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => timeType.id && del.mutate(timeType.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {timeTypes.length === 0 && <Typography variant="body2" color="text.secondary">No time types yet.</Typography>}
    </Box>
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
    <Box>
      <Typography variant="h6" mb={1.5}>Public Holidays</Typography>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={3}><TextField fullWidth size="small" type="date" label="Date" InputLabelProps={{ shrink: true }} value={form.holidayDate} onChange={(e) => setForm({ ...form, holidayDate: e.target.value })} /></Grid>
          <Grid item xs={12} sm={6}><TextField fullWidth size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Grid>
          <Grid item xs={12} sm={3}>
            <Stack direction="row" spacing={1}>
              <Button startIcon={<AddIcon />} variant="contained" disabled={!form.holidayDate || !form.name || save.isPending} onClick={() => save.mutate(form)}>
                {form.id ? "Update" : "Add"}
              </Button>
              {form.id && <Button onClick={() => setForm({ holidayDate: "", name: "" })}>Cancel</Button>}
            </Stack>
          </Grid>
        </Grid>
      </Paper>
      {data.map((h) => (
        <Paper key={h.id} variant="outlined" sx={{ p: 1.5, borderRadius: 2, mb: 1 }}>
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography fontWeight={600}>{h.holidayDate} - {h.name}</Typography>
            <Box>
              <Button size="small" onClick={() => setForm(h)}>Edit</Button>
              <IconButton size="small" color="error" onClick={() => h.id && del.mutate(h.id)}><DeleteIcon /></IconButton>
            </Box>
          </Stack>
        </Paper>
      ))}
      {data.length === 0 && <Typography variant="body2" color="text.secondary">No holidays yet.</Typography>}
    </Box>
  );
}

export default function TimeSetupPage() {
  return (
    <Box>
      <Typography variant="h5" mb={2}>Time Setup</Typography>
      <TimeTypesSection />
      <HolidaysSection />
    </Box>
  );
}
