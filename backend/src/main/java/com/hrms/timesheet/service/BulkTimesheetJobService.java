package com.hrms.timesheet.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.dto.BulkTimesheetJobDto;
import jakarta.annotation.PreDestroy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class BulkTimesheetJobService {

    private final TimesheetService timesheetService;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<UUID, JobState> jobs = new ConcurrentHashMap<>();

    public BulkTimesheetJobService(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    public BulkTimesheetJobDto start(UUID periodId, UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID id = UUID.randomUUID();
        JobState state = new JobState(id);
        jobs.put(id, state);
        executor.submit(() -> run(state, companyId, authentication, periodId, projectId));
        return state.toDto();
    }

    public BulkTimesheetJobDto get(UUID id) {
        JobState state = jobs.get(id);
        if (state == null) {
            throw new ResourceNotFoundException("Bulk timesheet job not found: " + id);
        }
        return state.toDto();
    }

    private void run(JobState state, UUID companyId, Authentication authentication, UUID periodId, UUID projectId) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        TenantContext.setCompanyId(companyId);
        SecurityContextHolder.setContext(securityContext);
        try {
            Map<String, Integer> result = timesheetService.generateBulk(periodId, projectId, (created, skipped) -> {
                state.created = created;
                state.skipped = skipped;
                state.message = "Generating timesheets... created " + created + ", skipped " + skipped + ".";
            });
            state.created = result.getOrDefault("created", 0);
            state.skipped = result.getOrDefault("skipped", 0);
            state.status = "COMPLETED";
            state.message = "Generated " + state.created + ", skipped " + state.skipped + ".";
        } catch (Exception ex) {
            state.status = "FAILED";
            state.message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        } finally {
            state.finishedAt = Instant.now();
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private static final class JobState {
        private final UUID id;
        private volatile String status = "RUNNING";
        private volatile int created = 0;
        private volatile int skipped = 0;
        private volatile String message = "Generating timesheets...";
        private final Instant startedAt = Instant.now();
        private volatile Instant finishedAt;

        private JobState(UUID id) {
            this.id = id;
        }

        private BulkTimesheetJobDto toDto() {
            return new BulkTimesheetJobDto(id, status, created, skipped, message, startedAt, finishedAt);
        }
    }
}
