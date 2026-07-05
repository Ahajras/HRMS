package com.hrms.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeTimeTypeUsageDto {
    private int year;
    private List<Row> rows = new ArrayList<>();

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }

    public static class Row {
        private String timeTypeCode;
        private String timeTypeName;
        private String category;
        private BigDecimal usedDays = BigDecimal.ZERO;
        private BigDecimal usedHours = BigDecimal.ZERO;
        private int occurrences;
        private LocalDate firstDate;
        private LocalDate lastDate;
        private int thresholdDays;
        private String thresholdScope;

        public String getTimeTypeCode() { return timeTypeCode; }
        public void setTimeTypeCode(String timeTypeCode) { this.timeTypeCode = timeTypeCode; }
        public String getTimeTypeName() { return timeTypeName; }
        public void setTimeTypeName(String timeTypeName) { this.timeTypeName = timeTypeName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getUsedDays() { return usedDays; }
        public void setUsedDays(BigDecimal usedDays) { this.usedDays = usedDays; }
        public BigDecimal getUsedHours() { return usedHours; }
        public void setUsedHours(BigDecimal usedHours) { this.usedHours = usedHours; }
        public int getOccurrences() { return occurrences; }
        public void setOccurrences(int occurrences) { this.occurrences = occurrences; }
        public LocalDate getFirstDate() { return firstDate; }
        public void setFirstDate(LocalDate firstDate) { this.firstDate = firstDate; }
        public LocalDate getLastDate() { return lastDate; }
        public void setLastDate(LocalDate lastDate) { this.lastDate = lastDate; }
        public int getThresholdDays() { return thresholdDays; }
        public void setThresholdDays(int thresholdDays) { this.thresholdDays = thresholdDays; }
        public String getThresholdScope() { return thresholdScope; }
        public void setThresholdScope(String thresholdScope) { this.thresholdScope = thresholdScope; }
    }
}
