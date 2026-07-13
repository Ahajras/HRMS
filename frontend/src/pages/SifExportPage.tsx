import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
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
import { periodApi, projectApi, sifApi, sponsorApi } from "../api/resources";
import type { Sponsor, SifFile, SifRow } from "../api/types";

function csvCell(v: string | number) {
  const s = String(v ?? "");
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

function downloadSifFile(file: SifFile) {
  const headerCols = [
    "Employer EID", "File Creation Date", "File Creation Time", "Payer EID", "Payer QID",
    "Payer Bank Short Name", "Payer IBAN", "Salary Year and Month", "Total Salaries", "Total Records",
  ];
  const now = new Date();
  const pad = (n: number, w = 2) => String(n).padStart(w, "0");
  const dateStr = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`;
  const timeStr = `${pad(now.getHours())}${pad(now.getMinutes())}`;
  const headerVals = [
    file.establishmentEid, dateStr, timeStr, file.establishmentEid, file.payerQid ?? "",
    file.payerBankCode, file.payerIban, `${file.periodYear}${pad(file.periodMonth)}`,
    file.totalSalaries, file.totalRecords,
  ];
  const detailCols = [
    "Record Sequence", "Employee QID", "Employee Visa ID", "Employee Name", "Employee Bank Short Name",
    "Employee Account", "Salary Frequency", "Number of Working days", "Net Salary", "Basic Salary",
    "Extra hours", "Extra income", "Deductions", "Payment Type", "Notes / Comments",
  ];
  const detailRows = file.rows.map((r) => [
    r.recordSequence, r.qid ?? "", r.visaId ?? "", r.employeeName, r.bankCode, r.bankAccount,
    r.salaryFrequency, r.workingDays, r.netSalary, r.basicSalary, r.extraHours, r.extraIncome,
    r.deductions, "", r.notes,
  ]);
  const csv = [
    headerCols.map(csvCell).join(","),
    headerVals.map(csvCell).join(","),
    detailCols.map(csvCell).join(","),
    ...detailRows.map((row) => row.map(csvCell).join(",")),
  ].join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${file.fileName}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function SponsorsPanel() {
  const qc = useQueryClient();
  const { data: sponsors = [] } = useQuery({ queryKey: ["sponsors"], queryFn: sponsorApi.list });
  const [form, setForm] = useState<Sponsor>({ code: "", name: "", establishmentEid: "", payerBankCode: "", payerIban: "" });

  const save = useMutation({
    mutationFn: (s: Sponsor) => sponsorApi.save(s),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["sponsors"] }); setForm({ code: "", name: "", establishmentEid: "", payerBankCode: "", payerIban: "" }); },
  });

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
      <Typography variant="subtitle1" fontWeight={700} mb={1}>Sponsors (WPS Employer Establishments)</Typography>
      <Table size="small" sx={{ mb: 2 }}>
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell><TableCell>Name</TableCell><TableCell>EID</TableCell>
            <TableCell>Payer Bank</TableCell><TableCell>Payer IBAN</TableCell><TableCell align="right"></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {sponsors.map((s) => (
            <TableRow key={s.id} hover>
              <TableCell>{s.code}</TableCell><TableCell>{s.name}</TableCell><TableCell>{s.establishmentEid}</TableCell>
              <TableCell>{s.payerBankCode}</TableCell><TableCell>{s.payerIban}</TableCell>
              <TableCell align="right"><Button size="small" onClick={() => setForm(s)}>Edit</Button></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap alignItems="center">
        <TextField size="small" label="Code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} sx={{ width: 100 }} />
        <TextField size="small" label="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} sx={{ width: 200 }} />
        <TextField size="small" label="Establishment EID" value={form.establishmentEid} onChange={(e) => setForm({ ...form, establishmentEid: e.target.value })} sx={{ width: 150 }} />
        <TextField size="small" label="Payer Bank Code" value={form.payerBankCode} onChange={(e) => setForm({ ...form, payerBankCode: e.target.value })} sx={{ width: 130 }} />
        <TextField size="small" label="Payer IBAN" value={form.payerIban} onChange={(e) => setForm({ ...form, payerIban: e.target.value })} sx={{ width: 240 }} />
        <TextField size="small" label="Payer QID (if individual)" value={form.payerQid ?? ""} onChange={(e) => setForm({ ...form, payerQid: e.target.value })} sx={{ width: 180 }} />
        <Button variant="contained" disabled={!form.code || !form.name || !form.establishmentEid || save.isPending} onClick={() => save.mutate(form)}>
          {form.id ? "Update" : "Add"}
        </Button>
      </Stack>
    </Paper>
  );
}

export default function SifExportPage() {
  const [periodId, setPeriodId] = useState("");
  const [selectedProjects, setSelectedProjects] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<import("../api/types").SifExportResult | null>(null);

  const { data: periods = [] } = useQuery({ queryKey: ["periodsForSif"], queryFn: () => periodApi.list() });
  const { data: projects = [] } = useQuery({ queryKey: ["projectsForSif"], queryFn: () => projectApi.list() });

  const generate = useMutation({
    mutationFn: () => sifApi.generate(periodId, selectedProjects),
    onSuccess: (data) => setResult(data),
    onError: (e: any) => setError(e?.response?.data?.message ?? "Could not generate — check that the payroll for this period is locked."),
  });

  const editRow = (fileIdx: number, rowIdx: number, notes: string) => {
    if (!result) return;
    const files = [...result.files];
    files[fileIdx] = { ...files[fileIdx], rows: [...files[fileIdx].rows] };
    files[fileIdx].rows[rowIdx] = { ...files[fileIdx].rows[rowIdx], notes };
    setResult({ ...result, files });
  };

  return (
    <Box>
      <Typography variant="h5" mb={1}>WPS / SIF Export</Typography>
      <Typography variant="body2" color="text.secondary" mb={2}>
        Generates Qatar's Wage Protection System salary file — one CSV per Sponsor (Employer Establishment). Cash-paid
        employees are excluded automatically. Review and adjust the Notes column below before downloading.
      </Typography>

      <SponsorsPanel />

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap" useFlexGap>
          <TextField select size="small" label="Period" value={periodId} onChange={(e) => setPeriodId(e.target.value)} sx={{ minWidth: 180 }}>
            {periods.map((p) => (
              <MenuItem key={p.id} value={p.id}>{p.periodYear}-{String(p.periodMonth).padStart(2, "0")}</MenuItem>
            ))}
          </TextField>
          <TextField
            select size="small" label="Projects (blank = all)" SelectProps={{ multiple: true }}
            value={selectedProjects} onChange={(e) => setSelectedProjects(e.target.value as unknown as string[])}
            sx={{ minWidth: 260 }}
          >
            {projects.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                <Checkbox size="small" checked={selectedProjects.includes(p.id!)} />
                {p.code} — {p.name}
              </MenuItem>
            ))}
          </TextField>
          <Button variant="contained" disabled={!periodId || generate.isPending} onClick={() => { setError(null); generate.mutate(); }}>
            Generate
          </Button>
        </Stack>
      </Paper>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      {result && result.exclusions.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          <Typography variant="subtitle2" mb={1}>{result.exclusions.length} employee(s) excluded — missing required data:</Typography>
          {result.exclusions.map((ex, i) => (
            <Typography key={i} variant="body2">{ex.employeeNumber} — {ex.employeeName}: {ex.reason}</Typography>
          ))}
        </Alert>
      )}

      {result?.files.map((file, fileIdx) => (
        <Paper key={file.sponsorCode} variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
            <Box>
              <Typography variant="subtitle1" fontWeight={700}>{file.sponsorName} ({file.sponsorCode})</Typography>
              <Typography variant="body2" color="text.secondary">
                EID {file.establishmentEid} · {file.totalRecords} records · Total {file.totalSalaries.toLocaleString()} QAR
              </Typography>
            </Box>
            <Button variant="contained" onClick={() => downloadSifFile(file)}>Download CSV</Button>
          </Stack>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>#</TableCell>
                <TableCell>Employee</TableCell>
                <TableCell>Project</TableCell>
                <TableCell align="right">Basic</TableCell>
                <TableCell align="right">Extra</TableCell>
                <TableCell align="right">Deductions</TableCell>
                <TableCell align="right">Net</TableCell>
                <TableCell>Notes</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {file.rows.map((row: SifRow, rowIdx: number) => (
                <TableRow key={row.employeeId} hover>
                  <TableCell>{row.recordSequence}</TableCell>
                  <TableCell>{row.employeeNumber} — {row.employeeName}</TableCell>
                  <TableCell><Chip size="small" label={row.projectCode ?? ""} /></TableCell>
                  <TableCell align="right">{row.basicSalary.toLocaleString()}</TableCell>
                  <TableCell align="right">{row.extraIncome.toLocaleString()}</TableCell>
                  <TableCell align="right">{row.deductions.toLocaleString()}</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>{row.netSalary.toLocaleString()}</TableCell>
                  <TableCell>
                    <TextField
                      select size="small" value={row.notes} onChange={(e) => editRow(fileIdx, rowIdx, e.target.value)} sx={{ minWidth: 150 }}
                    >
                      <MenuItem value="">(blank)</MenuItem>
                      <MenuItem value="Allowances">Allowances</MenuItem>
                      <MenuItem value="On Leave">On Leave</MenuItem>
                      <MenuItem value="Personal Loan">Personal Loan</MenuItem>
                    </TextField>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      ))}
    </Box>
  );
}
