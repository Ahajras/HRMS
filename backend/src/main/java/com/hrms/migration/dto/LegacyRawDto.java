package com.hrms.migration.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Parsed legacy snapshot for one employee: the full header row and every detail
 * (pay) line, with all columns preserved (blanks kept). Powers the admin
 * "Legacy Data" tab.
 */
public class LegacyRawDto {

    private String employeeNumber;
    private String source;
    private Instant importedAt;
    private Map<String, String> header;
    private List<Map<String, String>> detail;

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getImportedAt() { return importedAt; }
    public void setImportedAt(Instant importedAt) { this.importedAt = importedAt; }

    public Map<String, String> getHeader() { return header; }
    public void setHeader(Map<String, String> header) { this.header = header; }

    public List<Map<String, String>> getDetail() { return detail; }
    public void setDetail(List<Map<String, String>> detail) { this.detail = detail; }
}
