package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PayrollCostControlReportDto {
    private UUID periodId;
    private String periodName;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private UUID projectId;
    private BigDecimal debitTotal = BigDecimal.ZERO;
    private BigDecimal creditTotal = BigDecimal.ZERO;
    private BigDecimal difference = BigDecimal.ZERO;
    private List<Line> debitLines = new ArrayList<>();
    private List<Line> creditLines = new ArrayList<>();

    public static class Line {
        private String side;
        private String accountCode;
        private String description;
        private UUID projectId;
        private String projectCode;
        private String projectName;
        private String source;
        private BigDecimal amount = BigDecimal.ZERO;

        public Line() { }

        public Line(String side, String accountCode, String description, UUID projectId,
                    String projectCode, String projectName, String source, BigDecimal amount) {
            this.side = side;
            this.accountCode = accountCode;
            this.description = description;
            this.projectId = projectId;
            this.projectCode = projectCode;
            this.projectName = projectName;
            this.source = source;
            this.amount = amount;
        }

        public String getSide() { return side; }
        public void setSide(String side) { this.side = side; }
        public String getAccountCode() { return accountCode; }
        public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public UUID getProjectId() { return projectId; }
        public void setProjectId(UUID projectId) { this.projectId = projectId; }
        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public UUID getPeriodId() { return periodId; }
    public void setPeriodId(UUID periodId) { this.periodId = periodId; }
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    public LocalDate getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(LocalDate periodStartDate) { this.periodStartDate = periodStartDate; }
    public LocalDate getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(LocalDate periodEndDate) { this.periodEndDate = periodEndDate; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public BigDecimal getDebitTotal() { return debitTotal; }
    public void setDebitTotal(BigDecimal debitTotal) { this.debitTotal = debitTotal; }
    public BigDecimal getCreditTotal() { return creditTotal; }
    public void setCreditTotal(BigDecimal creditTotal) { this.creditTotal = creditTotal; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }
    public List<Line> getDebitLines() { return debitLines; }
    public void setDebitLines(List<Line> debitLines) { this.debitLines = debitLines; }
    public List<Line> getCreditLines() { return creditLines; }
    public void setCreditLines(List<Line> creditLines) { this.creditLines = creditLines; }
}
