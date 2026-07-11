import { api } from "./client";
import type {
  Assignment,
  Bank,
  Contract,
  ContractPayItem,
  Country,
  Currency,
  Employee,
  EmployeeProjectSummary,
  EmployeeSummary,
  EmployeeBankAccount,
  EmployeeDocument,
  GenerateTimesheetRequest,
  ImportSummary,
  LegacyRaw,
  LookupValue,
  OrgUnitTreeNode,
  OrgUnitType,
  OrganizationUnit,
  PageResponse,
  PayrollComponent,
  ProvisionCreateRequest,
  ProvisionRule,
  ProvisionRun,
  PayrollRun,
  PayrollResult,
  CostCodeLine,
  EmployeeCostBreakdown,
  Project,
  CostCode,
  PublicHoliday,
  PayrollCalendar,
  PayrollPeriod,
  EmployeeShift,
  Crew,
  CrewMember,
  CrewTrade,
  TimekeeperProject,
  TimekeeperDay,
  TimekeeperMarkRequest,
  Rule,
  RulePackage,
  OvertimeCategory,
  Shift,
  TimeType,
  Timesheet,
  TimesheetDay,
  TimesheetProjectSummary,
  TimesheetSummary,
  TicketAccrualReport,
  TicketBalance,
  TicketFare,
  TicketLedger,
  BulkTimesheetJob,
  BulkStatusJob,
  CompanyProfile,
  LeaveAdjustment,
  LeaveBalance,
  LeaveProjectSummary,
  LeaveRequest,
  LeaveType,
} from "./types";

export const companyProfileApi = {
  get: () => api.get<CompanyProfile>("/company-profile").then((r) => r.data),
  getPublic: () => api.get<CompanyProfile>("/company-profile/public").then((r) => r.data),
  save: (payload: CompanyProfile) => api.put<CompanyProfile>("/company-profile", payload).then((r) => r.data),
};

export const leaveApi = {
  types: () => api.get<LeaveType[]>("/leave/types").then((r) => r.data),
  saveType: (payload: LeaveType) => payload.id
    ? api.put<LeaveType>(`/leave/types/${payload.id}`, payload).then((r) => r.data)
    : api.post<LeaveType>("/leave/types", payload).then((r) => r.data),
  requests: (params?: { employeeId?: string; projectId?: string; status?: string; leaveTypeId?: string; q?: string; page?: number; size?: number }) =>
    api.get<PageResponse<LeaveRequest>>("/leave/requests", { params }).then((r) => r.data),
  projectSummary: (params?: { projectId?: string; status?: string; leaveTypeId?: string; fromDate?: string; toDate?: string }) =>
    api.get<LeaveProjectSummary[]>("/leave/requests/project-summary", { params }).then((r) => r.data),
  saveRequest: (payload: LeaveRequest) => payload.id
    ? api.put<LeaveRequest>(`/leave/requests/${payload.id}`, payload).then((r) => r.data)
    : api.post<LeaveRequest>("/leave/requests", payload).then((r) => r.data),
  setRequestStatus: (id: string, status: string) =>
    api.post<LeaveRequest>(`/leave/requests/${id}/status`, null, { params: { status } }).then((r) => r.data),
  adjustments: (employeeId: string) =>
    api.get<LeaveAdjustment[]>("/leave/adjustments", { params: { employeeId } }).then((r) => r.data),
  saveAdjustment: (payload: LeaveAdjustment) => payload.id
    ? api.put<LeaveAdjustment>(`/leave/adjustments/${payload.id}`, payload).then((r) => r.data)
    : api.post<LeaveAdjustment>("/leave/adjustments", payload).then((r) => r.data),
  balances: (employeeId: string, asOfDate?: string) =>
    api.get<LeaveBalance[]>("/leave/balances", { params: { employeeId, ...(asOfDate ? { asOfDate } : {}) } }).then((r) => r.data),
};

// --- Assignments ---
export const assignmentApi = {
  byEmployee: (employeeId: string) =>
    api.get<Assignment[]>("/assignments", { params: { employeeId } }).then((r) => r.data),
  create: (d: Assignment) => api.post<Assignment>("/assignments", d).then((r) => r.data),
  update: (id: string, d: Assignment) => api.put<Assignment>(`/assignments/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/assignments/${id}`).then(() => undefined),
};

// --- Projects & cost codes ---
export const projectApi = {
  list: () => api.get<Project[]>("/projects").then((r) => r.data),
  create: (d: Project) => api.post<Project>("/projects", d).then((r) => r.data),
  update: (id: string, d: Project) => api.put<Project>(`/projects/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/projects/${id}`).then(() => undefined),
};

export const costCodeApi = {
  list: () => api.get<CostCode[]>("/cost-codes").then((r) => r.data),
  byProject: (projectId: string) =>
    api.get<CostCode[]>("/cost-codes", { params: { projectId } }).then((r) => r.data),
  create: (d: CostCode) => api.post<CostCode>("/cost-codes", d).then((r) => r.data),
  update: (id: string, d: CostCode) => api.put<CostCode>(`/cost-codes/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/cost-codes/${id}`).then(() => undefined),
};

// --- Overtime categories (reference) ---
export const overtimeCategoryApi = {
  list: () => api.get<OvertimeCategory[]>("/overtime-categories").then((r) => r.data),
  create: (d: OvertimeCategory) => api.post<OvertimeCategory>("/overtime-categories", d).then((r) => r.data),
  update: (id: string, d: OvertimeCategory) => api.put<OvertimeCategory>(`/overtime-categories/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/overtime-categories/${id}`).then(() => undefined),
};

// --- Rule engine (country law) ---
export const rulePackageApi = {
  list: () => api.get<RulePackage[]>("/rule-packages").then((r) => r.data),
  getActive: () => api.get<{ packageCode: string }>("/rule-packages/active").then((r) => r.data),
  setActive: (packageCode: string) =>
    api.put<{ packageCode: string }>("/rule-packages/active", { packageCode }).then((r) => r.data),
};

export const ruleApi = {
  byPackage: (packageCode: string) =>
    api.get<Rule[]>("/rules", { params: { packageCode } }).then((r) => r.data),
  create: (d: Rule) => api.post<Rule>("/rules", d).then((r) => r.data),
  update: (id: string, d: Rule) => api.put<Rule>(`/rules/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/rules/${id}`).then(() => undefined),
};

// --- Lookups (configurable dropdown sources) ---
export const lookupApi = {
  byCategory: (category: string) =>
    api.get<LookupValue[]>("/lookups", { params: { category } }).then((r) => r.data),
};

// --- Banks ---
export const bankApi = {
  list: () => api.get<Bank[]>("/banks").then((r) => r.data),
  create: (d: Bank) => api.post<Bank>("/banks", d).then((r) => r.data),
  update: (id: string, d: Bank) => api.put<Bank>(`/banks/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/banks/${id}`).then(() => undefined),
};

// --- Contracts ---
export const contractApi = {
  byEmployee: (employeeId: string) =>
    api.get<Contract[]>("/contracts", { params: { employeeId } }).then((r) => r.data),
  create: (d: Contract) => api.post<Contract>("/contracts", d).then((r) => r.data),
  update: (id: string, d: Contract) => api.put<Contract>(`/contracts/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/contracts/${id}`).then(() => undefined),
};

// --- Contract pay items (effective-dated salary structure) ---
export const contractPayItemApi = {
  byContract: (contractId: string) =>
    api.get<ContractPayItem[]>("/contract-pay-items", { params: { contractId } }).then((r) => r.data),
  create: (d: ContractPayItem) => api.post<ContractPayItem>("/contract-pay-items", d).then((r) => r.data),
  update: (id: string, d: ContractPayItem) =>
    api.put<ContractPayItem>(`/contract-pay-items/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/contract-pay-items/${id}`).then(() => undefined),
};

// --- Employee documents ---
export const employeeDocumentApi = {
  byEmployee: (employeeId: string) =>
    api.get<EmployeeDocument[]>("/employee-documents", { params: { employeeId } }).then((r) => r.data),
  create: (d: EmployeeDocument) => api.post<EmployeeDocument>("/employee-documents", d).then((r) => r.data),
  update: (id: string, d: EmployeeDocument) =>
    api.put<EmployeeDocument>(`/employee-documents/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/employee-documents/${id}`).then(() => undefined),
};

// --- Employee bank accounts ---
export const employeeBankAccountApi = {
  byEmployee: (employeeId: string) =>
    api.get<EmployeeBankAccount[]>("/employee-bank-accounts", { params: { employeeId } }).then((r) => r.data),
  create: (d: EmployeeBankAccount) =>
    api.post<EmployeeBankAccount>("/employee-bank-accounts", d).then((r) => r.data),
  update: (id: string, d: EmployeeBankAccount) =>
    api.put<EmployeeBankAccount>(`/employee-bank-accounts/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/employee-bank-accounts/${id}`).then(() => undefined),
};

// --- Currencies ---
export const currencyApi = {
  list: () => api.get<Currency[]>("/currencies").then((r) => r.data),
  create: (d: Currency) => api.post<Currency>("/currencies", d).then((r) => r.data),
  update: (id: string, d: Currency) => api.put<Currency>(`/currencies/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/currencies/${id}`).then(() => undefined),
};

// --- Countries ---
export const countryApi = {
  list: () => api.get<Country[]>("/countries").then((r) => r.data),
  create: (d: Country) => api.post<Country>("/countries", d).then((r) => r.data),
  update: (id: string, d: Country) => api.put<Country>(`/countries/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/countries/${id}`).then(() => undefined),
};

// --- Org unit types ---
export const orgUnitTypeApi = {
  list: () => api.get<OrgUnitType[]>("/org-unit-types").then((r) => r.data),
  create: (d: OrgUnitType) => api.post<OrgUnitType>("/org-unit-types", d).then((r) => r.data),
  update: (id: string, d: OrgUnitType) => api.put<OrgUnitType>(`/org-unit-types/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/org-unit-types/${id}`).then(() => undefined),
};

// --- Organization units ---
export const organizationUnitApi = {
  list: () => api.get<OrganizationUnit[]>("/organization-units").then((r) => r.data),
  tree: () => api.get<OrgUnitTreeNode[]>("/organization-units/tree").then((r) => r.data),
  create: (d: OrganizationUnit) => api.post<OrganizationUnit>("/organization-units", d).then((r) => r.data),
  update: (id: string, d: OrganizationUnit) =>
    api.put<OrganizationUnit>(`/organization-units/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/organization-units/${id}`).then(() => undefined),
};

// --- Employees ---
export const employeeApi = {
  list: (page = 0, size = 20, q?: string, payStatus?: string, projectId?: string,
         opts?: { activeOnly?: boolean; assignedOnly?: boolean; unassigned?: boolean }) =>
    api
      .get<PageResponse<Employee>>("/employees", {
        params: {
          page,
          size,
          ...(q ? { q } : {}),
          ...(payStatus ? { payStatus } : {}),
          ...(projectId ? { projectId } : {}),
          ...(opts?.activeOnly ? { activeOnly: true } : {}),
          ...(opts?.assignedOnly ? { assignedOnly: true } : {}),
          ...(opts?.unassigned ? { unassigned: true } : {}),
        },
      })
      .then((r) => r.data),
  summary: (q?: string, projectId?: string) =>
    api
      .get<EmployeeSummary>("/employees/summary", {
        params: { ...(q ? { q } : {}), ...(projectId ? { projectId } : {}) },
      })
      .then((r) => r.data),
  projectSummary: () =>
    api.get<EmployeeProjectSummary[]>("/employees/project-summary").then((r) => r.data),
  get: (id: string) => api.get<Employee>(`/employees/${id}`).then((r) => r.data),
  timeTypeUsage: (id: string, year: number) =>
    api.get<import("./types").EmployeeTimeTypeUsage>(`/employees/${id}/time-type-usage`, { params: { year } }).then((r) => r.data),
  create: (d: Employee) => api.post<Employee>("/employees", d).then((r) => r.data),
  update: (id: string, d: Employee) => api.put<Employee>(`/employees/${id}`, d).then((r) => r.data),
  assignTimekeeperByProject: (projectId: string, timekeeperEmployeeId: string) =>
    api.put<{ updated: number }>("/employees/timekeeper/by-project", null, { params: { projectId, timekeeperEmployeeId } }).then((r) => r.data),
  assignTimekeeperByEmployees: (employeeIds: string[], timekeeperEmployeeId: string, projectId?: string) =>
    api.put<{ updated: number }>("/employees/timekeeper/by-employees", employeeIds, { params: { timekeeperEmployeeId, projectId } }).then((r) => r.data),
  moveTimekeeperByEmployees: (employeeIds: string[], timekeeperEmployeeId: string, projectId?: string) =>
    api.put<{ updated: number }>("/employees/timekeeper/move-employees", employeeIds, { params: { timekeeperEmployeeId, projectId } }).then((r) => r.data),
  clearTimekeeperByEmployees: (employeeIds: string[], projectId?: string) =>
    api.put<{ updated: number }>("/employees/timekeeper/clear-employees", employeeIds, { params: { projectId } }).then((r) => r.data),
  remove: (id: string) => api.delete(`/employees/${id}`).then(() => undefined),
};

// --- Legacy import (upload the old FoxPro/DBF snapshot; preview then commit) ---
export const legacyImportApi = {
  preview: (files: File[]) => post("/legacy-import/preview", files),
  commit: (files: File[]) => post("/legacy-import", files),
};

// --- Legacy raw snapshot (full header + detail, every column preserved) ---
export const legacyRawApi = {
  byEmployee: (employeeId: string) =>
    api.get<LegacyRaw>(`/legacy-import/raw/${employeeId}`).then((r) => r.data),
};

function post(url: string, files: File[]) {
  const form = new FormData();
  files.forEach((f) => form.append("files", f));
  return api
    .post<ImportSummary>(url, form, { headers: { "Content-Type": "multipart/form-data" } })
    .then((r) => r.data);
}

// --- Timesheet: shifts ---
export const shiftApi = {
  list: (projectId?: unknown) =>
    api.get<Shift[]>("/shifts", { params: typeof projectId === "string" && projectId ? { projectId } : {} }).then((r) => r.data),
  create: (d: Shift) => api.post<Shift>("/shifts", d).then((r) => r.data),
  update: (id: string, d: Shift) => api.put<Shift>(`/shifts/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/shifts/${id}`).then(() => undefined),
};

// --- Timesheet: time types ---
export const timeTypeApi = {
  list: () => api.get<TimeType[]>("/time-types").then((r) => r.data),
  create: (d: TimeType) => api.post<TimeType>("/time-types", d).then((r) => r.data),
  update: (id: string, d: TimeType) => api.put<TimeType>(`/time-types/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/time-types/${id}`).then(() => undefined),
};

export const timeTypePayrollRuleApi = {
  list: (timeTypeId: string) =>
    api.get<import("./types").TimeTypePayrollRule[]>(`/time-types/${timeTypeId}/payroll-rules`).then((r) => r.data),
  save: (timeTypeId: string, payload: import("./types").TimeTypePayrollRule) =>
    api.post<import("./types").TimeTypePayrollRule>(`/time-types/${timeTypeId}/payroll-rules`, payload).then((r) => r.data),
  initializeDefaults: (timeTypeId: string) =>
    api.post<import("./types").TimeTypePayrollRule[]>(`/time-types/${timeTypeId}/payroll-rules/initialize-defaults`).then((r) => r.data),
  remove: (timeTypeId: string, componentId: string) =>
    api.delete(`/time-types/${timeTypeId}/payroll-rules/${componentId}`).then(() => undefined),
};

export const ticketApi = {
  fares: () => api.get<TicketFare[]>("/tickets/fares").then((r) => r.data),
  saveFare: (payload: TicketFare) => payload.id
    ? api.put<TicketFare>(`/tickets/fares/${payload.id}`, payload).then((r) => r.data)
    : api.post<TicketFare>("/tickets/fares", payload).then((r) => r.data),
  importFares: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api.post<ImportSummary>("/tickets/fares/import", form, { headers: { "Content-Type": "multipart/form-data" } }).then((r) => r.data);
  },
  ledger: (employeeId: string) =>
    api.get<TicketLedger[]>("/tickets/ledger", { params: { employeeId } }).then((r) => r.data),
  saveLedger: (payload: TicketLedger) =>
    api.post<TicketLedger>("/tickets/ledger", payload).then((r) => r.data),
  balance: (employeeId: string, asOfDate?: string) =>
    api.get<TicketBalance>("/tickets/balance", { params: { employeeId, ...(asOfDate ? { asOfDate } : {}) } }).then((r) => r.data),
  accrualReport: (params?: { projectId?: string; payGroup?: string; asOfDate?: string }) =>
    api.get<TicketAccrualReport>("/tickets/accrual-report", { params }).then((r) => r.data),
};

// --- Timesheet: public holidays ---
export const publicHolidayApi = {
  list: () => api.get<PublicHoliday[]>("/public-holidays").then((r) => r.data),
  create: (d: PublicHoliday) => api.post<PublicHoliday>("/public-holidays", d).then((r) => r.data),
  update: (id: string, d: PublicHoliday) =>
    api.put<PublicHoliday>(`/public-holidays/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/public-holidays/${id}`).then(() => undefined),
};

// --- Payroll calendars ---
export const calendarApi = {
  list: () => api.get<PayrollCalendar[]>("/payroll-calendars").then((r) => r.data),
  create: (d: PayrollCalendar) => api.post<PayrollCalendar>("/payroll-calendars", d).then((r) => r.data),
  update: (id: string, d: PayrollCalendar) =>
    api.put<PayrollCalendar>(`/payroll-calendars/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/payroll-calendars/${id}`).then(() => undefined),
};

// --- Payroll periods (months) ---
export const periodApi = {
  list: (year?: number) =>
    api.get<PayrollPeriod[]>("/payroll-periods", { params: year ? { year } : {} }).then((r) => r.data),
  get: (id: string) => api.get<PayrollPeriod>(`/payroll-periods/${id}`).then((r) => r.data),
  generate: (year: number, calendarId?: string) =>
    api
      .post<PayrollPeriod[]>("/payroll-periods/generate", null, {
        params: { year, ...(calendarId ? { calendarId } : {}) },
      })
      .then((r) => r.data),
  lock: (id: string) => api.post<PayrollPeriod>(`/payroll-periods/${id}/lock`).then((r) => r.data),
  close: (id: string) => api.post<PayrollPeriod>(`/payroll-periods/${id}/close`).then((r) => r.data),
  reopen: (id: string) => api.post<PayrollPeriod>(`/payroll-periods/${id}/reopen`).then((r) => r.data),
  remove: (id: string) => api.delete(`/payroll-periods/${id}`).then(() => undefined),
};

// --- Per-project period locks ---
export const periodLockApi = {
  statuses: (periodId: string, payGroup?: string) =>
    api.get<{ projectId: string; projectLabel: string; status: string; payGroup: string }[]>("/period-locks", { params: { periodId, ...(payGroup ? { payGroup } : {}) } }).then((r) => r.data),
  lock: (periodId: string, projectId: string, payGroup?: string) =>
    api.post<{ status: string; payGroup: string }>("/period-locks/lock", null, { params: { periodId, projectId, ...(payGroup ? { payGroup } : {}) } }).then((r) => r.data),
  startLock: (periodId: string, projectId: string, payGroup?: string) =>
    api.post<BulkStatusJob>("/period-locks/lock-jobs", null, { params: { periodId, projectId, ...(payGroup ? { payGroup } : {}) } }).then((r) => r.data),
  getLockJob: (id: string) =>
    api.get<BulkStatusJob>(`/period-locks/lock-jobs/${id}`).then((r) => r.data),
  close: (periodId: string, projectId: string, payGroup?: string) =>
    api.post<{ status: string; payGroup: string }>("/period-locks/close", null, { params: { periodId, projectId, ...(payGroup ? { payGroup } : {}) } }).then((r) => r.data),
  reopen: (periodId: string, projectId: string, payGroup?: string) =>
    api.post<{ status: string; payGroup: string }>("/period-locks/reopen", null, { params: { periodId, projectId, ...(payGroup ? { payGroup } : {}) } }).then((r) => r.data),
};

// --- Shift roster (employee -> shift) ---
export const employeeShiftApi = {
  list: (employeeId?: string) =>
    api
      .get<EmployeeShift[]>("/employee-shifts", { params: employeeId ? { employeeId } : {} })
      .then((r) => r.data),
  create: (d: EmployeeShift) => api.post<EmployeeShift>("/employee-shifts", d).then((r) => r.data),
  update: (id: string, d: EmployeeShift) =>
    api.put<EmployeeShift>(`/employee-shifts/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/employee-shifts/${id}`).then(() => undefined),
  bulkAssign: (shiftId: string, effectiveFrom: string, employeeIds: string[]) =>
    api
      .post<{ created: number }>("/employee-shifts/bulk", { shiftId, effectiveFrom, employeeIds })
      .then((r) => r.data),
};

// --- Crews + members ---
export const crewApi = {
  list: () => api.get<Crew[]>("/crews").then((r) => r.data),
  create: (d: Crew) => api.post<Crew>("/crews", d).then((r) => r.data),
  update: (id: string, d: Crew) => api.put<Crew>(`/crews/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/crews/${id}`).then(() => undefined),
  byEmployee: (employeeId: string) => api.get<Crew | null>(`/crews/by-employee/${employeeId}`).then((r) => r.data),
  members: (id: string) => api.get<CrewMember[]>(`/crews/${id}/members`).then((r) => r.data),
  addMember: (id: string, d: CrewMember) => api.post<CrewMember>(`/crews/${id}/members`, d).then((r) => r.data),
  bulkAddMembers: (id: string, shiftId: string | undefined, effectiveFrom: string, employeeIds: string[]) =>
    api.post<{ created: number }>(`/crews/${id}/members/bulk`, { shiftId, effectiveFrom, employeeIds }).then((r) => r.data),
  removeMember: (memberId: string) => api.delete(`/crews/members/${memberId}`).then(() => undefined),
  trades: (id: string) => api.get<CrewTrade[]>(`/crews/${id}/trades`).then((r) => r.data),
  addTrade: (id: string, d: CrewTrade) => api.post<CrewTrade>(`/crews/${id}/trades`, d).then((r) => r.data),
  removeTrade: (tradeId: string) => api.delete(`/crews/trades/${tradeId}`).then(() => undefined),
};

// --- Timekeeper -> project assignment ---
export const timekeeperApi = {
  list: () => api.get<TimekeeperProject[]>("/timekeeper-projects").then((r) => r.data),
  create: (d: TimekeeperProject) => api.post<TimekeeperProject>("/timekeeper-projects", d).then((r) => r.data),
  remove: (id: string) => api.delete(`/timekeeper-projects/${id}`).then(() => undefined),
  console: (date: string, timekeeperEmployeeId?: string) =>
    api.get<TimekeeperDay[]>("/timekeeper-projects/console", { params: { date, timekeeperEmployeeId } }).then((r) => r.data),
  mark: (d: TimekeeperMarkRequest) => api.post<TimekeeperDay>("/timekeeper-projects/console/mark", d).then((r) => r.data),
};

// --- Timesheet: monthly timesheets ---
export const timesheetApi = {
  listByPeriod: (year: number, month: number, projectId?: string, page = 0, size = 50, q?: string) =>
    api.get<PageResponse<Timesheet>>("/timesheets", {
      params: { year, month, page, size, ...(projectId ? { projectId } : {}), ...(q ? { q } : {}) },
    }).then((r) => r.data),
  get: (id: string) => api.get<Timesheet>(`/timesheets/${id}`).then((r) => r.data),
  summary: (id: string) => api.get<TimesheetSummary>(`/timesheets/${id}/summary`).then((r) => r.data),
  projectSummary: (year: number, month: number, projectId?: string) =>
    api.get<TimesheetProjectSummary[]>("/timesheets/project-summary", {
      params: { year, month, ...(projectId ? { projectId } : {}) },
    }).then((r) => r.data),
  eligibleEmployees: (periodId: string) =>
    api.get<Employee[]>("/timesheets/eligible-employees", { params: { periodId } }).then((r) => r.data),
  generate: (d: GenerateTimesheetRequest) =>
    api.post<Timesheet>("/timesheets/generate", d).then((r) => r.data),
  generateBulk: (periodId: string, projectId?: string) =>
    api.post<{ created: number; skipped: number }>("/timesheets/generate-bulk", null, { params: { periodId, projectId: projectId || undefined } }).then((r) => r.data),
  startGenerateBulk: (periodId: string, projectId?: string) =>
    api.post<BulkTimesheetJob>("/timesheets/generate-bulk-jobs", null, { params: { periodId, projectId: projectId || undefined } }).then((r) => r.data),
  getGenerateBulkJob: (id: string) =>
    api.get<BulkTimesheetJob>(`/timesheets/generate-bulk-jobs/${id}`).then((r) => r.data),
  generateByCrew: (crewId: string, periodId: string) =>
    api.post<{ created: number; skipped: number; messages?: string[] }>("/timesheets/generate-by-crew", null, { params: { crewId, periodId } }).then((r) => r.data),
  submitAll: (year: number, month: number, projectId?: string) =>
    api.post<{ submitted: number }>("/timesheets/submit-all", null, { params: { year, month, ...(projectId ? { projectId } : {}) } }).then((r) => r.data),
  startSubmitAll: (year: number, month: number, projectId?: string) =>
    api.post<BulkStatusJob>("/timesheets/submit-all-jobs", null, { params: { year, month, ...(projectId ? { projectId } : {}) } }).then((r) => r.data),
  getSubmitAllJob: (id: string) =>
    api.get<BulkStatusJob>(`/timesheets/submit-all-jobs/${id}`).then((r) => r.data),
  approveAll: (year: number, month: number, projectId?: string) =>
    api.post<{ approved: number }>("/timesheets/approve-all", null, { params: { year, month, ...(projectId ? { projectId } : {}) } }).then((r) => r.data),
  startApproveAll: (year: number, month: number, projectId?: string) =>
    api.post<BulkStatusJob>("/timesheets/approve-all-jobs", null, { params: { year, month, ...(projectId ? { projectId } : {}) } }).then((r) => r.data),
  getApproveAllJob: (id: string) =>
    api.get<BulkStatusJob>(`/timesheets/approve-all-jobs/${id}`).then((r) => r.data),
  saveDays: (id: string, days: TimesheetDay[]) =>
    api.put<Timesheet>(`/timesheets/${id}/days`, days).then((r) => r.data),
  submit: (id: string) => api.post<Timesheet>(`/timesheets/${id}/submit`).then((r) => r.data),
  approve: (id: string) => api.post<Timesheet>(`/timesheets/${id}/approve`).then((r) => r.data),
  lock: (id: string) => api.post<Timesheet>(`/timesheets/${id}/lock`).then((r) => r.data),
  reopen: (id: string) => api.post<Timesheet>(`/timesheets/${id}/reopen`).then((r) => r.data),
  remove: (id: string) => api.delete(`/timesheets/${id}`).then(() => undefined),
};

// --- Payroll components ---
export const payrollComponentApi = {
  list: (category?: string) =>
    api
      .get<PayrollComponent[]>("/payroll-components", { params: category ? { category } : {} })
      .then((r) => r.data),
  initializeDefaults: () =>
    api.post<PayrollComponent[]>("/payroll-components/initialize-defaults").then((r) => r.data),
  create: (d: PayrollComponent) => api.post<PayrollComponent>("/payroll-components", d).then((r) => r.data),
  update: (id: string, d: PayrollComponent) =>
    api.put<PayrollComponent>(`/payroll-components/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/payroll-components/${id}`).then(() => undefined),
};

export const payrollRunApi = {
  list: (periodId?: string) =>
    api.get<PayrollRun[]>("/payroll-runs", { params: periodId ? { periodId } : {} }).then((r) => r.data),
  get: (id: string) => api.get<PayrollRun>(`/payroll-runs/${id}`).then((r) => r.data),
  create: (periodId: string, projectId?: string, payGroup?: string) =>
    api.post<PayrollRun>("/payroll-runs", null, { params: { periodId, ...(projectId ? { projectId } : {}), ...(payGroup ? { payGroup } : {}) } }).then((r) => r.data),
  calculate: (id: string) => api.post<PayrollRun>(`/payroll-runs/${id}/calculate`).then((r) => r.data),
  startCalculate: (id: string) => api.post<BulkStatusJob>(`/payroll-runs/${id}/calculate/start`).then((r) => r.data),
  getCalculateJob: (id: string) => api.get<BulkStatusJob>(`/payroll-runs/calculate-jobs/${id}`).then((r) => r.data),
  approve: (id: string) => api.post<PayrollRun>(`/payroll-runs/${id}/approve`).then((r) => r.data),
  lock: (id: string) => api.post<PayrollRun>(`/payroll-runs/${id}/lock`).then((r) => r.data),
  delete: (id: string) => api.delete(`/payroll-runs/${id}`),
  results: (id: string, page: number, size: number, search?: string) =>
    api.get<{ content: PayrollResult[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }>(
      `/payroll-runs/${id}/results`, { params: { page, size, ...(search ? { search } : {}) } }
    ).then((r) => r.data),
};

export const provisionApi = {
  list: (periodId?: string) =>
    api.get<ProvisionRun[]>("/provisions", { params: periodId ? { periodId } : {} }).then((r) => r.data),
  get: (id: string) => api.get<ProvisionRun>(`/provisions/${id}`).then((r) => r.data),
  calculate: (payload: ProvisionCreateRequest) =>
    api.post<ProvisionRun>("/provisions", payload).then((r) => r.data),
  startCalculate: (payload: ProvisionCreateRequest) =>
    api.post<BulkStatusJob>("/provisions/calculate-jobs", payload).then((r) => r.data),
  getCalculateJob: (id: string) =>
    api.get<BulkStatusJob>(`/provisions/calculate-jobs/${id}`).then((r) => r.data),
  delete: (id: string) => api.delete(`/provisions/${id}`).then(() => undefined),
};

export const provisionRuleApi = {
  list: () => api.get<ProvisionRule[]>("/provision-rules").then((r) => r.data),
  save: (payload: ProvisionRule) => payload.id
    ? api.put<ProvisionRule>(`/provision-rules/${payload.id}`, payload).then((r) => r.data)
    : api.post<ProvisionRule>("/provision-rules", payload).then((r) => r.data),
  initializeDefaults: () =>
    api.post<ProvisionRule[]>("/provision-rules/initialize-defaults").then((r) => r.data),
  delete: (id: string) => api.delete(`/provision-rules/${id}`).then(() => undefined),
};

export const payrollRuleApi = {
  list: () => api.get<import("./types").PayrollRule[]>("/payroll-rules").then((r) => r.data),
  create: (payload: import("./types").PayrollRule) => api.post<import("./types").PayrollRule>("/payroll-rules", payload).then((r) => r.data),
  update: (id: string, payload: import("./types").PayrollRule) =>
    api.put<import("./types").PayrollRule>(`/payroll-rules/${id}`, payload).then((r) => r.data),
};

export const payrollReportApi = {
  payrollListingSummary: (runId: string) =>
    api.get<import("./types").PayrollListingSummary>(`/payroll/reports/payroll-listing/${runId}/summary`).then((r) => r.data),
  payrollListingRows: (runId: string, page: number, size: number, search?: string) =>
    api.get<{ content: import("./types").PayrollListingRow[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }>(
      `/payroll/reports/payroll-listing/${runId}/rows`, { params: { page, size, ...(search ? { search } : {}) } }
    ).then((r) => r.data),
  costAllocationSummary: (periodId: string, projectId?: string) =>
    api.get<CostCodeLine[]>("/payroll/reports/cost-allocation/summary", { params: { periodId, ...(projectId ? { projectId } : {}) } }).then((r) => r.data),
  costAllocationEmployees: (periodId: string, projectId: string | undefined, page: number, size: number, search?: string) =>
    api.get<{ content: EmployeeCostBreakdown[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }>(
      "/payroll/reports/cost-allocation/employees", { params: { periodId, ...(projectId ? { projectId } : {}), page, size, ...(search ? { search } : {}) } }
    ).then((r) => r.data),
  costControl: (periodId: string, projectId?: string) =>
    api.get<import("./types").PayrollCostControlReport>("/payroll/reports/cost-control", {
      params: { periodId, ...(projectId ? { projectId } : {}) },
    }).then((r) => r.data),
};

export const dayZeroApi = {
  days: (employeeId: string) =>
    api.get<import("./types").DayZeroDay[]>(`/day-zero/employees/${employeeId}/days`).then((r) => r.data),
  correct: (employeeId: string, corrections: Record<string, import("./types").DayZeroCorrection>, note?: string) =>
    api.post<{ adjustmentsCreated: number; lines: { workDate: string; amount: number }[] }>(
      `/day-zero/employees/${employeeId}/correct`, { corrections, note }
    ).then((r) => r.data),
};

export const auditApi = {
  dayZeroAdjustments: (employeeId: string) =>
    api.get<import("./types").AuditDayZeroAdjustment[]>("/audit/day-zero-adjustments", { params: { employeeId } }).then((r) => r.data),
  deleteDayZeroAdjustment: (id: string) => api.delete(`/audit/day-zero-adjustments/${id}`).then(() => undefined),
  payrollRuns: (limit = 50) =>
    api.get<import("./types").AuditPayrollRun[]>("/audit/payroll-runs", { params: { limit } }).then((r) => r.data),
  deletePayrollRun: (id: string) => api.delete(`/audit/payroll-runs/${id}`).then(() => undefined),
  timeUsage: (employeeId: string, year: number) =>
    api.get<import("./types").EmployeeTimeTypeUsage>("/audit/time-usage", { params: { employeeId, year } }).then((r) => r.data),
  leaveDiscrepancies: (employeeId: string) =>
    api.get<import("./types").AuditLeaveDiscrepancy[]>("/audit/leave-discrepancies", { params: { employeeId } }).then((r) => r.data),
  recalculateLeaveRequest: (leaveRequestId: string, newTotalDays: number) =>
    api.post(`/audit/leave-requests/${leaveRequestId}/recalculate`, { newTotalDays }).then(() => undefined),
};

export const selfApi = {
  payslips: () => api.get<import("./types").PayrollResult[]>("/me/payslips").then((r) => r.data),
  payslipDetail: (resultId: string) => api.get<import("./types").PayrollResult>(`/me/payslips/${resultId}`).then((r) => r.data),
  timesheet: (year: number, month: number) =>
    api.get<import("./types").Timesheet>("/me/timesheet", { params: { year, month } }).then((r) => r.data),
  leaveTypes: () => api.get<import("./types").LeaveType[]>("/me/leave-types").then((r) => r.data),
  leaveRequests: () => api.get<import("./types").LeaveRequest[]>("/me/leave-requests").then((r) => r.data),
  leaveBalance: () => api.get<import("./types").LeaveBalance[]>("/me/leave-balance").then((r) => r.data),
  submitLeaveRequest: (dto: Partial<import("./types").LeaveRequest>) =>
    api.post<import("./types").LeaveRequest>("/me/leave-requests", dto).then((r) => r.data),
};
