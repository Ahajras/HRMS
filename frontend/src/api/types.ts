// Mirror of backend DTOs (Phase 1 master data + Phase 2 security).

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
  nationalityCountryCode?: string;
  dateOfBirth?: string;
  gender?: string;
  hireDate: string;
  terminationDate?: string;
  email?: string;
  phone?: string;
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
