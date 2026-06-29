package com.hrms.timesheet.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One calendar day within a {@link Timesheet}. Carries the classification
 * (time type), the planned vs actual hours, computed worked/OT hours and the
 * cost-allocation target (project/cost code).
 */
@Entity
@Table(name = "timesheet_day")
public class TimesheetDay extends AuditableEntity {

    @Column(name = "timesheet_id", nullable = false)
    private UUID timesheetId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "shift_id")
    private UUID shiftId;

    @Column(name = "time_type_id")
    private UUID timeTypeId;

    @Column(name = "planned_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal plannedHours = BigDecimal.ZERO;

    @Column(name = "actual_in")
    private LocalTime actualIn;

    @Column(name = "actual_out")
    private LocalTime actualOut;

    @Column(name = "worked_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal workedHours = BigDecimal.ZERO;

    @Column(name = "ot_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal otHours = BigDecimal.ZERO;

    @Column(name = "normal_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal normalHours = BigDecimal.ZERO;

    @Column(name = "declared_ot_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal declaredOtHours = BigDecimal.ZERO;

    @Column(name = "undeclared_ot_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal undeclaredOtHours = BigDecimal.ZERO;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "cost_code_id")
    private UUID costCodeId;

    @Column(name = "remarks", length = 255)
    private String remarks;

    public UUID getTimesheetId() { return timesheetId; }
    public void setTimesheetId(UUID timesheetId) { this.timesheetId = timesheetId; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public UUID getTimeTypeId() { return timeTypeId; }
    public void setTimeTypeId(UUID timeTypeId) { this.timeTypeId = timeTypeId; }

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

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
