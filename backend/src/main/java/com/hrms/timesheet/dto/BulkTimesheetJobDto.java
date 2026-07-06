package com.hrms.timesheet.dto;

import java.time.Instant;
import java.util.UUID;

public record BulkTimesheetJobDto(
        UUID id,
        String status,
        int created,
        int skipped,
        String message,
        Instant startedAt,
        Instant finishedAt
) {
}
