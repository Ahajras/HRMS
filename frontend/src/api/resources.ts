import { api } from "./client";
import type {
  Country,
  Currency,
  Employee,
  OrgUnitTreeNode,
  OrgUnitType,
  OrganizationUnit,
  PageResponse,
  PayrollComponent,
} from "./types";

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
  list: (page = 0, size = 20) =>
    api.get<PageResponse<Employee>>("/employees", { params: { page, size } }).then((r) => r.data),
  get: (id: string) => api.get<Employee>(`/employees/${id}`).then((r) => r.data),
  create: (d: Employee) => api.post<Employee>("/employees", d).then((r) => r.data),
  update: (id: string, d: Employee) => api.put<Employee>(`/employees/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/employees/${id}`).then(() => undefined),
};

// --- Payroll components ---
export const payrollComponentApi = {
  list: (category?: string) =>
    api
      .get<PayrollComponent[]>("/payroll-components", { params: category ? { category } : {} })
      .then((r) => r.data),
  create: (d: PayrollComponent) => api.post<PayrollComponent>("/payroll-components", d).then((r) => r.data),
  update: (id: string, d: PayrollComponent) =>
    api.put<PayrollComponent>(`/payroll-components/${id}`, d).then((r) => r.data),
  remove: (id: string) => api.delete(`/payroll-components/${id}`).then(() => undefined),
};
