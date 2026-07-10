package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TimesheetDayDto {

    private UUID id;
    private UUID timesheetId;
    private LocalDate workDate;
    private UUID shiftId;
    private UUID timeTypeId;
    private UUID leaveRequestId;
    private String timeTypeCode;
    private BigDecimal plannedHours;
    private LocalTime actualIn;
    private LocalTime actualOut;
    private BigDecimal workedHours;
    private BigDecimal otHours;
    private BigDecimal normalHours;
    private BigDecimal declaredOtHours;
    private BigDecimal undeclaredOtHours;
    private BigDecimal ineligibleOtHours;
    private UUID projectId;
    private UUID costCodeId;
    private String remarks;
    private boolean estimated;
    private java.math.BigDecimal dayZeroAdjustmentAmount;
    private String dayZeroAdjustmentReason;
    /** Optional split of worked hours across cost codes. */
    private List<TimesheetDayCostDto> costs = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTimesheetId() { return timesheetId; }
    public void setTimesheetId(UUID timesheetId) { this.timesheetId = timesheetId; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public UUID getTimeTypeId() { return timeTypeId; }
    public void setTimeTypeId(UUID timeTypeId) { this.timeTypeId = timeTypeId; }

    public UUID getLeaveRequestId() { return leaveRequestId; }
    public void setLeaveRequestId(UUID leaveRequestId) { this.leaveRequestId = leaveRequestId; }

    public String getTimeTypeCode() { return timeTypeCode; }
    public void setTimeTypeCode(String timeTypeCode) { this.timeTypeCode = timeTypeCode; }

    public BigDecimal getPlannedHours() { return plannedHours; }
    public void setPlannedHours(BigDecimal plannedHours) { this.plannedHours = plannedHours; }

    public LocalTime getActualIn() { return actualIn; }
    public void setActualIn(LocalTime actualIn) { this.actualIn = actualIn; }

    public LocalTime getActualOut() { return actualOut; }
    public void setActualOut(LocalTime actualOut) { this.actualOut = actualOut; }

    public BigDecimal getWorkedHours() { return workedHours; }
    public void setWorkedHours(BigDecimal workedHours) { this.workedHours = workedHours; }

    public BigDecimal getOtHours() { return otHours; }
    public void setOtHours(BigDecimal otHours) { this.otHours = otHours; }

    public BigDecimal getNormalHours() { return normalHours; }
    public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }

    public BigDecimal getDeclaredOtHours() { return declaredOtHours; }
    public void setDeclaredOtHours(BigDecimal declaredOtHours) { this.declaredOtHours = declaredOtHours; }

    public BigDecimal getUndeclaredOtHours() { return undeclaredOtHours; }
    public void setUndeclaredOtHours(BigDecimal undeclaredOtHours) { this.undeclaredOtHours = undeclaredOtHours; }

    public BigDecimal getIneligibleOtHours() { return ineligibleOtHours; }
    public void setIneligibleOtHours(BigDecimal ineligibleOtHours) { this.ineligibleOtHours = ineligibleOtHours; }

    public List<TimesheetDayCostDto> getCosts() { return costs; }
    public void setCosts(List<TimesheetDayCostDto> costs) { this.costs = costs; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public boolean isEstimated() { return estimated; }
    public void setEstimated(boolean estimated) { this.estimated = estimated; }

    public java.math.BigDecimal getDayZeroAdjustmentAmount() { return dayZeroAdjustmentAmount; }
    public void setDayZeroAdjustmentAmount(java.math.BigDecimal dayZeroAdjustmentAmount) { this.dayZeroAdjustmentAmount = dayZeroAdjustmentAmount; }

    public String getDayZeroAdjustmentReason() { return dayZeroAdjustmentReason; }
    public void setDayZeroAdjustmentReason(String dayZeroAdjustmentReason) { this.dayZeroAdjustmentReason = dayZeroAdjustmentReason; }
}
