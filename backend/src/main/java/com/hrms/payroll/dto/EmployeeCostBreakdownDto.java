package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** One employee's hours+value broken down by (project, cost code) for a payroll run. */
public class EmployeeCostBreakdownDto {

    private UUID employeeId;
    private String employeeNumber;
    private String employeeName;
    private List<CostCodeLineDto> lines;
    private BigDecimal totalHours = BigDecimal.ZERO;
    private BigDecimal totalValue = BigDecimal.ZERO;

    public EmployeeCostBreakdownDto() { }

    public EmployeeCostBreakdownDto(UUID employeeId, String employeeNumber, String employeeName,
                                    List<CostCodeLineDto> lines, BigDecimal totalHours, BigDecimal totalValue) {
        this.employeeId = employeeId;
        this.employeeNumber = employeeNumber;
        this.employeeName = employeeName;
        this.lines = lines;
        this.totalHours = totalHours;
        this.totalValue = totalValue;
    }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public List<CostCodeLineDto> getLines() { return lines; }
    public void setLines(List<CostCodeLineDto> lines) { this.lines = lines; }
    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
}
