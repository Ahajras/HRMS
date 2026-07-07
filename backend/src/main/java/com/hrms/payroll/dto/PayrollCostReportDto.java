package com.hrms.payroll.dto;

import java.util.List;
import java.util.UUID;

/** Cost allocation report for one payroll run, in both directions:
 *  - byEmployee: each employee's hours/value split across cost codes.
 *  - byCostCode: each cost code's total hours/value across all employees. */
public class PayrollCostReportDto {

    private UUID runId;
    private List<EmployeeCostBreakdownDto> byEmployee;
    private List<CostCodeLineDto> byCostCode;

    public PayrollCostReportDto() { }

    public PayrollCostReportDto(UUID runId, List<EmployeeCostBreakdownDto> byEmployee, List<CostCodeLineDto> byCostCode) {
        this.runId = runId;
        this.byEmployee = byEmployee;
        this.byCostCode = byCostCode;
    }

    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
    public List<EmployeeCostBreakdownDto> getByEmployee() { return byEmployee; }
    public void setByEmployee(List<EmployeeCostBreakdownDto> byEmployee) { this.byEmployee = byEmployee; }
    public List<CostCodeLineDto> getByCostCode() { return byCostCode; }
    public void setByCostCode(List<CostCodeLineDto> byCostCode) { this.byCostCode = byCostCode; }
}
