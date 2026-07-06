package com.hrms.timesheet.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.security.AuthenticatedUser;
import com.hrms.security.repository.AppUserRepository;
import com.hrms.timesheet.dto.BulkTimesheetJobDto;
import jakarta.annotation.PreDestroy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class BulkTimesheetJobService {

    private final TimesheetService timesheetService;
    private final BulkTimesheetJobNotificationService notificationService;
    private final AppUserRepository appUserRepo;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<UUID, JobState> jobs = new ConcurrentHashMap<>();

    public BulkTimesheetJobService(TimesheetService timesheetService,
                                   BulkTimesheetJobNotificationService notificationService,
                                   AppUserRepository appUserRepo) {
        this.timesheetService = timesheetService;
        this.notificationService = notificationService;
        this.appUserRepo = appUserRepo;
    }

    public BulkTimesheetJobDto start(UUID periodId, UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = userEmail(authentication);
        UUID id = UUID.randomUUID();
        JobState state = new JobState(id);
        jobs.put(id, state);
        executor.submit(() -> run(state, companyId, authentication, userEmail, periodId, projectId));
        return state.toDto();
    }

    public BulkTimesheetJobDto get(UUID id) {
        JobState state = jobs.get(id);
        if (state == null) {
            throw new ResourceNotFoundException("Bulk timesheet job not found: " + id);
        }
        return state.toDto();
    }

    private void run(JobState state, UUID companyId, Authentication authentication, String userEmail,
                     UUID periodId, UUID projectId) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        TenantContext.setCompanyId(companyId);
        SecurityContextHolder.setContext(securityContext);
        try {
            Map<String, Integer> result = timesheetService.generateBulk(periodId, projectId, new TimesheetService.BulkProgressListener() {
                @Override
                public void onStart(int total) {
                    state.total = total;
                    state.message = "Generating timesheets... processed 0 / " + total
                            + " in " + state.elapsedSeconds() + "s.";
                }

                @Override
                public void onProgress(int created, int skipped, int total) {
                    state.created = created;
                    state.skipped = skipped;
                    state.total = total;
                    state.message = "Generating timesheets... processed " + state.processed()
                            + " / " + total + " in " + state.elapsedSeconds() + "s.";
                }
            });
            state.created = result.getOrDefault("created", 0);
            state.skipped = result.getOrDefault("skipped", 0);
            state.status = "COMPLETED";
            state.message = "Generated " + state.created + ", skipped " + state.skipped
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
                    "HRMS bulk timesheet generation " + state.status.toLowerCase(),
                    "Bulk timesheet generation " + state.status.toLowerCase() + ".\n\n"
                            + "Created: " + state.created + "\n"
                            + "Skipped: " + state.skipped + "\n"
                            + "Processed: " + state.processed() + " / " + state.total + "\n"
                            + "Duration: " + state.elapsedSeconds() + " seconds\n"
                            + "Message: " + state.message + "\n"
                            + "Started: " + state.startedAt + "\n"
                            + "Finished: " + state.finishedAt + "\n");
        } catch (Exception ignored) {
            // Notification is temporary and must never fail the completed job.
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
        private volatile int total = 0;
        private volatile String message = "Generating timesheets...";
        private final Instant startedAt = Instant.now();
        private volatile Instant finishedAt;

        private JobState(UUID id) {
            this.id = id;
        }

        private int processed() {
            return created + skipped;
        }

        private long elapsedSeconds() {
            Instant end = finishedAt != null ? finishedAt : Instant.now();
            return Duration.between(startedAt, end).toSeconds();
        }

        private BulkTimesheetJobDto toDto() {
            Long durationSeconds = finishedAt != null ? elapsedSeconds() : null;
            return new BulkTimesheetJobDto(id, status, created, skipped, processed(), total,
                    elapsedSeconds(), durationSeconds, message, startedAt, finishedAt);
        }
    }
}
