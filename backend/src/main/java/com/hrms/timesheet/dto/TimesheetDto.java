package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TimesheetDto {

    private UUID id;
    private UUID companyId;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private int periodYear;
    private int periodMonth;
    private UUID shiftId;
    private String status;
    private BigDecimal totalWorkedHours;
    private BigDecimal totalOtHours;
    private BigDecimal totalAbsenceDays;
    private Instant submittedAt;
    private Instant approvedAt;
    private String approvedBy;
    private List<TimesheetDayDto> days = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalWorkedHours() { return totalWorkedHours; }
    public void setTotalWorkedHours(BigDecimal totalWorkedHours) { this.totalWorkedHours = totalWorkedHours; }

    public BigDecimal getTotalOtHours() { return totalOtHours; }
    public void setTotalOtHours(BigDecimal totalOtHours) { this.totalOtHours = totalOtHours; }

    public BigDecimal getTotalAbsenceDays() { return totalAbsenceDays; }
    public void setTotalAbsenceDays(BigDecimal totalAbsenceDays) { this.totalAbsenceDays = totalAbsenceDays; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public List<TimesheetDayDto> getDays() { return days; }
    public void setDays(List<TimesheetDayDto> days) { this.days = days; }
}
