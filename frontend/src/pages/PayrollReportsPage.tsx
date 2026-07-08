import { useState } from "react";
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
import { payrollReportApi, payrollRunApi, periodApi, projectApi } from "../api/resources";
import type { EmployeeCostBreakdown, PayrollListingRow, PayrollListingSummary, PayrollRun } from "../api/types";

const money = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const qty = (v?: number) => Number(v ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 });
const PAGE_SIZE = 25;

export default function PayrollReportsPage() {
  const [reportType, setReportType] = useState<"payroll-listing" | "cost-allocation">("payroll-listing");
  const [periodId, setPeriodId] = useState("");
  const [runId, setRunId] = useState("");
  const [costProjectId, setCostProjectId] = useState(""); // "" = every project's run for the period

  // Payroll listing — fast summary, then paginated + searchable rows
  const [listingPage, setListingPage] = useState(0);
  const [listingSearch, setListingSearch] = useState("");
  const [listingSearchInput, setListingSearchInput] = useState("");

  // Cost allocation — fast summary, then paginated + searchable employees
  const [costPage, setCostPage] = useState(0);
  const [costSearch, setCostSearch] = useState("");
  const [costSearchInput, setCostSearchInput] = useState("");

  const { data: periods = [] } = useQuery({ queryKey: ["periods"], queryFn: () => periodApi.list() });
  const { data: runs = [] } = useQuery({ queryKey: ["payrollRuns", periodId], queryFn: () => payrollRunApi.list(periodId), enabled: !!periodId });
  const { data: projects = [] } = useQuery({ queryKey: ["projects"], queryFn: () => projectApi.list() });

  const { data: listingSummary } = useQuery({
    queryKey: ["payrollListingSummary", runId],
    queryFn: () => payrollReportApi.payrollListingSummary(runId),
    enabled: !!runId && reportType === "payroll-listing",
  });
  const { data: listingRowsPage } = useQuery({
    queryKey: ["payrollListingRows", runId, listingPage, listingSearch],
    queryFn: () => payrollReportApi.payrollListingRows(runId, listingPage, PAGE_SIZE, listingSearch || undefined),
    enabled: !!runId && reportType === "payroll-listing",
  });
  const listingRows = listingRowsPage?.content ?? [];

  // Cost allocation is period-based (optionally narrowed to one project) —
  // each project is calculated as its own separate payroll run, so a
  // "whole month" view has to combine several runs, not pick just one.
  const { data: costSummary } = useQuery({
    queryKey: ["payrollCostSummary", periodId, costProjectId],
    queryFn: () => payrollReportApi.costAllocationSummary(periodId, costProjectId || undefined),
    enabled: !!periodId && reportType === "cost-allocation",
  });
  const { data: costEmployeesPage } = useQuery({
    queryKey: ["payrollCostEmployees", periodId, costProjectId, costPage, costSearch],
    queryFn: () => payrollReportApi.costAllocationEmployees(periodId, costProjectId || undefined, costPage, PAGE_SIZE, costSearch || undefined),
    enabled: !!periodId && reportType === "cost-allocation",
  });
  const costEmployeeRows = costEmployeesPage?.content ?? [];

  const runLabel = (run: PayrollRun) => {
    const scope = [run.projectId ? "Project" : "All projects", run.payGroup ?? "ALL"].join(" / ");
    return `${scope} - ${run.status}`;
  };

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
          {reportType === "payroll-listing" ? (
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
          ) : (
            <TextField
              select
              size="small"
              label="Project"
              value={costProjectId}
              onChange={(e) => { setCostProjectId(e.target.value); setCostPage(0); }}
              disabled={!periodId}
              sx={{ minWidth: 260 }}
            >
              <MenuItem value="">All projects (whole period)</MenuItem>
              {projects.map((p) => <MenuItem key={p.id} value={p.id}>{p.code} — {p.name}</MenuItem>)}
            </TextField>
          )}
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            disabled={reportType === "payroll-listing" ? !listingSummary : !costSummary}
            onClick={async () => {
              if (reportType === "payroll-listing" && listingSummary) {
                const allRows = await fetchAllPages((page) => payrollReportApi.payrollListingRows(runId, page, 200));
                downloadListingCsv(runId, listingSummary, allRows);
              }
              if (reportType === "cost-allocation" && costSummary) {
                const allEmployees = await fetchAllPages((page) => payrollReportApi.costAllocationEmployees(periodId, costProjectId || undefined, page, 200));
                downloadCostCsv(periodId, allEmployees);
              }
            }}
          >
            CSV (full detail)
          </Button>
        </Stack>
      </Paper>

      {reportType === "payroll-listing" && listingSummary && (
        <>
          <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} mb={2}>
            <Total label="Employees" value={listingSummary.employeeCount} plain />
            <Total label="Basic" value={listingSummary.totalBasic} />
            <Total label="Allowances" value={listingSummary.totalAllowances} />
            <Total label="Overtime" value={listingSummary.totalOvertime} />
            <Total label="Deductions" value={listingSummary.totalDeductions} />
            <Total label="Net" value={listingSummary.totalNet} />
          </Stack>

          <Stack direction="row" spacing={1} mb={1.5} alignItems="center">
            <TextField
              size="small"
              label="Search employee (name or number)"
              value={listingSearchInput}
              onChange={(e) => setListingSearchInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === "Enter") { setListingPage(0); setListingSearch(listingSearchInput); } }}
              sx={{ minWidth: 260 }}
            />
            <Button size="small" variant="outlined" onClick={() => { setListingPage(0); setListingSearch(listingSearchInput); }}>Search</Button>
            {listingSearch && (
              <Button size="small" onClick={() => { setListingSearchInput(""); setListingSearch(""); setListingPage(0); }}>Clear</Button>
            )}
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
                {listingRows.map((row) => <ListingRow key={row.employeeId} row={row} />)}
                {listingRows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={14}>
                      <Typography variant="body2" color="text.secondary">No payroll results match.</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Paper>
          {listingRowsPage && listingRowsPage.totalPages > 1 && (
            <Stack direction="row" justifyContent="space-between" alignItems="center" mt={1.5}>
              <Typography variant="caption" color="text.secondary">
                Page {listingRowsPage.page + 1} of {listingRowsPage.totalPages} - {listingRowsPage.totalElements} employee(s)
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" disabled={listingRowsPage.first} onClick={() => setListingPage((p) => Math.max(p - 1, 0))}>Previous</Button>
                <Button size="small" disabled={listingRowsPage.last} onClick={() => setListingPage((p) => p + 1)}>Next</Button>
              </Stack>
            </Stack>
          )}
        </>
      )}

      {reportType === "cost-allocation" && costSummary && (
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
                {costSummary.map((line, i) => (
                  <TableRow key={i} hover>
                    <TableCell>{line.projectCode ?? line.projectName ?? ""}</TableCell>
                    <TableCell>{line.costCodeCode ?? line.costCodeName ?? ""}</TableCell>
                    <TableCell align="right">{qty(line.hours)}</TableCell>
                    <TableCell align="right">{money(line.value)}</TableCell>
                  </TableRow>
                ))}
                {costSummary.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4}>
                      <Typography variant="body2" color="text.secondary">No cost allocation found for this run.</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Paper>

          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1} flexWrap="wrap" gap={1}>
            <Typography variant="subtitle1">By employee</Typography>
            <Stack direction="row" spacing={1} alignItems="center">
              <TextField
                size="small"
                label="Search employee (name or number)"
                value={costSearchInput}
                onChange={(e) => setCostSearchInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter") { setCostPage(0); setCostSearch(costSearchInput); } }}
                sx={{ minWidth: 260 }}
              />
              <Button size="small" variant="outlined" onClick={() => { setCostPage(0); setCostSearch(costSearchInput); }}>Search</Button>
              {costSearch && (
                <Button size="small" onClick={() => { setCostSearchInput(""); setCostSearch(""); setCostPage(0); }}>Clear</Button>
              )}
            </Stack>
          </Stack>
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
                {costEmployeeRows.flatMap((emp) =>
                  (emp.lines.length > 0 ? emp.lines : [null]).map((line, i) => (
                    <TableRow key={`${emp.employeeId}-${i}`} hover>
                      <TableCell>
                        {i === 0 && (
                          <>
                            <Typography variant="body2" fontWeight={700}>{emp.employeeNumber}</Typography>
                            <Typography variant="caption" color="text.secondary">{emp.employeeName}</Typography>
                          </>
                        )}
                      </TableCell>
                      <TableCell>{line?.projectCode ?? line?.projectName ?? ""}</TableCell>
                      <TableCell>{line?.costCodeCode ?? line?.costCodeName ?? ""}</TableCell>
                      <TableCell align="right">{line ? qty(line.hours) : ""}</TableCell>
                      <TableCell align="right">{line ? money(line.value) : ""}</TableCell>
                    </TableRow>
                  ))
                )}
                {costEmployeeRows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Typography variant="body2" color="text.secondary">No employees match.</Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Paper>
          {costEmployeesPage && costEmployeesPage.totalPages > 1 && (
            <Stack direction="row" justifyContent="space-between" alignItems="center" mt={1.5}>
              <Typography variant="caption" color="text.secondary">
                Page {costEmployeesPage.page + 1} of {costEmployeesPage.totalPages} - {costEmployeesPage.totalElements} employee(s)
              </Typography>
              <Stack direction="row" spacing={1}>
                <Button size="small" disabled={costEmployeesPage.first} onClick={() => setCostPage((p) => Math.max(p - 1, 0))}>Previous</Button>
                <Button size="small" disabled={costEmployeesPage.last} onClick={() => setCostPage((p) => p + 1)}>Next</Button>
              </Stack>
            </Stack>
          )}
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

function downloadListingCsv(runId: string, summary: PayrollListingSummary, rows: PayrollListingRow[]) {
  const headers = ["Employee No", "Employee Name", "Project", "Cost Code", "Pay Group", "Days", "Normal Hours", "OT Hours", "Basic", "Allowances", "Overtime", "Deductions", "Gross", "Net", "Status"];
  const lines = rows.map((row) => [
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
  a.download = `payroll-listing-${summary.periodName ?? runId}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

function downloadCostCsv(periodId: string, employees: EmployeeCostBreakdown[]) {
  const headers = ["Employee No", "Employee Name", "Project", "Cost Code", "Hours", "Value"];
  const lines = employees.flatMap((emp) =>
    (emp.lines.length > 0 ? emp.lines : [null]).map((line) => [
      emp.employeeNumber ?? "",
      emp.employeeName ?? "",
      line?.projectCode ?? line?.projectName ?? "",
      line?.costCodeCode ?? line?.costCodeName ?? "",
      line?.hours ?? "",
      line?.value ?? "",
    ])
  );
  const csv = [headers, ...lines].map((line) => line.map(csvCell).join(",")).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `cost-allocation-${periodId}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

/** Walks every page of a paginated endpoint (large page size) and
 * concatenates the results — used only for the explicit "download all"
 * action, never for the on-screen table (which stays paginated). */
async function fetchAllPages<T>(
  fetchPage: (page: number) => Promise<{ content: T[]; totalPages: number }>
): Promise<T[]> {
  const first = await fetchPage(0);
  let all = [...first.content];
  for (let page = 1; page < first.totalPages; page++) {
    const next = await fetchPage(page);
    all = all.concat(next.content);
  }
  return all;
}

function csvCell(value: unknown) {
  const text = String(value ?? "");
  return `"${text.replace(/"/g, '""')}"`;
}
