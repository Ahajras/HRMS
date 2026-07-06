package com.hrms.timesheet.dto;

import java.time.Instant;
import java.util.UUID;

public record BulkTimesheetJobDto(
        UUID id,
        String status,
        int created,
        int skipped,
        int processed,
        int total,
        long elapsedSeconds,
        Long durationSeconds,
        String message,
        Instant startedAt,
        Instant finishedAt
) {
}
