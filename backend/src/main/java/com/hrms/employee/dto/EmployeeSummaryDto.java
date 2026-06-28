package com.hrms.employee.dto;

/**
 * Headcount summary for the Employees screen: total / active / not-active /
 * monthly / daily within the current filter scope (company + optional free-text
 * + optional project). Pay-status tab is NOT applied here.
 */
public class EmployeeSummaryDto {

    private long total;
    private long active;
    private long notActive;
    private long monthly;
    private long daily;

    public EmployeeSummaryDto() {
    }

    public EmployeeSummaryDto(long total, long active, long notActive, long monthly, long daily) {
        this.total = total;
        this.active = active;
        this.notActive = notActive;
        this.monthly = monthly;
        this.daily = daily;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getActive() {
        return active;
    }

    public void setActive(long active) {
        this.active = active;
    }

    public long getNotActive() {
        return notActive;
    }

    public void setNotActive(long notActive) {
        this.notActive = notActive;
    }

    public long getMonthly() {
        return monthly;
    }

    public void setMonthly(long monthly) {
        this.monthly = monthly;
    }

    public long getDaily() {
        return daily;
    }

    public void setDaily(long daily) {
        this.daily = daily;
    }
}
