// Mirror of backend DTOs (Phase 1 master data + Phase 2 security).

// --- Legacy import (DBF snapshot -> HRMS) ---
export interface ImportSamplePreview {
  employeeNumber: string;
  name: string;
  nationality?: string | null;
  hireDate: string;
  pay?: string | null;
  action: "NEW" | "UPDATE";
}

export interface ImportSummary {
  committed: boolean;
  sourceHeaderRows: number;
  sourceDetailRows: number;
  sourceDependentRows: number;
  counts: Record<string, number>;
  warnings: string[];
  sample: ImportSamplePreview[];
}

// Faithful legacy snapshot for one employee: full header row + all detail
// (pay) lines, every legacy column preserved (blanks kept).
export interface LegacyRaw {
  employeeNumber: string;
  source?: string | null;
  importedAt?: string | null;
  header: Record<string, string>;
  detail: Record<string, string>[];
}

export interface AuthUser {
  id?: string;
  companyId?: string;
  employeeId?: string;
  username: string;
  email?: string;
  fullName?: string;
  status?: string;
  lastLoginAt?: string;
  roles: string[];
  authorities: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMinutes: number;
  user: AuthUser;
}

export interface UserPayload {
  id?: string;
  companyId?: string;
  employeeId?: string;
  username: string;
  email?: string;
  password?: string;
  fullName?: string;
  status?: string;
  roles: string[];
  authorities?: string[];
}

export interface Role {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  description?: string;
  permissions: string[];
}

export interface CompanyProfile {
  id?: string;
  companyId?: string;
  companyName: string;
  legalName?: string;
  taxNumber?: string;
  registrationNo?: string;
  email?: string;
  phone?: string;
  website?: string;
  addressLine?: string;
  logoUrl?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Currency {
  id?: string;
  code: string;
  name: string;
  symbol?: string;
  minorUnits: number;
  status?: string;
}

export interface Country {
  id?: string;
  code: string;
  name: string;
  defaultCurrencyCode?: string;
  status?: string;
}

export interface OrgUnitType {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  levelOrder: number;
  mandatory: boolean;
  status?: string;
}

export interface OrgUnitTreeNode {
  id: string;
  parentId?: string;
  typeId: string;
  code: string;
  name: string;
  effectiveFrom: string;
  effectiveTo?: string;
  status?: string;
  children: OrgUnitTreeNode[];
}

export interface OrganizationUnit {
  id?: string;
  companyId?: string;
  parentId?: string;
  typeId: string;
  code: string;
  name: string;
  effectiveFrom: string;
  effectiveTo?: string;
  status?: string;
}

export interface Employee {
  id?: string;
  companyId?: string;
  projectId?: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  middleName?: string;
  nationalityCountryCode?: string;
  maritalStatus?: string;
  addressLine?: string;
  city?: string;
  countryOfResidenceCode?: string;
  dateOfBirth?: string;
  gender?: string;
  hireDate: string;
  terminationDate?: string;
  email?: string;
  phone?: string;
  status?: string;
  jobTitle?: string;
  jobTitleCode?: string;
  payStatus?: string;
  overtimeCategoryCode?: string;
  band?: string;
  arabicName?: string;
  supervisorEmployeeId?: string;
  supervisorName?: string;
  timekeeperEmployeeId?: string;
  timekeeperName?: string;
  photoUrl?: string;
}

export interface EmployeeSummary {
  total: number;
  active: number;
  notActive: number;
  monthly: number;
  daily: number;
  withoutProject: number;
}

export interface EmployeeProjectSummary {
  projectId: string;
  projectCode: string;
  projectName: string;
  total: number;
  active: number;
  monthly: number;
  daily: number;
}

export interface EmployeeTimeTypeUsage {
  year: number;
  rows: EmployeeTimeTypeUsageRow[];
}

export interface EmployeeTimeTypeUsageRow {
  timeTypeCode: string;
  timeTypeName: string;
  category: string;
  usedDays: number;
  usedHours: number;
  occurrences: number;
  firstDate?: string;
  lastDate?: string;
  thresholdDays: number;
  thresholdScope?: string;
}

export interface BulkTimesheetJob {
  id: string;
  status: "RUNNING" | "COMPLETED" | "FAILED";
  created: number;
  skipped: number;
  processed: number;
  total: number;
  elapsedSeconds: number;
  durationSeconds?: number;
  message?: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface BulkStatusJob {
  id: string;
  status: "RUNNING" | "COMPLETED" | "FAILED";
  done: number;
  total: number;
  elapsedSeconds: number;
  durationSeconds?: number;
  message?: string;
  startedAt?: string;
  finishedAt?: string;
  result?: Record<string, unknown>;
}

export interface TimesheetProjectSummary {
  projectId: string;
  projectCode: string;
  projectName: string;
  eligible: number;
  generated: number;
  missing: number;
  draft: number;
  submitted: number;
  approved: number;
  locked: number;
}

export interface LeaveType {
  id?: string;
  code: string;
  name: string;
  timeTypeId: string;
  timeTypeCode?: string;
  deductsBalance: boolean;
  paid: boolean;
  requiresTicketDefault: boolean;
  status?: string;
}

export interface LeaveProjectSummary {
  projectId: string;
  projectCode: string;
  projectName: string;
  total: number;
  pending: number;
  approved: number;
  rejected: number;
  approvedDays: number;
}

export interface LeaveRequest {
  id?: string;
  employeeId: string;
  employeeNumber?: string;
  employeeName?: string;
  leaveTypeId: string;
  leaveTypeCode?: string;
  leaveTypeName?: string;
  startDate: string;
  endDate: string;
  returnDate?: string;
  totalDays?: number;
  reason?: string;
  status?: string;
  requiresTicket: boolean;
  ticketFrom?: string;
  ticketTo?: string;
  travelDate?: string;
  returnTravelDate?: string;
  destination?: string;
  travelRemarks?: string;
  contactPhone?: string;
  contactEmail?: string;
  addressDuringLeave?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
}

export interface LeaveAdjustment {
  id?: string;
  employeeId: string;
  leaveTypeId: string;
  leaveTypeCode?: string;
  adjustmentType: string;
  days: number;
  effectiveDate: string;
  reason?: string;
}

export interface LeaveBalance {
  employeeId: string;
  leaveTypeId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  asOfDate: string;
  annualRate: number;
  entitledToDate: number;
  adjustments: number;
  usedApproved: number;
  pending: number;
  balance: number;
}

export interface LookupValue {
  id?: string;
  companyId?: string;
  category: string;
  code: string;
  label: string;
  sortOrder?: number;
  status?: string;
}

export interface Bank {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  swiftCode?: string;
  countryCode?: string;
  status?: string;
}

export interface Contract {
  id?: string;
  employeeId: string;
  contractNumber?: string;
  contractType: string;
  effectiveFrom: string;
  effectiveTo?: string;
  baseCurrencyCode?: string;
  status?: string;
  // Reference-only standard terms (actual worked hours come from the timesheet/shift).
  workingHoursPerWeek?: number;
  workingDaysPerWeek?: number;
  overtimeCategory?: string;
  overtimeCategoryDesc?: string;
}

export interface ContractPayItem {
  id?: string;
  contractId: string;
  employeeId?: string;
  payComponentId: string;
  amount: number;
  currencyCode?: string;
  effectiveFrom: string;
  effectiveTo?: string;
  status?: string;
  remarks?: string;
  actionSheetNo?: string;
}

export interface Assignment {
  id?: string;
  employeeId: string;
  organizationUnitId: string;
  positionTitle?: string;
  supervisorEmployeeId?: string;
  projectId?: string;
  costCodeId?: string;
  primaryAssignment?: boolean;
  effectiveFrom: string;
  effectiveTo?: string;
  status?: string;
}

export interface Project {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  managerEmployeeId?: string;
  status?: string;
}

export interface CostCode {
  id?: string;
  companyId?: string;
  projectId: string;
  code: string;
  name: string;
  status?: string;
}

export interface RulePackage {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  countryCode?: string;
  status?: string;
}

export interface Rule {
  id?: string;
  packageId: string;
  companyId?: string;
  code: string;
  category: string;
  name: string;
  valueType: string;
  valueNumber?: number;
  valueText?: string;
  unit?: string;
  effectiveFrom: string;
  effectiveTo?: string;
  status?: string;
  description?: string;
}

export interface EmployeeDocument {
  id?: string;
  employeeId: string;
  documentType: string;
  documentNumber: string;
  issuingCountryCode?: string;
  issueDate?: string;
  expiryDate?: string;
  issuingAuthority?: string;
  status?: string;
}

export interface EmployeeBankAccount {
  id?: string;
  employeeId: string;
  bankId?: string;
  accountHolderName?: string;
  iban?: string;
  accountNumber?: string;
  currencyCode?: string;
  primary?: boolean;
  status?: string;
}

// --- Timesheet / Shift / Time Type (Phase 4, FTDD Vol.1 Ch.3-5) ---
export interface ShiftDay {
  id?: string;
  dayOfWeek: string;
  normalHours?: number;
  declaredOt?: number;
  weeklyOff: boolean;
}

export interface Shift {
  id?: string;
  companyId?: string;
  projectId?: string;
  code: string;
  name: string;
  startTime?: string;
  endTime?: string;
  breakMinutes: number;
  standardHours?: number;
  crossesMidnight: boolean;
  weeklyOff?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  status?: string;
  days?: ShiftDay[];
}

export interface TimeType {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  category?: string;
  paid: boolean;
  countsAsWorked: boolean;
  affectsLeave: boolean;
  factor?: number;
  sortOrder: number;
  status?: string;
}

export interface PublicHoliday {
  id?: string;
  companyId?: string;
  holidayDate: string;
  name: string;
  status?: string;
}

export interface OvertimeCategory {
  id?: string;
  code: string;
  name: string;
  otEligible: boolean;
  status?: string;
}

export interface TimesheetSummaryLine {
  category: string;
  days: number;
  hours: number;
  paid: boolean;
}

export interface TimesheetAllocationLine {
  projectId: string;
  projectCode?: string;
  projectName?: string;
  costCodeId: string;
  costCode?: string;
  costCodeName?: string;
  hours: number;
}

export interface TimesheetSummary {
  timesheetId: string;
  employeeId: string;
  employeeName?: string;
  periodYear: number;
  periodMonth: number;
  status: string;
  normalHours: number;
  overtimeHours: number;
  workedHours: number;
  restHours: number;
  holidayHours: number;
  absenceHours: number;
  leaveHours: number;
  workedDays: number;
  absenceDays: number;
  leaveDays: number;
  restDays: number;
  holidayDays: number;
  totalDays: number;
  lines: TimesheetSummaryLine[];
  allocationLines: TimesheetAllocationLine[];
}

export interface TimesheetDayCost {
  id?: string;
  projectId?: string;
  costCodeId?: string;
  hours?: number;
}

export interface TimesheetDay {
  id?: string;
  timesheetId?: string;
  workDate: string;
  shiftId?: string;
  timeTypeId?: string;
  timeTypeCode?: string;
  plannedHours?: number;
  actualIn?: string | null;
  actualOut?: string | null;
  workedHours?: number;
  otHours?: number;
  normalHours?: number;
  declaredOtHours?: number;
  undeclaredOtHours?: number;
  ineligibleOtHours?: number;
  projectId?: string;
  costCodeId?: string;
  remarks?: string;
  costs?: TimesheetDayCost[];
}

export interface Timesheet {
  id?: string;
  companyId?: string;
  employeeId: string;
  employeeName?: string;
  employeeNumber?: string;
  periodYear: number;
  periodMonth: number;
  shiftId?: string;
  status?: string;
  totalWorkedHours?: number;
  totalOtHours?: number;
  totalAbsenceDays?: number;
  submittedAt?: string;
  approvedAt?: string;
  approvedBy?: string;
  days: TimesheetDay[];
}

export interface TimeTypePayrollRule {
  id?: string;
  timeTypeId: string;
  payrollComponentId: string;
  payrollComponentCode?: string;
  payrollComponentName?: string;
  action: string;
  percent: number;
  basis: string;
  thresholdDays?: number;
  thresholdScope?: string;
  yearBasis?: string;
  affectsOvertime: boolean;
  processSeparately: boolean;
  sortOrder: number;
  remarks?: string;
}

// --- Crew / Foreman + Timekeeper (FTDD Vol.1 Ch.4) ---
export interface Crew {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  projectId?: string;
  projectCode?: string;
  foremanEmployeeId?: string;
  foremanName?: string;
  parentCrewId?: string;
  status?: string;
  memberCount?: number;
}

export interface CrewMember {
  id?: string;
  crewId?: string;
  employeeId: string;
  employeeName?: string;
  employeeNumber?: string;
  shiftId?: string;
  shiftCode?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  status?: string;
}

export interface CrewTrade {
  id?: string;
  crewId?: string;
  tradeCode: string;
  tradeName?: string;
  plannedCount: number;
  assignedCount?: number;
}

export interface TimekeeperProject {
  id?: string;
  employeeId: string;
  employeeName?: string;
  employeeNumber?: string;
  projectId: string;
  projectCode?: string;
  status?: string;
}

export interface GenerateTimesheetRequest {
  employeeId: string;
  periodId: string;
  year?: number;
  month?: number;
  shiftId?: string;
  overwrite?: boolean;
}

// --- Payroll Calendar / Period / Week (FTDD Vol.1 Ch.4) ---
export interface PayrollCalendar {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  frequency?: string;
  weekStart?: string;
  status?: string;
}

export interface PayrollWeek {
  id?: string;
  periodId?: string;
  weekNo: number;
  startDate: string;
  endDate: string;
}

export interface PayrollPeriod {
  id?: string;
  companyId?: string;
  calendarId?: string;
  periodYear: number;
  periodMonth: number;
  name: string;
  startDate: string;
  endDate: string;
  payDate?: string;
  status?: string;
  lockedAt?: string;
  closedAt?: string;
  weeks: PayrollWeek[];
}

export interface EmployeeShift {
  id?: string;
  companyId?: string;
  employeeId: string;
  employeeName?: string;
  employeeNumber?: string;
  shiftId: string;
  shiftCode?: string;
  effectiveFrom: string;
  effectiveTo?: string;
  status?: string;
}

export interface PayrollComponent {
  id?: string;
  companyId?: string;
  code: string;
  name: string;
  category: string;
  componentType: string;
  paymentFrequency: string;
  calculationMethod: string;
  roundingMethod: string;
  roundingScale: number;
  currencyCode?: string;
  priority: number;
  taxable: boolean;
  insurable: boolean;
  wpsIncluded: boolean;
  eosIncluded: boolean;
  provisionIncluded: boolean;
  leaveIncluded: boolean;
  visibleOnPayslip: boolean;
  visibleOnReports: boolean;
  costAllocationRequired: boolean;
  approvalRequired: boolean;
  effectiveFrom: string;
  effectiveTo?: string;
  status?: string;
  remarks?: string;
}

export interface PayrollResultLine {
  id?: string;
  componentCode?: string;
  componentName: string;
  componentType: string;
  category?: string;
  quantity?: number;
  rate?: number;
  amount: number;
  source?: string;
  details?: string;
}

export interface PayrollResult {
  id?: string;
  employeeId: string;
  employeeNumber?: string;
  employeeName?: string;
  payStatus?: string;
  workedDays: number;
  normalHours: number;
  otHours: number;
  gross: number;
  totalEarnings: number;
  totalDeductions: number;
  net: number;
  status?: string;
  message?: string;
  lines: PayrollResultLine[];
}

export interface PayrollRun {
  id?: string;
  periodId: string;
  periodName?: string;
  periodYear?: number;
  periodMonth?: number;
  periodStartDate?: string;
  periodEndDate?: string;
  payDate?: string;
  projectId?: string;
  payGroup?: string;
  runType?: string;
  status?: string;
  currencyCode?: string;
  calculatedAt?: string;
  notes?: string;
  totalGross: number;
  totalNet: number;
  employeeCount: number;
  results: PayrollResult[];
}

export interface ProvisionResult {
  id?: string;
  employeeId: string;
  employeeNumber?: string;
  employeeName?: string;
  projectId?: string;
  payGroup?: string;
  eligibleAmount: number;
  provisionAmount: number;
  formulaNote?: string;
  status?: string;
  message?: string;
}

export interface ProvisionRun {
  id?: string;
  periodId: string;
  periodName?: string;
  periodStartDate?: string;
  periodEndDate?: string;
  projectId?: string;
  payGroup?: string;
  provisionType: string;
  status?: string;
  calculatedAt?: string;
  employeeCount: number;
  totalEligibleAmount: number;
  totalProvisionAmount: number;
  notes?: string;
  results: ProvisionResult[];
}

export interface ProvisionCreateRequest {
  periodId: string;
  projectId?: string;
  payGroup?: string;
  provisionType: string;
}

export interface ProvisionRule {
  id?: string;
  projectId?: string;
  payGroup: string;
  provisionType: string;
  name: string;
  basisMode: string;
  basisCategories?: string;
  basisComponentCodes?: string;
  formulaExpression: string;
  divisor: number;
  fixedAmount: number;
  entitlementDaysUnderFive: number;
  entitlementDaysFiveOrMore: number;
  ticketCycleMonths: number;
  effectiveFrom: string;
  effectiveTo?: string;
  status?: string;
  notes?: string;
}

export interface PayrollRule {
  id?: string;
  payGroup: string;
  payItemBasis: string;
  quantitySource?: string;
  otMultiplier: number;
  restDayOtMultiplier: number;
  standardHoursPerDay: number;
  weeklyRestPaid: boolean;
  monthDivisor: number;
  divisorMode?: string;
  projectId?: string | null;
  status?: string;
  dayZeroCutoffDay?: number | null;
  categoryRules?: PayrollCategoryRule[];
}

export interface PayrollCategoryRule {
  id?: string;
  payrollRuleId?: string;
  category: string;
  basis: string;
  divisorMode?: string;
  monthDivisor?: number | null;
  status?: string;
}

export interface TimekeeperDay {
  employeeId: string;
  employeeNumber?: string;
  employeeName?: string;
  timesheetId?: string;
  timesheetDayId?: string;
  workDate: string;
  timesheetStatus?: string;
  shiftCode?: string;
  shiftName?: string;
  plannedIn?: string;
  plannedOut?: string;
  actualIn?: string;
  actualOut?: string;
  timeTypeCode?: string;
  plannedHours?: number;
  workedHours?: number;
  normalHours?: number;
  otHours?: number;
  editable: boolean;
  blockedReason?: string;
}

export interface TimekeeperMarkRequest {
  employeeId: string;
  workDate: string;
  action: "ATTEND" | "LATE" | "CHECK_OUT" | "OUT_CUSTOM" | "ABSENT";
  actualIn?: string;
  actualOut?: string;
  remarks?: string;
}

export interface PayrollListingReport {
  runId: string;
  periodId: string;
  periodName?: string;
  periodStartDate?: string;
  periodEndDate?: string;
  projectId?: string | null;
  projectCode?: string;
  projectName?: string;
  payGroup?: string;
  status?: string;
  employeeCount: number;
  totalBasic: number;
  totalAllowances: number;
  totalOvertime: number;
  totalDeductions: number;
  totalGross: number;
  totalNet: number;
  componentCodes: string[];
  rows: PayrollListingRow[];
}

export type PayrollListingSummary = Omit<PayrollListingReport, "rows">;

export interface PayrollListingRow {
  employeeId: string;
  employeeNumber?: string;
  employeeName?: string;
  payGroup?: string;
  projectId?: string | null;
  projectCode?: string;
  projectName?: string;
  costCodeId?: string | null;
  costCode?: string;
  costCodeName?: string;
  workedDays: number;
  normalHours: number;
  otHours: number;
  basic: number;
  allowances: number;
  overtime: number;
  deductions: number;
  gross: number;
  net: number;
  status?: string;
  message?: string;
  componentAmounts: Record<string, number>;
}

export interface CostCodeLine {
  projectId?: string;
  projectCode?: string;
  projectName?: string;
  costCodeId?: string;
  costCodeCode?: string;
  costCodeName?: string;
  hours: number;
  value: number;
}

export interface EmployeeCostBreakdown {
  employeeId: string;
  employeeNumber?: string;
  employeeName?: string;
  lines: CostCodeLine[];
  totalHours: number;
  totalValue: number;
}

export interface PayrollCostReport {
  runId: string;
  byEmployee: EmployeeCostBreakdown[];
  byCostCode: CostCodeLine[];
}

export interface DayZeroDay {
  id: string;
  workDate: string;
  periodYear: number;
  periodMonth: number;
  timeTypeCode?: string;
  timeTypeName?: string;
}
