import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControlLabel,
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
import { payrollRuleApi, projectApi } from "../api/resources";
import type { PayrollRule } from "../api/types";

const BASIS = [
  { value: "FIXED_AMOUNT", label: "Fixed amount" },
  { value: "DAILY_RATE", label: "Daily rate x payable days" },
];

export default function PayrollRulesPage() {
  const qc = useQueryClient();
  const { data: rules = [] } = useQuery({ queryKey: ["payrollRules"], queryFn: payrollRuleApi.list });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const [drafts, setDrafts] = useState<Record<string, PayrollRule>>({});
  const [projectId, setProjectId] = useState<string>("");  // "" = default (all projects)

  const createRule = useMutation({
    mutationFn: (payload: PayrollRule) => payrollRuleApi.create(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["payrollRules"] }),
  });

  const shownRules = rules.filter((r) => (r.projectId ?? "") === projectId);
  const cloneForProject = () => {
    const defaults = rules.filter((r) => !r.projectId);
    defaults.forEach((d) => {
      const { id, ...rest } = d;
      createRule.mutate({ ...(rest as PayrollRule), projectId });
    });
  };

  const save = useMutation({
    mutationFn: (rule: PayrollRule) => payrollRuleApi.update(rule.id!, rule),
    onSuccess: () => {
      setDrafts({});
      qc.invalidateQueries({ queryKey: ["payrollRules"] });
    },
  });

  const value = (rule: PayrollRule) => drafts[rule.id ?? ""] ?? rule;
  const set = (rule: PayrollRule, patch: Partial<PayrollRule>) => {
    setDrafts((d) => ({ ...d, [rule.id ?? ""]: { ...value(rule), ...patch } }));
  };

  return (
    <Box>
      <Typography variant="h5" mb={2}>Payroll Rules</Typography>
      <Stack direction="row" spacing={1.5} mb={2} alignItems="center">
        <TextField select size="small" label="Project" value={projectId} onChange={(e) => setProjectId(e.target.value)} sx={{ minWidth: 260 }}>
          <MenuItem value="">Default (all projects)</MenuItem>
          {projects.map((p: { id?: string; code?: string; name?: string }) => (
            <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>
          ))}
        </TextField>
        {projectId && shownRules.length === 0 && (
          <Button variant="contained" disabled={createRule.isPending} onClick={cloneForProject}>
            Create payroll structure for this project
          </Button>
        )}
      </Stack>
      {projectId && shownRules.length === 0 ? (
        <Alert severity="info">This project uses the default payroll structure. Click the button above to give it its own settings.</Alert>
      ) : (
      <Paper variant="outlined" sx={{ borderRadius: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Pay group</TableCell>
              <TableCell>Pay item basis</TableCell>
              <TableCell>Weekly rest paid</TableCell>
              <TableCell align="right">OT multiplier</TableCell>
              <TableCell align="right">Rest day OT</TableCell>
              <TableCell align="right">Hours / day</TableCell>
              <TableCell align="right">Month divisor</TableCell>
              <TableCell>Divisor mode</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {shownRules.map((rule) => {
              const row = value(rule);
              return (
                <TableRow key={rule.id}>
                  <TableCell>{rule.payGroup}</TableCell>
                  <TableCell>
                    <TextField select size="small" fullWidth value={row.payItemBasis} onChange={(e) => set(rule, { payItemBasis: e.target.value })}>
                      {BASIS.map((b) => <MenuItem key={b.value} value={b.value}>{b.label}</MenuItem>)}
                    </TextField>
                  </TableCell>
                  <TableCell>
                    <FormControlLabel
                      control={<Checkbox checked={!!row.weeklyRestPaid} onChange={(e) => set(rule, { weeklyRestPaid: e.target.checked })} />}
                      label="Paid"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <TextField size="small" type="number" value={row.otMultiplier} onChange={(e) => set(rule, { otMultiplier: Number(e.target.value) })} inputProps={{ step: "0.01" }} />
                  </TableCell>
                  <TableCell align="right">
                    <TextField size="small" type="number" value={row.restDayOtMultiplier} onChange={(e) => set(rule, { restDayOtMultiplier: Number(e.target.value) })} inputProps={{ step: "0.01" }} />
                  </TableCell>
                  <TableCell align="right">
                    <TextField size="small" type="number" value={row.standardHoursPerDay} onChange={(e) => set(rule, { standardHoursPerDay: Number(e.target.value) })} inputProps={{ step: "0.25" }} />
                  </TableCell>
                  <TableCell align="right">
                    <TextField size="small" type="number" value={row.monthDivisor} onChange={(e) => set(rule, { monthDivisor: Number(e.target.value) })} inputProps={{ step: "1" }} />
                  </TableCell>
                  <TableCell>
                    <TextField select size="small" fullWidth value={row.divisorMode ?? "FIXED"} onChange={(e) => set(rule, { divisorMode: e.target.value })}>
                      <MenuItem value="FIXED">Fixed value</MenuItem>
                      <MenuItem value="ACTUAL_MONTH">By month days</MenuItem>
                    </TextField>
                  </TableCell>
                  <TableCell align="right">
                    <Button size="small" variant="contained" disabled={!drafts[rule.id ?? ""] || save.isPending} onClick={() => save.mutate(row)}>Save</Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </Paper>
      )}

      <Paper variant="outlined" sx={{ borderRadius: 2, p: 2, mt: 2 }}>
        <Typography variant="subtitle1" gutterBottom>How pay is calculated</Typography>
        <Typography variant="body2" component="div" color="text.secondary">
          <b>Monthly employee:</b> the salary is fixed by the month divisor.<br />
          &nbsp;&nbsp;Hourly rate = Salary ÷ (Month&nbsp;divisor × Shift&nbsp;hours)<br />
          &nbsp;&nbsp;Base pay = full divisor amount, reduced only by unpaid days.<br />
          <b>Daily employee:</b> pay = worked days × daily rate.<br />
          &nbsp;&nbsp;Hourly rate = Daily&nbsp;rate ÷ Shift&nbsp;hours<br />
          <b>Overtime</b> = Hourly&nbsp;rate × OT&nbsp;hours × OT&nbsp;multiplier<br />
          <b>Net</b> = Gross − Total&nbsp;deductions<br /><br />
          <b>Divisor mode:</b> “Fixed value” always uses the divisor above (e.g. 30) so pay is
          the same every month; “By month days” uses the real number of days in the month.<br />
          <b>Shift hours</b> come from each employee’s assigned shift.
        </Typography>
        <Typography variant="subtitle2" sx={{ mt: 2 }} gutterBottom>Variables available (database)</Typography>
        <Typography variant="body2" component="div" color="text.secondary">
          payroll_rule.month_divisor, payroll_rule.divisor_mode, payroll_rule.ot_multiplier,
          payroll_rule.rest_day_ot_multiplier, payroll_rule.weekly_rest_paid ·
          payroll_result.daily_rate, hourly_rate, worked_days, normal_hours, ot_hours,
          gross, total_deductions, net · shift.standard_hours (employee’s shift)
        </Typography>
      </Paper>
      {save.isError && (
        <Alert severity="error" sx={{ mt: 2 }}>{(save.error as any)?.response?.data?.message ?? "Could not save payroll rule."}</Alert>
      )}
      <Stack mt={2}>
        <Typography variant="body2" color="text.secondary">
          Changes apply when you calculate or recalculate a payroll run.
        </Typography>
      </Stack>
    </Box>
  );
}
