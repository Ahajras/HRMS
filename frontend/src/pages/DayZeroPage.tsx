import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Checkbox,
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
import { dayZeroApi, employeeApi, timeTypeApi } from "../api/resources";
import type { DayZeroCorrection, Employee } from "../api/types";

export default function DayZeroPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [selected, setSelected] = useState<Record<string, DayZeroCorrection>>({}); // dayId -> correction
  const [note, setNote] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [lastLines, setLastLines] = useState<{ workDate: string; amount: number }[]>([]);

  const { data: matchesPage } = useQuery({
    queryKey: ["dayZeroEmployeeSearch", search],
    queryFn: () => employeeApi.list(0, 10, search),
    enabled: search.length >= 2 && !employee,
  });
  const matches = matchesPage?.content ?? [];

  const { data: days = [] } = useQuery({
    queryKey: ["dayZeroDays", employee?.id],
    queryFn: () => dayZeroApi.days(employee!.id!),
    enabled: !!employee?.id,
  });

  const { data: timeTypes = [] } = useQuery({ queryKey: ["timeTypes"], queryFn: () => timeTypeApi.list() });

  const apply = useMutation({
    mutationFn: () => dayZeroApi.correct(employee!.id!, selected, note || undefined),
    onSuccess: (r) => {
      setLastLines(r.lines ?? []);
      setMessage(
        r.adjustmentsCreated > 0
          ? `Created ${r.adjustmentsCreated} pending adjustment(s) — see the per-day breakdown below. They will appear on the employee's next payroll run.`
          : "No pay difference — nothing to adjust (the new type/hours pay the same as what was already assumed)."
      );
      setSelected({});
      qc.invalidateQueries({ queryKey: ["dayZeroDays", employee?.id] });
    },
  });

  return (
    <Box>
      <Typography variant="h5" mb={1}>Day Zero — Correct an Estimated Day</Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        For days that were paid on a default assumption because their project closed early. The locked period itself is never
        changed — correcting a day here recomputes the original month with the engine and queues the difference for the
        employee's next payslip. Change the day type (e.g. sick, unpaid), enter actual worked hours (e.g. the employee showed
        up on a rest day and earned overtime), or both.
      </Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        {!employee ? (
          <TextField
            fullWidth
            size="small"
            label="Search employee (name or number)"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        ) : (
          <Stack direction="row" spacing={2} alignItems="center">
            <Typography variant="subtitle1" fontWeight={700}>
              {employee.employeeNumber} — {employee.firstName} {employee.lastName}
            </Typography>
            <Button size="small" onClick={() => { setEmployee(null); setSearch(""); setSelected({}); setMessage(null); }}>
              Change employee
            </Button>
          </Stack>
        )}
        {!employee && search.length >= 2 && (
          <Paper variant="outlined" sx={{ mt: 1, maxHeight: 240, overflow: "auto" }}>
            {matches.map((e) => (
              <Box
                key={e.id}
                sx={{ p: 1, cursor: "pointer", "&:hover": { bgcolor: "action.hover" } }}
                onClick={() => { setEmployee(e); setSearch(""); }}
              >
                {e.employeeNumber} — {e.firstName} {e.lastName}
              </Box>
            ))}
            {matches.length === 0 && <Box sx={{ p: 1 }}><Typography variant="body2" color="text.secondary">No matches.</Typography></Box>}
          </Paper>
        )}
      </Paper>

      {employee && (
        <>
          {message && <Alert severity="info" sx={{ mb: 2 }} onClose={() => setMessage(null)}>{message}</Alert>}
          {lastLines.length > 0 && (
            <Paper variant="outlined" sx={{ mb: 2, p: 1.5, borderRadius: 2 }}>
              <Typography variant="subtitle2" mb={1}>Per-day amount (this is what to tell the employee)</Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell align="right">Amount</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {lastLines.map((l) => (
                    <TableRow key={l.workDate}>
                      <TableCell>{l.workDate}</TableCell>
                      <TableCell align="right" sx={{ color: l.amount < 0 ? "error.main" : "success.main" }}>
                        {l.amount < 0 ? "-" : "+"}{Math.abs(l.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
          )}
          <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto", mb: 2 }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell padding="checkbox"></TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Period</TableCell>
                  <TableCell>Currently paid as</TableCell>
                  <TableCell>Correct to (optional)</TableCell>
                  <TableCell>Worked hours (optional)</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {days.map((d) => {
                  const isSelected = d.id in selected;
                  const correction = selected[d.id] ?? {};
                  return (
                    <TableRow key={d.id} hover selected={isSelected}>
                      <TableCell padding="checkbox">
                        <Checkbox
                          checked={isSelected}
                          onChange={(e) => {
                            setSelected((prev) => {
                              const next = { ...prev };
                              if (e.target.checked) next[d.id] = {};
                              else delete next[d.id];
                              return next;
                            });
                          }}
                        />
                      </TableCell>
                      <TableCell>{d.workDate}</TableCell>
                      <TableCell>{d.periodYear}-{String(d.periodMonth).padStart(2, "0")}</TableCell>
                      <TableCell>{d.timeTypeName ?? d.timeTypeCode ?? ""}</TableCell>
                      <TableCell>
                        <TextField
                          select
                          size="small"
                          disabled={!isSelected}
                          value={correction.newTimeTypeId ?? ""}
                          onChange={(e) => setSelected((prev) => ({
                            ...prev, [d.id]: { ...prev[d.id], newTimeTypeId: e.target.value || undefined },
                          }))}
                          sx={{ minWidth: 180 }}
                        >
                          <MenuItem value="">(keep current type)</MenuItem>
                          {timeTypes.map((t) => (
                            <MenuItem key={t.id} value={t.id}>{t.name} ({t.code})</MenuItem>
                          ))}
                        </TextField>
                      </TableCell>
                      <TableCell>
                        <TextField
                          type="number"
                          size="small"
                          disabled={!isSelected}
                          placeholder="e.g. worked Friday"
                          value={correction.workedHours ?? ""}
                          onChange={(e) => setSelected((prev) => ({
                            ...prev, [d.id]: { ...prev[d.id], workedHours: e.target.value === "" ? undefined : Number(e.target.value) },
                          }))}
                          sx={{ width: 140 }}
                        />
                      </TableCell>
                    </TableRow>
                  );
                })}
                {days.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6}>
                      <Typography variant="body2" color="text.secondary" p={1}>
                        No estimated Day Zero days found for this employee (nothing locked past a configured cutoff yet).
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Paper>

          <Stack direction="row" spacing={2} alignItems="center">
            <TextField
              size="small"
              label="Note (optional)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              sx={{ minWidth: 320 }}
            />
            <Button
              variant="contained"
              disabled={Object.keys(selected).length === 0 || apply.isPending}
              onClick={() => apply.mutate()}
            >
              Apply correction ({Object.keys(selected).length} day{Object.keys(selected).length === 1 ? "" : "s"})
            </Button>
          </Stack>
        </>
      )}
    </Box>
  );
}
