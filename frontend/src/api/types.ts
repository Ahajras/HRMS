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
