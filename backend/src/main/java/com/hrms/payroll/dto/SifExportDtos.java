package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SifExportDtos {

    /** One sponsor's worth of a SIF export — the header values plus every
     * detail row, ready to render as CSV. */
    public static class SifFile {
        private String sponsorCode;
        private String sponsorName;
        private String fileName;
        private String establishmentEid;
        private String payerBankCode;
        private String payerIban;
        private String payerQid;
        private int periodYear;
        private int periodMonth;
        private BigDecimal totalSalaries = BigDecimal.ZERO;
        private int totalRecords;
        private List<SifRow> rows = new ArrayList<>();

        public String getSponsorCode() { return sponsorCode; }
        public void setSponsorCode(String sponsorCode) { this.sponsorCode = sponsorCode; }
        public String getSponsorName() { return sponsorName; }
        public void setSponsorName(String sponsorName) { this.sponsorName = sponsorName; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getEstablishmentEid() { return establishmentEid; }
        public void setEstablishmentEid(String establishmentEid) { this.establishmentEid = establishmentEid; }
        public String getPayerBankCode() { return payerBankCode; }
        public void setPayerBankCode(String payerBankCode) { this.payerBankCode = payerBankCode; }
        public String getPayerIban() { return payerIban; }
        public void setPayerIban(String payerIban) { this.payerIban = payerIban; }
        public String getPayerQid() { return payerQid; }
        public void setPayerQid(String payerQid) { this.payerQid = payerQid; }
        public int getPeriodYear() { return periodYear; }
        public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
        public int getPeriodMonth() { return periodMonth; }
        public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
        public BigDecimal getTotalSalaries() { return totalSalaries; }
        public void setTotalSalaries(BigDecimal totalSalaries) { this.totalSalaries = totalSalaries; }
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
        public List<SifRow> getRows() { return rows; }
        public void setRows(List<SifRow> rows) { this.rows = rows; }
    }

    /** One employee's SIF detail row — editable (notes) before final export. */
    public static class SifRow {
        private int recordSequence;
        private String employeeId;
        private String employeeNumber;
        private String qid;
        private String visaId;
        private String employeeName;
        private String bankCode;
        private String bankAccount;
        private String salaryFrequency = "M";
        private int workingDays;
        private long netSalary;
        private long basicSalary;
        private BigDecimal extraHours = BigDecimal.ZERO;
        private long extraIncome;
        private long deductions;
        private String notes;
        private String projectCode;

        public int getRecordSequence() { return recordSequence; }
        public void setRecordSequence(int recordSequence) { this.recordSequence = recordSequence; }
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public String getEmployeeNumber() { return employeeNumber; }
        public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
        public String getQid() { return qid; }
        public void setQid(String qid) { this.qid = qid; }
        public String getVisaId() { return visaId; }
        public void setVisaId(String visaId) { this.visaId = visaId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getBankCode() { return bankCode; }
        public void setBankCode(String bankCode) { this.bankCode = bankCode; }
        public String getBankAccount() { return bankAccount; }
        public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
        public String getSalaryFrequency() { return salaryFrequency; }
        public void setSalaryFrequency(String salaryFrequency) { this.salaryFrequency = salaryFrequency; }
        public int getWorkingDays() { return workingDays; }
        public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }
        public long getNetSalary() { return netSalary; }
        public void setNetSalary(long netSalary) { this.netSalary = netSalary; }
        public long getBasicSalary() { return basicSalary; }
        public void setBasicSalary(long basicSalary) { this.basicSalary = basicSalary; }
        public BigDecimal getExtraHours() { return extraHours; }
        public void setExtraHours(BigDecimal extraHours) { this.extraHours = extraHours; }
        public long getExtraIncome() { return extraIncome; }
        public void setExtraIncome(long extraIncome) { this.extraIncome = extraIncome; }
        public long getDeductions() { return deductions; }
        public void setDeductions(long deductions) { this.deductions = deductions; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    }

    /** An employee that should be in the SIF but is missing required data
     * (bank account or QID/Visa) — surfaced so it can be fixed, not
     * silently dropped. */
    public static class SifExclusion {
        private String employeeNumber;
        private String employeeName;
        private String reason;

        public SifExclusion() { }
        public SifExclusion(String employeeNumber, String employeeName, String reason) {
            this.employeeNumber = employeeNumber;
            this.employeeName = employeeName;
            this.reason = reason;
        }
        public String getEmployeeNumber() { return employeeNumber; }
        public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class SifExportResult {
        private List<SifFile> files = new ArrayList<>();
        private List<SifExclusion> exclusions = new ArrayList<>();

        public List<SifFile> getFiles() { return files; }
        public void setFiles(List<SifFile> files) { this.files = files; }
        public List<SifExclusion> getExclusions() { return exclusions; }
        public void setExclusions(List<SifExclusion> exclusions) { this.exclusions = exclusions; }
    }
}
