package com.hrms.migration.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of a legacy import run (preview or committed). Returned to the admin
 * "Import from legacy system" screen so the user sees exactly what was / would be
 * inserted vs. updated, plus any warnings (e.g. unmapped nationality codes).
 */
public class ImportSummary {

    /** false = preview only (no DB writes); true = changes were committed. */
    private boolean committed;

    /** Rows read from each source DBF. */
    private int sourceHeaderRows;
    private int sourceDetailRows;
    private int sourceDependentRows;

    /** Ordered insert/update tallies, e.g. {"employee_inserted": 25, ...}. */
    private final Map<String, Integer> counts = new LinkedHashMap<>();

    /** Human-readable warnings (skipped rows, unmapped codes, ...). */
    private final List<String> warnings = new ArrayList<>();

    /** A small preview of the first employees (number, name, nationality, pay, action). */
    private final List<Map<String, Object>> sample = new ArrayList<>();

    public void bump(String key) {
        counts.merge(key, 1, Integer::sum);
    }

    public boolean isCommitted() { return committed; }
    public void setCommitted(boolean committed) { this.committed = committed; }

    public int getSourceHeaderRows() { return sourceHeaderRows; }
    public void setSourceHeaderRows(int sourceHeaderRows) { this.sourceHeaderRows = sourceHeaderRows; }

    public int getSourceDetailRows() { return sourceDetailRows; }
    public void setSourceDetailRows(int sourceDetailRows) { this.sourceDetailRows = sourceDetailRows; }

    public int getSourceDependentRows() { return sourceDependentRows; }
    public void setSourceDependentRows(int sourceDependentRows) { this.sourceDependentRows = sourceDependentRows; }

    public Map<String, Integer> getCounts() { return counts; }

    public List<String> getWarnings() { return warnings; }

    public List<Map<String, Object>> getSample() { return sample; }
}
