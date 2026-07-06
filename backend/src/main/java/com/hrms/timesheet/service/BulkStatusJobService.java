package com.hrms.timesheet.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.security.AuthenticatedUser;
import com.hrms.security.repository.AppUserRepository;
import com.hrms.timesheet.dto.BulkStatusJobDto;
import jakarta.annotation.PreDestroy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Generic background job runner for bulk status-change actions (submit-all,
 * approve-all, lock) that previously ran as one long synchronous request and
 * risked a gateway timeout at scale. Mirrors BulkTimesheetJobService's proven
 * pattern (background thread + progress polling + completion notification)
 * so it can be reused for any of these actions without duplicating that
 * machinery three times over.
 */
@Service
public class BulkStatusJobService {

    private final BulkTimesheetJobNotificationService notificationService;
    private final AppUserRepository appUserRepo;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<UUID, JobState> jobs = new ConcurrentHashMap<>();

    public BulkStatusJobService(BulkTimesheetJobNotificationService notificationService, AppUserRepository appUserRepo) {
        this.notificationService = notificationService;
        this.appUserRepo = appUserRepo;
    }

    public BulkStatusJobDto start(String operationLabel, UUID companyId,
                                  Function<TimesheetService.BulkStatusProgressListener, Map<String, Object>> work) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = userEmail(authentication);
        UUID id = UUID.randomUUID();
        JobState state = new JobState(id, operationLabel);
        jobs.put(id, state);
        executor.submit(() -> run(state, companyId, authentication, userEmail, work));
        return state.toDto();
    }

    public BulkStatusJobDto get(UUID id) {
        JobState state = jobs.get(id);
        if (state == null) {
            throw new ResourceNotFoundException("Bulk job not found: " + id);
        }
        return state.toDto();
    }

    private void run(JobState state, UUID companyId, Authentication authentication, String userEmail,
                     Function<TimesheetService.BulkStatusProgressListener, Map<String, Object>> work) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        TenantContext.setCompanyId(companyId);
        SecurityContextHolder.setContext(securityContext);
        try {
            Map<String, Object> result = work.apply((done, total) -> {
                state.done = done;
                state.total = total;
                state.message = state.operationLabel + "... processed " + done + " / " + total
                        + " in " + state.elapsedSeconds() + "s.";
            });
            state.result = result;
            state.status = "COMPLETED";
            state.message = state.operationLabel + " completed: " + state.done + " / " + state.total
                    + " in " + state.elapsedSeconds() + "s.";
        } catch (Exception ex) {
            state.status = "FAILED";
            state.message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        } finally {
            state.finishedAt = Instant.now();
            sendNotification(state, userEmail);
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String userEmail(Authentication authentication) {
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof AuthenticatedUser user && user.userId() != null) {
            return appUserRepo.findById(user.userId()).map(u -> u.getEmail()).orElse(null);
        }
        return null;
    }

    private void sendNotification(JobState state, String userEmail) {
        try {
            notificationService.sendCompletion(userEmail,
                    "HRMS " + state.operationLabel.toLowerCase() + " " + state.status.toLowerCase(),
                    state.operationLabel + " " + state.status.toLowerCase() + ".\n\n"
                            + "Processed: " + state.done + " / " + state.total + "\n"
                            + "Duration: " + state.elapsedSeconds() + " seconds\n"
                            + "Message: " + state.message + "\n"
                            + "Started: " + state.startedAt + "\n"
                            + "Finished: " + state.finishedAt + "\n");
        } catch (Exception ignored) {
            // Notification is best-effort and must never fail the completed job.
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private static final class JobState {
        private final UUID id;
        private final String operationLabel;
        private volatile String status = "RUNNING";
        private volatile int done = 0;
        private volatile int total = 0;
        private volatile String message;
        private volatile Map<String, Object> result = new HashMap<>();
        private final Instant startedAt = Instant.now();
        private volatile Instant finishedAt;

        private JobState(UUID id, String operationLabel) {
            this.id = id;
            this.operationLabel = operationLabel;
            this.message = operationLabel + "...";
        }

        private long elapsedSeconds() {
            Instant end = finishedAt != null ? finishedAt : Instant.now();
            return Duration.between(startedAt, end).toSeconds();
        }

        private BulkStatusJobDto toDto() {
            Long durationSeconds = finishedAt != null ? elapsedSeconds() : null;
            return new BulkStatusJobDto(id, status, done, total, elapsedSeconds(), durationSeconds, message,
                    startedAt, finishedAt, result);
        }
    }
}
