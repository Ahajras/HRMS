package com.hrms.timesheet.web;

import com.hrms.common.tenant.TenantContext;
import com.hrms.common.web.PageResponse;
import com.hrms.timesheet.dto.GenerateTimesheetRequest;
import com.hrms.timesheet.dto.BulkStatusJobDto;
import com.hrms.timesheet.dto.BulkTimesheetJobDto;
import com.hrms.timesheet.dto.TimesheetDayDto;
import com.hrms.timesheet.dto.TimesheetDto;
import com.hrms.timesheet.dto.TimesheetProjectSummaryDto;
import com.hrms.timesheet.dto.TimesheetSummaryDto;
import com.hrms.timesheet.service.BulkStatusJobService;
import com.hrms.timesheet.service.BulkTimesheetJobService;
import com.hrms.timesheet.service.TimesheetService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** REST API for monthly timesheets (FTDD Vol.1 Ch.3). */
@RestController
@RequestMapping("/api/v1/timesheets")
public class TimesheetController {

    private final TimesheetService service;
    private final BulkTimesheetJobService bulkJobService;
    private final BulkStatusJobService bulkStatusJobService;

    public TimesheetController(TimesheetService service, BulkTimesheetJobService bulkJobService,
                               BulkStatusJobService bulkStatusJobService) {
        this.service = service;
        this.bulkJobService = bulkJobService;
        this.bulkStatusJobService = bulkStatusJobService;
    }

    @GetMapping
    public PageResponse<TimesheetDto> listByPeriod(@RequestParam int year,
                                                   @RequestParam int month,
                                                   @RequestParam(required = false) UUID projectId,
                                                   @RequestParam(required = false) String q,
                                                   @PageableDefault(size = 50) Pageable pageable) {
        return service.listByPeriod(year, month, projectId, q, pageable);
    }

    @GetMapping("/{id}")
    public TimesheetDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/summary")
    public TimesheetSummaryDto summary(@PathVariable UUID id) {
        return service.summarize(id);
    }

    @GetMapping("/project-summary")
    public List<TimesheetProjectSummaryDto> projectSummary(@RequestParam int year,
                                                           @RequestParam int month,
                                                           @RequestParam(required = false) UUID projectId) {
        return service.projectSummary(year, month, projectId);
    }

    @GetMapping("/eligible-employees")
    public List<Map<String, Object>> eligibleEmployees(@RequestParam UUID periodId) {
        return service.eligibleEmployees(periodId);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public TimesheetDto generate(@Valid @RequestBody GenerateTimesheetRequest req) {
        return service.generate(req);
    }

    @PostMapping("/generate-bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> generateBulk(@RequestParam UUID periodId,
                                             @RequestParam(required = false) UUID projectId) {
        return service.generateBulk(periodId, projectId);
    }

    @PostMapping("/generate-bulk-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BulkTimesheetJobDto startGenerateBulk(@RequestParam UUID periodId,
                                                 @RequestParam(required = false) UUID projectId) {
        return bulkJobService.start(periodId, projectId);
    }

    @GetMapping("/generate-bulk-jobs/{id}")
    public BulkTimesheetJobDto getGenerateBulkJob(@PathVariable UUID id) {
        return bulkJobService.get(id);
    }

    @PostMapping("/generate-by-crew")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> generateByCrew(@RequestParam UUID crewId, @RequestParam UUID periodId) {
        return service.generateByCrew(crewId, periodId);
    }

    @PostMapping("/submit-all")
    public Map<String, Integer> submitAll(@RequestParam int year, @RequestParam int month,
                                          @RequestParam(required = false) UUID projectId) {
        return service.submitAll(year, month, projectId);
    }

    @PostMapping("/submit-all-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BulkStatusJobDto startSubmitAll(@RequestParam int year, @RequestParam int month,
                                           @RequestParam(required = false) UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        return bulkStatusJobService.start("Submitting timesheets", companyId,
                progress -> new HashMap<>(service.submitAll(year, month, projectId, progress)));
    }

    @GetMapping("/submit-all-jobs/{id}")
    public BulkStatusJobDto getSubmitAllJob(@PathVariable UUID id) {
        return bulkStatusJobService.get(id);
    }

    @PostMapping("/approve-all")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('MANAGER') or hasRole('COMPANY_ADMIN') or hasRole('SUPER_ADMIN')")
    public Map<String, Integer> approveAll(@RequestParam int year, @RequestParam int month,
                                           @RequestParam(required = false) UUID projectId) {
        return service.approveAll(year, month, projectId);
    }

    @PostMapping("/approve-all-jobs")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('MANAGER') or hasRole('COMPANY_ADMIN') or hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BulkStatusJobDto startApproveAll(@RequestParam int year, @RequestParam int month,
                                            @RequestParam(required = false) UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        return bulkStatusJobService.start("Approving timesheets", companyId,
                progress -> new HashMap<>(service.approveAll(year, month, projectId, progress)));
    }

    @GetMapping("/approve-all-jobs/{id}")
    public BulkStatusJobDto getApproveAllJob(@PathVariable UUID id) {
        return bulkStatusJobService.get(id);
    }

    @PutMapping("/{id}/days")
    public TimesheetDto saveDays(@PathVariable UUID id, @RequestBody List<TimesheetDayDto> days) {
        return service.saveDays(id, days);
    }

    @PostMapping("/{id}/submit")
    public TimesheetDto submit(@PathVariable UUID id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/approve")
    public TimesheetDto approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/lock")
    public TimesheetDto lock(@PathVariable UUID id) {
        return service.lock(id);
    }

    @PostMapping("/{id}/reopen")
    public TimesheetDto reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
