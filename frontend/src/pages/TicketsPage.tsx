import { useState } from "react";
import {
  Alert,
  Box,
  Button,
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
import SaveIcon from "@mui/icons-material/Save";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { employeeApi, lookupApi, projectApi, ticketApi } from "../api/resources";
import type { ImportSummary, TicketFare, TicketLedger } from "../api/types";

const money = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const emptyFare = (): TicketFare => ({
  fromAirportCode: "",
  toAirportCode: "",
  amount: 0,
  currencyCode: "QAR",
  effectiveFrom: new Date().toISOString().slice(0, 10),
  status: "ACTIVE",
});

export default function TicketsPage() {
  const qc = useQueryClient();
  const [fare, setFare] = useState<TicketFare>(emptyFare());
  const [q, setQ] = useState("");
  const [employeeId, setEmployeeId] = useState("");
  const [asOfDate, setAsOfDate] = useState(new Date().toISOString().slice(0, 10));
  const [reportProjectId, setReportProjectId] = useState("");
  const [reportPayGroup, setReportPayGroup] = useState("ALL");
  const [importSummary, setImportSummary] = useState<ImportSummary | null>(null);
  const [ledger, setLedger] = useState<Partial<TicketLedger>>({
    entryType: "OPENING_USED",
    entryDate: new Date().toISOString().slice(0, 10),
    amount: 0,
    status: "ACTIVE",
  });

  const { data: fares = [] } = useQuery({ queryKey: ["ticketFares"], queryFn: ticketApi.fares });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: payGroups = [] } = useQuery({ queryKey: ["lookup", "PAY_STATUS"], queryFn: () => lookupApi.byCategory("PAY_STATUS") });
  const { data: accrualReport } = useQuery({
    queryKey: ["ticketAccrualReport", reportProjectId, reportPayGroup, asOfDate],
    queryFn: () => ticketApi.accrualReport({
      ...(reportProjectId ? { projectId: reportProjectId } : {}),
      ...(reportPayGroup ? { payGroup: reportPayGroup } : {}),
      asOfDate,
    }),
  });
  const { data: employees } = useQuery({
    queryKey: ["ticketEmployees", q],
    queryFn: () => employeeApi.list(0, 25, q || undefined),
  });
  const { data: balance } = useQuery({
    queryKey: ["ticketBalance", employeeId, asOfDate],
    queryFn: () => ticketApi.balance(employeeId, asOfDate),
    enabled: !!employeeId,
  });
  const { data: ledgerRows = [] } = useQuery({
    queryKey: ["ticketLedger", employeeId],
    queryFn: () => ticketApi.ledger(employeeId),
    enabled: !!employeeId,
  });

  const saveFare = useMutation({
    mutationFn: ticketApi.saveFare,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["ticketFares"] });
      qc.invalidateQueries({ queryKey: ["ticketBalance"] });
      setFare(emptyFare());
    },
  });
  const importFares = useMutation({
    mutationFn: ticketApi.importFares,
    onSuccess: (summary) => {
      qc.invalidateQueries({ queryKey: ["ticketFares"] });
      qc.invalidateQueries({ queryKey: ["ticketBalance"] });
      setImportSummary(summary);
    },
  });
  const saveLedger = useMutation({
    mutationFn: (payload: TicketLedger) => ticketApi.saveLedger(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["ticketLedger"] });
      qc.invalidateQueries({ queryKey: ["ticketBalance"] });
      setLedger({ entryType: "OPENING_USED", entryDate: new Date().toISOString().slice(0, 10), amount: 0, status: "ACTIVE" });
    },
  });

  const employeesList = employees?.content ?? [];

  return (
    <Box>
      <Typography variant="h5" mb={2}>Tickets</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Typography variant="subtitle1" fontWeight={800} mb={1}>Ticket fare reference</Typography>
        <Grid container spacing={1.5}>
          <Grid item xs={12} md={2}>
            <TextField fullWidth size="small" label="Work airport" value={fare.fromAirportCode} onChange={(e) => setFare({ ...fare, fromAirportCode: e.target.value.toUpperCase() })} />
          </Grid>
          <Grid item xs={12} md={2}>
            <TextField fullWidth size="small" label="Home airport" value={fare.toAirportCode} onChange={(e) => setFare({ ...fare, toAirportCode: e.target.value.toUpperCase() })} />
          </Grid>
          <Grid item xs={12} md={2}>
            <TextField fullWidth size="small" type="number" label="Amount" value={fare.amount} onChange={(e) => setFare({ ...fare, amount: Number(e.target.value) })} />
          </Grid>
          <Grid item xs={12} md={2}>
            <TextField fullWidth size="small" label="Currency" value={fare.currencyCode ?? ""} onChange={(e) => setFare({ ...fare, currencyCode: e.target.value.toUpperCase() })} />
          </Grid>
          <Grid item xs={12} md={2}>
            <TextField fullWidth size="small" type="date" label="Effective from" InputLabelProps={{ shrink: true }} value={fare.effectiveFrom} onChange={(e) => setFare({ ...fare, effectiveFrom: e.target.value })} />
          </Grid>
          <Grid item xs={12} md={2}>
            <Button fullWidth variant="contained" startIcon={<SaveIcon />} disabled={!fare.fromAirportCode || !fare.toAirportCode || saveFare.isPending} onClick={() => saveFare.mutate(fare)}>
              Save fare
            </Button>
          </Grid>
          <Grid item xs={12} md={4}>
            <Button component="label" fullWidth variant="outlined" startIcon={<UploadFileIcon />} disabled={importFares.isPending}>
              Import CSV fares
              <input hidden type="file" accept=".csv,text/csv" onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) importFares.mutate(file);
                e.currentTarget.value = "";
              }} />
            </Button>
          </Grid>
        </Grid>
        <Typography variant="caption" color="text.secondary" display="block" mt={1}>
          CSV columns: from_airport_code, to_airport_code, amount, currency_code, effective_from, effective_to, status, remarks
        </Typography>
        {importFares.error && <Alert severity="error" sx={{ mt: 2 }}>{(importFares.error as Error).message}</Alert>}
        {importSummary && (
          <Alert severity="success" sx={{ mt: 2 }}>
            Imported. Inserted: {importSummary.counts.ticket_fare_inserted ?? 0}, Updated: {importSummary.counts.ticket_fare_updated ?? 0}, Skipped: {importSummary.counts.ticket_fare_skipped ?? 0}
            {importSummary.warnings.length ? `, Warnings: ${importSummary.warnings.slice(0, 3).join(" | ")}` : ""}
          </Alert>
        )}
        <Box sx={{ overflow: "auto", mt: 2 }}>
          <Table size="small">
            <TableHead><TableRow><TableCell>From</TableCell><TableCell>To</TableCell><TableCell align="right">Amount</TableCell><TableCell>Currency</TableCell><TableCell>Effective</TableCell><TableCell>Source</TableCell><TableCell>Provider</TableCell><TableCell>Status</TableCell></TableRow></TableHead>
            <TableBody>
              {fares.map((f) => (
                <TableRow key={f.id} hover onClick={() => setFare(f)} sx={{ cursor: "pointer" }}>
                  <TableCell>{f.fromAirportCode}</TableCell><TableCell>{f.toAirportCode}</TableCell><TableCell align="right">{money(f.amount)}</TableCell><TableCell>{f.currencyCode}</TableCell><TableCell>{f.effectiveFrom}</TableCell><TableCell>{f.source ?? "MANUAL"}</TableCell><TableCell>{f.provider ?? ""}</TableCell><TableCell>{f.status}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
        <Typography variant="subtitle1" fontWeight={800} mb={1}>Employee ticket accrual</Typography>
        <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} mb={2}>
          <TextField select size="small" label="Project" value={reportProjectId} onChange={(e) => setReportProjectId(e.target.value)} sx={{ minWidth: 220 }}>
            <MenuItem value="">Whole company</MenuItem>
            {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} - {p.name}</MenuItem>)}
          </TextField>
          <TextField select size="small" label="Pay group" value={reportPayGroup} onChange={(e) => setReportPayGroup(e.target.value)} sx={{ minWidth: 160 }}>
            <MenuItem value="ALL">All</MenuItem>
            {payGroups.map((g) => <MenuItem key={g.code} value={g.code}>{g.label || g.code}</MenuItem>)}
          </TextField>
          <TextField size="small" type="date" label="As of" InputLabelProps={{ shrink: true }} value={asOfDate} onChange={(e) => setAsOfDate(e.target.value)} />
        </Stack>

        {accrualReport && (
          <>
            <Grid container spacing={1.5} mb={2}>
              <Stat label="Employees" value={String(accrualReport.employeeCount)} />
              <Stat label="Missing setup" value={String(accrualReport.missingSetupCount)} />
              <Stat label="Ticket amount" value={money(accrualReport.totalTicketAmount)} />
              <Stat label="Accrued" value={money(accrualReport.totalAccruedAmount)} />
              <Stat label="Used" value={money(accrualReport.totalUsedAmount)} />
              <Stat label="Balance" value={money(accrualReport.totalBalance)} />
            </Grid>
            <Box sx={{ overflow: "auto", mb: 2, maxHeight: 320 }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Employee</TableCell>
                    <TableCell>Route</TableCell>
                    <TableCell align="right">Ticket</TableCell>
                    <TableCell align="right">Accrued</TableCell>
                    <TableCell align="right">Used</TableCell>
                    <TableCell align="right">Balance</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {accrualReport.rows.slice(0, 200).map((row) => (
                    <TableRow key={row.employeeId} hover>
                      <TableCell>
                        <Typography variant="body2" fontWeight={700}>{row.employeeNumber}</Typography>
                        <Typography variant="caption" color="text.secondary">{row.employeeName}</Typography>
                      </TableCell>
                      <TableCell>{row.fromAirportCode ?? "-"} -&gt; {row.toAirportCode ?? "-"}</TableCell>
                      <TableCell align="right">{money(row.ticketAmount)}</TableCell>
                      <TableCell align="right">{money(row.accruedAmount)}</TableCell>
                      <TableCell align="right">{money(row.usedAmount)}</TableCell>
                      <TableCell align="right">{money(row.balance)}</TableCell>
                      <TableCell>{row.message ? <Typography color="error" variant="caption">{row.message}</Typography> : "OK"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </>
        )}

        <Typography variant="subtitle2" fontWeight={800} mb={1}>Single employee check</Typography>
        <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} mb={2}>
          <TextField size="small" label="Search employee" value={q} onChange={(e) => setQ(e.target.value)} sx={{ minWidth: 220 }} />
          <TextField select size="small" label="Employee" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} sx={{ minWidth: 320 }}>
            <MenuItem value="">Select employee</MenuItem>
            {employeesList.map((e) => <MenuItem key={e.id} value={e.id}>{e.employeeNumber} - {e.firstName} {e.lastName}</MenuItem>)}
          </TextField>
        </Stack>

        {balance?.message && <Alert severity="warning" sx={{ mb: 2 }}>{balance.message}</Alert>}
        {balance && (
          <Grid container spacing={1.5} mb={2}>
            <Stat label="Route" value={`${balance.fromAirportCode ?? "-"} -> ${balance.toAirportCode ?? "-"}`} />
            <Stat label="Ticket amount" value={money(balance.ticketAmount)} />
            <Stat label="Accrued months" value={String(balance.accruedMonths)} />
            <Stat label="Accrued" value={money(balance.accruedAmount)} />
            <Stat label="Used" value={money(balance.usedAmount)} />
            <Stat label="Balance" value={money(balance.balance)} />
          </Grid>
        )}

        {employeeId && (
          <>
            <Grid container spacing={1.5} mb={2}>
              <Grid item xs={12} md={2}>
                <TextField select fullWidth size="small" label="Entry type" value={ledger.entryType ?? "OPENING_USED"} onChange={(e) => setLedger({ ...ledger, entryType: e.target.value })}>
                  <MenuItem value="OPENING_USED">Opening used</MenuItem>
                  <MenuItem value="OPENING_ACCRUED">Opening accrued</MenuItem>
                  <MenuItem value="MANUAL_DEBIT">Manual debit</MenuItem>
                  <MenuItem value="MANUAL_CREDIT">Manual credit</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} md={2}>
                <TextField fullWidth size="small" type="date" label="Date" InputLabelProps={{ shrink: true }} value={ledger.entryDate ?? ""} onChange={(e) => setLedger({ ...ledger, entryDate: e.target.value })} />
              </Grid>
              <Grid item xs={12} md={2}>
                <TextField fullWidth size="small" type="number" label="Amount" value={ledger.amount ?? 0} onChange={(e) => setLedger({ ...ledger, amount: Number(e.target.value) })} />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField fullWidth size="small" label="Remarks" value={ledger.remarks ?? ""} onChange={(e) => setLedger({ ...ledger, remarks: e.target.value })} />
              </Grid>
              <Grid item xs={12} md={2}>
                <Button fullWidth variant="outlined" disabled={saveLedger.isPending} onClick={() => saveLedger.mutate({ ...(ledger as TicketLedger), employeeId })}>Save entry</Button>
              </Grid>
            </Grid>
            <Table size="small">
              <TableHead><TableRow><TableCell>Date</TableCell><TableCell>Type</TableCell><TableCell align="right">Amount</TableCell><TableCell>Route</TableCell><TableCell>Status</TableCell><TableCell>Remarks</TableCell></TableRow></TableHead>
              <TableBody>
                {ledgerRows.map((row) => (
                  <TableRow key={row.id}>
                    <TableCell>{row.entryDate}</TableCell><TableCell>{row.entryType}</TableCell><TableCell align="right">{money(row.amount)}</TableCell><TableCell>{row.fromAirportCode ?? ""} {row.toAirportCode ? `-> ${row.toAirportCode}` : ""}</TableCell><TableCell>{row.status}</TableCell><TableCell>{row.remarks}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </>
        )}
      </Paper>
    </Box>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <Grid item xs={6} md={2}>
      <Paper variant="outlined" sx={{ p: 1.25, borderRadius: 2 }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography fontWeight={800}>{value}</Typography>
      </Paper>
    </Grid>
  );
}
