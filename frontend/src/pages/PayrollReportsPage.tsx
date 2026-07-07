import { useMemo, useState } from "react";
import {
  Box,
  Button,
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
import DownloadIcon from "@mui/icons-material/Download";
import { useQuery } from "@tanstack/react-query";
import { payrollReportApi, payrollRunApi, periodApi } from "../api/resources";
import type { CostCodeLine, PayrollCostReport, PayrollListingReport, PayrollListingRow, PayrollRun } from "../api/types";

const money = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const qty = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 });

export default function PayrollReportsPage() {
  const [reportType, setReportType] = useState<"payroll-listing" | "cost-allocation">("payroll-listing");
  const [periodId, setPeriodId] = useState("");
  const [runId, setRunId] = useState("");
  const { data: periods = [] } = useQuery({ queryKey: ["periods"], queryFn: () => periodApi.list() });
  const { data: runs = [] } = useQuery({ queryKey: ["payrollRuns", periodId], queryFn: () => payrollRunApi.list(periodId), enabled: !!periodId });
  const { data: report } = useQuery({
    queryKey: ["payrollListing", runId],
    queryFn: () => payrollReportApi.payrollListing(runId),
    enabled: !!runId && reportType === "payroll-listing",
  });
  const { data: costReport } = useQuery({
    queryKey: ["payrollCostReport", runId],
    queryFn: () => payrollReportApi.costReport(runId),
    enabled: !!runId && reportType === "cost-allocation",
  });

  const runLabel = (run: PayrollRun) => {
    const scope = [run.projectId ? "Project" : "All projects", run.payGroup ?? "ALL"].join(" / ");
    return `${scope} - ${run.status}`;
  };

  const rows = useMemo(() => report?.rows ?? [], [report]);

  return (
    <Box>
      <Typography variant="h5" mb={2}>Payroll Reports</Typography>
      <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, mb: 2 }}>
        <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} alignItems={{ md: "center" }}>
          <TextField
            select
            size="small"
            label="Report"
            value={reportType}
            onChange={(e) => setReportType(e.target.value as "payroll-listing" | "cost-allocation")}
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="payroll-listing">Payroll Listing</MenuItem>
            <MenuItem value="cost-allocation">Cost Allocation</MenuItem>
          </TextField>
          <TextField
            select
            size="small"
            label="Period"
            value={periodId}
            onChange={(e) => {
              setPeriodId(e.target.value);
              setRunId("");
            }}
            sx={{ minWidth: 260 }}
          >
            <MenuItem value="">Select period</MenuItem>
            {periods.map((p) => <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>)}
          </TextField>
          <TextField
            select
            size="small"
            label="Payroll run"
            value={runId}
            onChange={(e) => setRunId(e.target.value)}
            disabled={!periodId}
            sx={{ minWidth: 320 }}
          >
            <MenuItem value="">Select run</MenuItem>
            {runs.map((r) => <MenuItem key={r.id} value={r.id}>{runLabel(r)}</MenuItem>)}
          </TextField>
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            disabled={reportType === "payroll-listing" ? !report : !costReport}
            onClick={() => {
              if (reportType === "payroll-listing" && report) downloadCsv(report);
              if (reportType === "cost-allocation" && costReport) downloadCostCsv(costReport);
            }}
          >
            CSV
          </Button>
        </Stack>
      </Paper>

      {reportType === "payroll-listing" && report && (
        <>
          <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} mb={2}>
            <Total label="Employees" value={report.employeeCount} plain />
            <Total label="Basic" value={report.totalBasic} />
            <Total label="Allowances" value={report.totalAllowances} />
            <Total label="Overtime" value={report.totalOvertime} />
            <Total label="Deductions" value={report.totalDeductions} />
            <Total label="Net" value={report.totalNet} />
          </Stack>

          <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto" }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>Employee</TableCell>
                  <TableCell>Project</TableCell>
                  <TableCell>Cost code</TableCell>
                  <TableCell>Pay</TableCell>
                  <TableCell align="right">Days</TableCell>
                  <TableCell align="right">Normal</TableCell>
                  <TableCell align="right">OT</TableCell>
                  <TableCell align="right">Basic</TableCell>
                  <TableCell align="right">Allowances</TableCell>
                  <TableCell align="right">Overtime</TableCell>
                  <TableCell align="right">Deductions</TableCell>
                  <TableCell align="right">Gross</TableCell>
                  <TableCell align="right">Net</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => <ListingRow key={row.employeeId} row={row} />)}
                {rows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={14}>
                      <Typography variant="body2" color="text.secondary">No payroll results found for this run.</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Paper>
        </>
      )}

      {reportType === "cost-allocation" && costReport && (
        <>
          <Typography variant="subtitle1" mb={1}>By cost code (all employees combined)</Typography>
          <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto", mb: 3 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>Project</TableCell>
                  <TableCell>Cost code</TableCell>
                  <TableCell align="right">Hours</TableCell>
                  <TableCell align="right">Value</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {costReport.byCostCode.map((line, i) => (
                  <TableRow key={i} hover>
                    <TableCell>{line.projectCode ?? line.projectName ?? ""}</TableCell>
                    <TableCell>{line.costCodeCode ?? line.costCodeName ?? ""}</TableCell>
                    <TableCell align="right">{qty(line.hours)}</TableCell>
                    <TableCell align="right">{money(line.value)}</TableCell>
                  </TableRow>
                ))}
                {costReport.byCostCode.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4}>
                      <Typography variant="body2" color="text.secondary">No cost allocation found for this run.</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Paper>

          <Typography variant="subtitle1" mb={1}>By employee</Typography>
          <Paper variant="outlined" sx={{ borderRadius: 2, overflow: "auto" }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>Employee</TableCell>
                  <TableCell>Project</TableCell>
                  <TableCell>Cost code</TableCell>
                  <TableCell align="right">Hours</TableCell>
                  <TableCell align="right">Value</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {costReport.byEmployee.flatMap((emp) =>
                  emp.lines.map((line, i) => (
                    <TableRow key={`${emp.employeeId}-${i}`} hover>
                      <TableCell>
                        {i === 0 && (
                          <>
                            <Typography variant="body2" fontWeight={700}>{emp.employeeNumber}</Typography>
                            <Typography variant="caption" color="text.secondary">{emp.employeeName}</Typography>
                          </>
                        )}
                      </TableCell>
                      <TableCell>{line.projectCode ?? line.projectName ?? ""}</TableCell>
                      <TableCell>{line.costCodeCode ?? line.costCodeName ?? ""}</TableCell>
                      <TableCell align="right">{qty(line.hours)}</TableCell>
                      <TableCell align="right">{money(line.value)}</TableCell>
                    </TableRow>
                  ))
                )}
                {costReport.byEmployee.every((e) => e.lines.length === 0) && (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Typography variant="body2" color="text.secondary">No cost allocation found for this run.</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Paper>
        </>
      )}
    </Box>
  );
}

function Total({ label, value, plain = false }: { label: string; value: number; plain?: boolean }) {
  return (
    <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, minWidth: 150 }}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography fontWeight={700}>{plain ? value : money(value)}</Typography>
    </Paper>
  );
}

function ListingRow({ row }: { row: PayrollListingRow }) {
  return (
    <TableRow hover>
      <TableCell>
        <Typography variant="body2" fontWeight={700}>{row.employeeNumber}</Typography>
        <Typography variant="caption" color="text.secondary">{row.employeeName}</Typography>
      </TableCell>
      <TableCell>{row.projectCode ?? ""}</TableCell>
      <TableCell>{row.costCode ?? ""}</TableCell>
      <TableCell>{row.payGroup}</TableCell>
      <TableCell align="right">{qty(row.workedDays)}</TableCell>
      <TableCell align="right">{qty(row.normalHours)}</TableCell>
      <TableCell align="right">{qty(row.otHours)}</TableCell>
      <TableCell align="right">{money(row.basic)}</TableCell>
      <TableCell align="right">{money(row.allowances)}</TableCell>
      <TableCell align="right">{money(row.overtime)}</TableCell>
      <TableCell align="right">{money(row.deductions)}</TableCell>
      <TableCell align="right">{money(row.gross)}</TableCell>
      <TableCell align="right">{money(row.net)}</TableCell>
      <TableCell>{row.status}{row.message ? ` - ${row.message}` : ""}</TableCell>
    </TableRow>
  );
}

function downloadCsv(report: PayrollListingReport) {
  const headers = ["Employee No", "Employee Name", "Project", "Cost Code", "Pay Group", "Days", "Normal Hours", "OT Hours", "Basic", "Allowances", "Overtime", "Deductions", "Gross", "Net", "Status"];
  const lines = report.rows.map((row) => [
    row.employeeNumber,
    row.employeeName,
    row.projectCode,
    row.costCode,
    row.payGroup,
    row.workedDays,
    row.normalHours,
    row.otHours,
    row.basic,
    row.allowances,
    row.overtime,
    row.deductions,
    row.gross,
    row.net,
    row.status,
  ]);
  const csv = [headers, ...lines].map((line) => line.map(csvCell).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `payroll-listing-${report.periodName ?? report.runId}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function downloadCostCsv(report: PayrollCostReport) {
  const headers = ["Project", "Cost Code", "Hours", "Value"];
  const lines = report.byCostCode.map((line: CostCodeLine) => [
    line.projectCode ?? line.projectName ?? "",
    line.costCodeCode ?? line.costCodeName ?? "",
    line.hours,
    line.value,
  ]);
  const csv = [headers, ...lines].map((line) => line.map(csvCell).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `cost-allocation-${report.runId}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function csvCell(value: unknown) {
  const text = String(value ?? "");
  return `"${text.replace(/"/g, '""')}"`;
}
