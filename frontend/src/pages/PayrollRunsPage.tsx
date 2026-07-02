import { Fragment, useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
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
import PrintIcon from "@mui/icons-material/Print";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { payrollRunApi, periodApi, projectApi } from "../api/resources";
import type { PayrollResult, PayrollResultLine, PayrollRun } from "../api/types";

const STATUS_COLOR: Record<string, "default" | "info" | "success" | "warning"> = {
  DRAFT: "default",
  CALCULATED: "info",
  APPROVED: "success",
  LOCKED: "warning",
};

const money = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const qty = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 });

const periodLabel = (r: PayrollRun) => {
  if (r.periodName) return r.periodName;
  if (r.periodYear && r.periodMonth) return `${r.periodYear}/${String(r.periodMonth).padStart(2, "0")}`;
  return "Period";
};

export default function PayrollRunsPage() {
  const qc = useQueryClient();
  const [periodId, setPeriodId] = useState("");
  const [projectId, setProjectId] = useState("");
  const [payGroup, setPayGroup] = useState("ALL");
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const { data: periods = [] } = useQuery({ queryKey: ["periods"], queryFn: () => periodApi.list() });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: projectApi.list });
  const { data: runs = [] } = useQuery({
    queryKey: ["payrollRuns", periodId],
    queryFn: () => payrollRunApi.list(periodId || undefined),
  });
  const { data: detail } = useQuery({
    queryKey: ["payrollRun", selectedId],
    queryFn: () => payrollRunApi.get(selectedId!),
    enabled: !!selectedId,
  });

  useEffect(() => {
    if (!periodId && periods.length > 0) {
      setPeriodId(periods[0].id ?? "");
    }
  }, [periodId, periods]);

  const create = useMutation({
    mutationFn: () => payrollRunApi.create(periodId, projectId || undefined, payGroup),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["payrollRuns"] });
      setSelectedId(r.id ?? null);
    },
  });
  const action = useMutation({
    mutationFn: ({ id, fn }: { id: string; fn: "calculate" | "approve" | "lock" }) => payrollRunApi[fn](id),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["payrollRuns"] });
      qc.invalidateQueries({ queryKey: ["payrollRun", r.id] });
      setSelectedId(r.id ?? null);
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => payrollRunApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["payrollRuns"] });
      setSelectedId(null);
    },
  });

  return (
    <Box>
      <Typography variant="h5" mb={2}>Payroll Runs</Typography>

      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Grid container spacing={1.5} alignItems="center">
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Period" value={periodId} onChange={(e) => setPeriodId(e.target.value)}>
              <MenuItem value="">All periods</MenuItem>
              {periods.map((p) => <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField select fullWidth size="small" label="Project" value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              <MenuItem value="">Whole company</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>)}
            </TextField>
          </Grid>
          <Grid item xs={6} sm={2}>
            <TextField select fullWidth size="small" label="Pay group" value={payGroup} onChange={(e) => setPayGroup(e.target.value)}>
              <MenuItem value="ALL">All</MenuItem>
              <MenuItem value="DAILY">Daily</MenuItem>
              <MenuItem value="MONTHLY">Monthly</MenuItem>
            </TextField>
          </Grid>
          <Grid item xs={6} sm={2}>
            <Button variant="contained" disabled={!periodId || create.isPending} onClick={() => create.mutate()}>
              Create run
            </Button>
          </Grid>
        </Grid>
        {(create.isError || action.isError || remove.isError) && (
          <Alert severity="error" sx={{ mt: 1.5 }}>
            {((create.error || action.error || remove.error) as any)?.response?.data?.message ?? "Payroll action failed."}
          </Alert>
        )}
      </Paper>

      <Paper variant="outlined" sx={{ borderRadius: 2, mb: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Period</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Pay group</TableCell>
              <TableCell>Project</TableCell>
              <TableCell align="right">Employees</TableCell>
              <TableCell align="right">Gross</TableCell>
              <TableCell align="right">Net</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {runs.map((r) => (
              <TableRow key={r.id} hover selected={r.id === selectedId}>
                <TableCell>
                  <Stack spacing={0.25}>
                    <Typography variant="body2">{periodLabel(r)}</Typography>
                    {r.periodStartDate && r.periodEndDate && (
                      <Typography variant="caption" color="text.secondary">{r.periodStartDate} - {r.periodEndDate}</Typography>
                    )}
                  </Stack>
                </TableCell>
                <TableCell><Chip size="small" label={r.status} color={STATUS_COLOR[r.status ?? "DRAFT"] ?? "default"} /></TableCell>
                <TableCell>{r.payGroup}</TableCell>
                <TableCell>{projects.find((p) => p.id === r.projectId)?.code ?? "All"}</TableCell>
                <TableCell align="right">{r.employeeCount}</TableCell>
                <TableCell align="right">{money(r.totalGross)}</TableCell>
                <TableCell align="right">{money(r.totalNet)}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setSelectedId(r.id ?? null)}>Open</Button>
                  {(r.status === "DRAFT" || r.status === "CALCULATED") && r.id && (
                    <Button size="small" onClick={() => action.mutate({ id: r.id!, fn: "calculate" })}>
                      {r.status === "CALCULATED" ? "Recalculate" : "Calculate"}
                    </Button>
                  )}
                  {r.status === "DRAFT" && r.id && (
                    <Button size="small" color="error" onClick={() => remove.mutate(r.id!)}>Delete</Button>
                  )}
                  {r.status === "CALCULATED" && r.id && (
                    <Button size="small" color="success" onClick={() => action.mutate({ id: r.id!, fn: "approve" })}>Approve</Button>
                  )}
                  {r.status === "APPROVED" && r.id && (
                    <Button size="small" color="warning" onClick={() => action.mutate({ id: r.id!, fn: "lock" })}>Lock</Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {runs.length === 0 && (
              <TableRow><TableCell colSpan={8}><Typography variant="body2" color="text.secondary" p={1}>No payroll runs yet.</Typography></TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      {detail && <PayrollRunDetail run={detail} />}
    </Box>
  );
}

function PayrollRunDetail({ run }: { run: PayrollRun }) {
  const [openPayslip, setOpenPayslip] = useState<string | null>(null);

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}>
        <Box>
          <Typography variant="subtitle1" fontWeight={700}>Payroll results</Typography>
          <Typography variant="caption" color="text.secondary">
            {periodLabel(run)} - {run.employeeCount} employees - gross {money(run.totalGross)} - net {money(run.totalNet)}
          </Typography>
        </Box>
        <Chip size="small" label={run.status} color={STATUS_COLOR[run.status ?? "DRAFT"] ?? "default"} />
      </Stack>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Employee</TableCell>
            <TableCell>Pay</TableCell>
            <TableCell align="right">Days</TableCell>
            <TableCell align="right">Normal</TableCell>
            <TableCell align="right">OT</TableCell>
            <TableCell align="right">Earnings</TableCell>
            <TableCell align="right">Deduct.</TableCell>
            <TableCell align="right">Net</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Payslip</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {run.results.map((r) => {
            const isOpen = openPayslip === r.id;
            return (
              <Fragment key={r.id}>
                <TableRow>
                  <TableCell>{r.employeeNumber} - {r.employeeName}</TableCell>
                  <TableCell>{r.payStatus}</TableCell>
                  <TableCell align="right">{r.workedDays}</TableCell>
                  <TableCell align="right">{r.normalHours}</TableCell>
                  <TableCell align="right">{r.otHours}</TableCell>
                  <TableCell align="right">{money(r.totalEarnings)}</TableCell>
                  <TableCell align="right">{money(r.totalDeductions)}</TableCell>
                  <TableCell align="right">{money(r.net)}</TableCell>
                  <TableCell>{r.status}{r.message ? ` - ${r.message}` : ""}</TableCell>
                <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button size="small" onClick={() => setOpenPayslip(isOpen ? null : r.id ?? null)}>
                        {isOpen ? "Hide" : "Payslip"}
                      </Button>
                      <Button size="small" startIcon={<PrintIcon />} onClick={() => printPayslip(run, r)}>
                        Print
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
                {isOpen && (
                  <TableRow>
                    <TableCell colSpan={10} sx={{ bgcolor: "#f8fafc", p: 2 }}>
                      <PayslipPanel run={run} result={r} />
                    </TableCell>
                  </TableRow>
                )}
              </Fragment>
            );
          })}
          {run.results.length === 0 && (
            <TableRow><TableCell colSpan={10}><Typography variant="body2" color="text.secondary" p={1}>No results. Calculate the run first.</Typography></TableCell></TableRow>
          )}
        </TableBody>
      </Table>
    </Paper>
  );
}

function PayslipPanel({ run, result }: { run: PayrollRun; result: PayrollResult }) {
  const earnings = result.lines.filter((l) => l.componentType !== "DEDUCTION" && l.category !== "OVERTIME");
  const overtime = result.lines.filter((l) => l.category === "OVERTIME" || l.source === "OVERTIME");
  const deductions = result.lines.filter((l) => l.componentType === "DEDUCTION");
  const componentBreakdown = buildComponentBreakdown(result.lines);
  const regularDays = maxQty(result.lines.filter((l) => l.source === "REGULAR_DAYS"));
  const restHours = maxQty(result.lines.filter((l) => l.source === "WEEKLY_REST"));
  const normalOt = sumQty(overtime.filter((l) => !String(l.componentName).toUpperCase().includes("REST")));
  const restOt = sumQty(overtime.filter((l) => String(l.componentName).toUpperCase().includes("REST")));

  return (
    <Box sx={{ border: "1px solid", borderColor: "divider", borderRadius: 2, bgcolor: "background.paper", overflow: "hidden" }}>
      <Box sx={{ px: 2, py: 1.5, bgcolor: "#0f766e", color: "common.white" }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Box>
            <Typography variant="subtitle1" fontWeight={800}>Payslip</Typography>
            <Typography variant="caption">{periodLabel(run)} · {result.employeeNumber} · {result.employeeName}</Typography>
          </Box>
          <Box textAlign="right">
            <Typography variant="caption">Net pay</Typography>
            <Typography variant="h6" fontWeight={800}>{money(result.net)}</Typography>
          </Box>
        </Stack>
      </Box>

      <Box sx={{ p: 2 }}>
        <Grid container spacing={1.5} mb={2}>
          <PayslipMetric label="Regular days" value={qty(regularDays || result.workedDays)} />
          <PayslipMetric label="Weekend/Holiday hours" value={qty(restHours)} />
          <PayslipMetric label="Normal hours" value={qty(result.normalHours)} />
          <PayslipMetric label="Normal OT" value={qty(normalOt)} />
          <PayslipMetric label="Rest/Holiday OT" value={qty(restOt)} />
          <PayslipMetric label="Total OT" value={qty(result.otHours)} />
        </Grid>

        <Grid container spacing={2}>
          <Grid item xs={12} md={7}>
            <PayslipBreakdownTable items={componentBreakdown} />
            <Box mt={2}>
              <Divider />
            </Box>
            <Box mt={2}>
              <Typography variant="subtitle2" fontWeight={800} mb={1}>Raw payroll lines</Typography>
            </Box>
            <PayslipLineTable title="Earnings" lines={earnings} empty="No earnings." total={result.totalEarnings} />
            <Box mt={2}>
              <PayslipLineTable title="Overtime" lines={overtime} empty="No overtime." />
            </Box>
          </Grid>
          <Grid item xs={12} md={5}>
            <PayslipLineTable title="Deductions" lines={deductions} empty="No deductions." total={result.totalDeductions} />
            <Box sx={{ mt: 2, border: "1px solid", borderColor: "divider", borderRadius: 1, overflow: "hidden" }}>
              <SummaryRow label="Gross earnings" value={result.totalEarnings} />
              <SummaryRow label="Total deductions" value={result.totalDeductions} negative />
              <SummaryRow label="Net pay" value={result.net} strong />
            </Box>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
}

function PayslipMetric({ label, value }: { label: string; value: string }) {
  return (
    <Grid item xs={6} sm={4} md={2}>
      <Box sx={{ border: "1px solid", borderColor: "divider", borderRadius: 1, px: 1.25, py: 1 }}>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
        <Typography variant="subtitle2" fontWeight={800}>{value}</Typography>
      </Box>
    </Grid>
  );
}

function PayslipLineTable({ title, lines, empty, total }: { title: string; lines: PayrollResultLine[]; empty: string; total?: number }) {
  return (
    <Box sx={{ border: "1px solid", borderColor: "divider", borderRadius: 1, overflow: "hidden" }}>
      <Box sx={{ px: 1.5, py: 1, bgcolor: "grey.100" }}>
        <Typography variant="subtitle2" fontWeight={800}>{title}</Typography>
      </Box>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Item</TableCell>
            <TableCell align="right">Qty</TableCell>
            <TableCell align="right">Rate</TableCell>
            <TableCell align="right">Amount</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {lines.map((line) => (
            <TableRow key={line.id ?? `${line.source}-${line.componentName}`}>
              <TableCell>
                <Typography variant="body2">{line.componentName}</Typography>
                <Typography variant="caption" color="text.secondary">{line.componentCode} · {line.source ?? "PAY_ITEM"}</Typography>
              </TableCell>
              <TableCell align="right">{qty(line.quantity)}</TableCell>
              <TableCell align="right">{money(line.rate)}</TableCell>
              <TableCell align="right">{money(line.amount)}</TableCell>
            </TableRow>
          ))}
          {lines.length === 0 && (
            <TableRow>
              <TableCell colSpan={4}><Typography variant="body2" color="text.secondary">{empty}</Typography></TableCell>
            </TableRow>
          )}
          {total !== undefined && (
            <TableRow>
              <TableCell colSpan={3} align="right"><strong>Total</strong></TableCell>
              <TableCell align="right"><strong>{money(total)}</strong></TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </Box>
  );
}

function SummaryRow({ label, value, strong, negative }: { label: string; value: number; strong?: boolean; negative?: boolean }) {
  return (
    <Stack direction="row" justifyContent="space-between" sx={{ px: 1.5, py: 1, borderBottom: "1px solid", borderColor: "divider", bgcolor: strong ? "grey.100" : "background.paper" }}>
      <Typography variant="body2" fontWeight={strong ? 800 : 500}>{label}</Typography>
      <Typography variant="body2" fontWeight={strong ? 800 : 500} color={negative ? "error.main" : "text.primary"}>
        {negative && value > 0 ? "-" : ""}{money(value)}
      </Typography>
    </Stack>
  );
}

function sumQty(lines: PayrollResultLine[]) {
  return lines.reduce((acc, line) => acc + Number(line.quantity ?? 0), 0);
}

function maxQty(lines: PayrollResultLine[]) {
  return lines.reduce((acc, line) => Math.max(acc, Number(line.quantity ?? 0)), 0);
}

function printPayslip(run: PayrollRun, result: PayrollResult) {
  const earnings = result.lines.filter((l) => l.componentType !== "DEDUCTION" && l.category !== "OVERTIME");
  const overtime = result.lines.filter((l) => l.category === "OVERTIME" || l.source === "OVERTIME");
  const deductions = result.lines.filter((l) => l.componentType === "DEDUCTION");
  const breakdown = buildComponentBreakdown(result.lines);
  const html = `
    <html>
      <head>
        <title>Payslip ${result.employeeNumber}</title>
        <style>
          body { font-family: Arial, sans-serif; margin: 24px; color: #111827; }
          h1,h2,h3,p { margin: 0; }
          .header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom: 20px; }
          .muted { color:#6b7280; font-size:12px; }
          .grid { display:grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin: 18px 0; }
          .card { border:1px solid #d1d5db; border-radius:8px; padding:10px 12px; }
          .value { font-size:20px; font-weight:700; margin-top:4px; }
          table { width:100%; border-collapse: collapse; margin-top: 10px; }
          th, td { border:1px solid #d1d5db; padding:8px; text-align:left; vertical-align:top; }
          th { background:#f3f4f6; }
          .section { margin-top: 20px; }
          .right { text-align:right; }
          .details { margin-top:6px; font-size:12px; color:#6b7280; }
          .total { font-weight:700; }
        </style>
      </head>
      <body>
        <div class="header">
          <div>
            <h1>Payslip</h1>
            <p>${periodLabel(run)} - ${escapeHtml(result.employeeNumber ?? "")} - ${escapeHtml(result.employeeName ?? "")}</p>
            <p class="muted">Pay group: ${escapeHtml(result.payStatus ?? "")}</p>
          </div>
          <div class="right">
            <p class="muted">Net pay</p>
            <div class="value">${money(result.net)}</div>
          </div>
        </div>

        <div class="grid">
          <div class="card"><div class="muted">Worked days</div><div class="value">${qty(result.workedDays)}</div></div>
          <div class="card"><div class="muted">Normal hours</div><div class="value">${qty(result.normalHours)}</div></div>
          <div class="card"><div class="muted">OT hours</div><div class="value">${qty(result.otHours)}</div></div>
        </div>

        <div class="section">
          <h2>Component breakdown</h2>
          <table>
            <thead>
              <tr>
                <th>Component</th>
                <th class="right">Paid qty</th>
                <th class="right">Deduct qty</th>
                <th class="right">Rate</th>
                <th class="right">Net amount</th>
              </tr>
            </thead>
            <tbody>
              ${breakdown.map((item) => `
                <tr>
                  <td>
                    <div><strong>${escapeHtml(item.label)}</strong></div>
                    <div class="muted">${escapeHtml(item.code)}</div>
                    ${item.details.map((d) => `<div class="details">${d.kind === "pay" ? "Paid" : "Deducted"}: ${escapeHtml(d.label)} - ${qty(d.qty)} - ${money(d.amount)}</div>`).join("")}
                  </td>
                  <td class="right">${qty(item.payQty)}</td>
                  <td class="right">${qty(item.deductQty)}</td>
                  <td class="right">${money(item.rate)}</td>
                  <td class="right total">${money(item.netAmount)}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>

        <div class="section">
          <h2>Earnings</h2>
          ${printLineTable(earnings)}
        </div>
        <div class="section">
          <h2>Deductions</h2>
          ${printLineTable(deductions)}
        </div>
        <div class="section">
          <h2>Overtime</h2>
          ${printLineTable(overtime)}
        </div>

        <div class="section">
          <table>
            <tbody>
              <tr><td><strong>Gross earnings</strong></td><td class="right">${money(result.totalEarnings)}</td></tr>
              <tr><td><strong>Total deductions</strong></td><td class="right">${money(result.totalDeductions)}</td></tr>
              <tr><td><strong>Net pay</strong></td><td class="right">${money(result.net)}</td></tr>
            </tbody>
          </table>
        </div>
      </body>
    </html>
  `;
  openPrintWindow(html);
}

function printLineTable(lines: PayrollResultLine[]) {
  if (lines.length === 0) return `<p class="muted">No lines.</p>`;
  return `
    <table>
      <thead>
        <tr>
          <th>Item</th>
          <th class="right">Qty</th>
          <th class="right">Rate</th>
          <th class="right">Amount</th>
        </tr>
      </thead>
      <tbody>
        ${lines.map((line) => `
          <tr>
            <td>${escapeHtml(line.componentName ?? "")}<div class="muted">${escapeHtml(String(line.componentCode ?? ""))}</div></td>
            <td class="right">${qty(line.quantity)}</td>
            <td class="right">${money(line.rate)}</td>
            <td class="right">${money(line.amount)}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function openPrintWindow(html: string) {
  const win = window.open("", "_blank", "width=1100,height=900");
  if (!win) return;
  win.document.open();
  win.document.write(html);
  win.document.close();
  win.focus();
  win.onload = () => {
    win.print();
  };
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

type ComponentBreakdownRow = {
  code: string;
  label: string;
  payQty: number;
  deductQty: number;
  rate: number;
  payAmount: number;
  deductAmount: number;
  netAmount: number;
  details: Array<{ label: string; qty: number; amount: number; kind: "pay" | "deduct" }>;
};

function PayslipBreakdownTable({ items }: { items: ComponentBreakdownRow[] }) {
  return (
    <Box sx={{ border: "1px solid", borderColor: "divider", borderRadius: 1, overflow: "hidden" }}>
      <Box sx={{ px: 1.5, py: 1, bgcolor: "grey.100" }}>
        <Typography variant="subtitle2" fontWeight={800}>Component breakdown</Typography>
        <Typography variant="caption" color="text.secondary">
          Paid qty, deducted qty, and net amount for each payroll component.
        </Typography>
      </Box>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Component</TableCell>
            <TableCell align="right">Paid qty</TableCell>
            <TableCell align="right">Deduct qty</TableCell>
            <TableCell align="right">Rate</TableCell>
            <TableCell align="right">Net amount</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.code}>
              <TableCell sx={{ minWidth: 320 }}>
                <Typography variant="body2" fontWeight={700}>{item.label}</Typography>
                <Typography variant="caption" color="text.secondary">{item.code}</Typography>
                <Stack spacing={0.25} mt={0.75}>
                  {item.details.map((detail, idx) => (
                    <Typography key={`${item.code}-${idx}`} variant="caption" color="text.secondary">
                      {detail.kind === "pay" ? "Paid" : "Deducted"}: {detail.label} · {qty(detail.qty)} · {money(detail.amount)}
                    </Typography>
                  ))}
                </Stack>
              </TableCell>
              <TableCell align="right">{qty(item.payQty)}</TableCell>
              <TableCell align="right">{qty(item.deductQty)}</TableCell>
              <TableCell align="right">{money(item.rate)}</TableCell>
              <TableCell align="right">
                <Typography variant="body2" fontWeight={700}>{money(item.netAmount)}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {money(item.payAmount)} - {money(item.deductAmount)}
                </Typography>
              </TableCell>
            </TableRow>
          ))}
          {items.length === 0 && (
            <TableRow>
              <TableCell colSpan={5}><Typography variant="body2" color="text.secondary">No component breakdown available.</Typography></TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </Box>
  );
}

function buildComponentBreakdown(lines: PayrollResultLine[]): ComponentBreakdownRow[] {
  const grouped = new Map<string, ComponentBreakdownRow>();
  for (const line of lines.filter((line) => line.category !== "OVERTIME")) {
    const key = String(line.componentCode ?? line.componentName ?? line.id);
    const row = grouped.get(key) ?? {
      code: String(line.componentCode ?? ""),
      label: baseComponentLabel(String(line.componentName ?? "")),
      payQty: 0,
      deductQty: 0,
      rate: Number(line.rate ?? 0),
      payAmount: 0,
      deductAmount: 0,
      netAmount: 0,
      details: [],
    };
    const lineQty = Number(line.quantity ?? 0);
    const lineAmount = Number(line.amount ?? 0);
    row.rate = row.rate || Number(line.rate ?? 0);
    if (line.componentType === "DEDUCTION") {
      row.deductQty += lineQty;
      row.deductAmount += lineAmount;
      row.details.push({ label: detailLabel(line), qty: lineQty, amount: lineAmount, kind: "deduct" });
    } else {
      row.payQty += lineQty;
      row.payAmount += lineAmount;
      row.details.push({ label: detailLabel(line), qty: lineQty, amount: lineAmount, kind: "pay" });
    }
    row.netAmount = row.payAmount - row.deductAmount;
    grouped.set(key, row);
  }
  return Array.from(grouped.values()).sort((a, b) => a.code.localeCompare(b.code));
}

function baseComponentLabel(name: string) {
  return name
    .replace(/\s*-\s*Time type pay/i, "")
    .replace(/\s*-\s*Time type deduction/i, "")
    .replace(/\s*-\s*Normal paid hours/i, "")
    .replace(/\s*-\s*Weekend\/Holiday paid hours/i, "")
    .trim();
}

function detailLabel(line: PayrollResultLine) {
  const source = String(line.source ?? "");
  if (source === "TIME_TYPE_RULE_PAY") return "time type paid hours";
  if (source === "TIME_TYPE_RULE_DEDUCT") return "time type deduction";
  if (source === "REGULAR_HOURS") return "regular paid hours";
  if (source === "REGULAR_DAYS") return "regular paid days";
  if (source === "WEEKLY_REST") return "weekend / holiday paid hours";
  if (source === "OVERTIME") return "overtime";
  return source || "pay item";
}
