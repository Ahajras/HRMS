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
import type { Employee } from "../api/types";

export default function DayZeroPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [selected, setSelected] = useState<Record<string, string>>({}); // dayId -> new time type id
  const [note, setNote] = useState("");
  const [message, setMessage] = useState<string | null>(null);

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
      setMessage(
        r.adjustmentsCreated > 0
          ? `Created ${r.adjustmentsCreated} pending adjustment(s). They will appear on the employee's next payroll run.`
          : "No pay difference — nothing to adjust (the new type pays the same as what was already assumed)."
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
        employee's next payslip.
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
          <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto", mb: 2 }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell padding="checkbox"></TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Period</TableCell>
                  <TableCell>Currently paid as</TableCell>
                  <TableCell>Correct to</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {days.map((d) => (
                  <TableRow key={d.id} hover selected={d.id in selected}>
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={d.id in selected}
                        onChange={(e) => {
                          setSelected((prev) => {
                            const next = { ...prev };
                            if (e.target.checked) next[d.id] = timeTypes[0]?.id ?? "";
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
                        disabled={!(d.id in selected)}
                        value={selected[d.id] ?? ""}
                        onChange={(e) => setSelected((prev) => ({ ...prev, [d.id]: e.target.value }))}
                        sx={{ minWidth: 180 }}
                      >
                        {timeTypes.map((t) => (
                          <MenuItem key={t.id} value={t.id}>{t.name} ({t.code})</MenuItem>
                        ))}
                      </TextField>
                    </TableCell>
                  </TableRow>
                ))}
                {days.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5}>
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
