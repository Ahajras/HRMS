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
import { payrollRuleApi } from "../api/resources";
import type { PayrollRule } from "../api/types";

const BASIS = [
  { value: "FIXED_AMOUNT", label: "Fixed amount" },
  { value: "DAILY_RATE", label: "Daily rate x payable days" },
];

export default function PayrollRulesPage() {
  const qc = useQueryClient();
  const { data: rules = [] } = useQuery({ queryKey: ["payrollRules"], queryFn: payrollRuleApi.list });
  const [drafts, setDrafts] = useState<Record<string, PayrollRule>>({});

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
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rules.map((rule) => {
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
                  <TableCell align="right">
                    <Button size="small" variant="contained" disabled={!drafts[rule.id ?? ""] || save.isPending} onClick={() => save.mutate(row)}>Save</Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
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
