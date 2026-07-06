package com.hrms.timesheet.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Generic progress DTO for background bulk status-change jobs (submit-all, approve-all, lock). */
public record BulkStatusJobDto(
        UUID id,
        String status,
        int done,
        int total,
        long elapsedSeconds,
        Long durationSeconds,
        String message,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> result
) {
}
