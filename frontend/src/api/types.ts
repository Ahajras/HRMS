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
  arabicName?: string;
}

export interface EmployeeSummary {
  total: number;
  active: number;
  notActive: number;
  monthly: number;
  daily: number;
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
export interface Shift {
  id?: string;
  companyId?: string;
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
  projectId?: string;
  costCodeId?: string;
  remarks?: string;
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
