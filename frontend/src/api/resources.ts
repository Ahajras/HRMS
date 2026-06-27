import { api } from "./client";
import type {
  Assignment,
  Bank,
  Contract,
  ContractPayItem,
  Country,
  Currency,
  Employee,
  EmployeeBankAccount,
  EmployeeDocument,
  ImportSummary,
  LegacyRaw,
  LookupValue,
  OrgUnitTreeNode,
  OrgUnitType,
  OrganizationUnit,
  PageResponse,
  PayrollComponent,
  Project,
  CostCode,
  Rule,
  RulePackage,
} from "./types";

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
  list: (page = 0, size = 20, q?: string) =>
    api.get<PageResponse<Employee>>("/employees", { params: { page, size, ...(q ? { q } : {}) } }).then((r) => r.data),
  get: (id: string) => api.get<Employee>(`/employees/${id}`).then((r) => r.data),
  create: (d: Employee) => api.post<Employee>("/employees", d).then((r) => r.data),
  update: (id: string, d: Employee) => api.put<Employee>(`/employees/${id}`, d).then((r) => r.data),
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
