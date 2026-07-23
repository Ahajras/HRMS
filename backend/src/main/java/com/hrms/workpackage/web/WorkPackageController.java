package com.hrms.workpackage.web;

import com.hrms.workpackage.dto.WorkPackageCrewDto;
import com.hrms.workpackage.dto.WorkPackageDto;
import com.hrms.workpackage.dto.WorkPackageRequirementDto;
import com.hrms.workpackage.service.WorkPackageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-packages")
public class WorkPackageController {

    private final WorkPackageService service;

    public WorkPackageController(WorkPackageService service) {
        this.service = service;
    }

    @GetMapping
    public List<WorkPackageDto> list(@RequestParam(required = false) UUID projectId) {
        return service.list(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkPackageDto create(@RequestBody WorkPackageDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public WorkPackageDto update(@PathVariable UUID id, @RequestBody WorkPackageDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/requirements")
    public List<WorkPackageRequirementDto> requirements(@PathVariable UUID id) {
        return service.requirements(id);
    }

    @PostMapping("/{id}/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkPackageRequirementDto addRequirement(@PathVariable UUID id, @RequestBody WorkPackageRequirementDto dto) {
        return service.addRequirement(id, dto);
    }

    @DeleteMapping("/requirements/{requirementId}")
    public ResponseEntity<Void> removeRequirement(@PathVariable UUID requirementId) {
        service.removeRequirement(requirementId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/crews")
    public List<WorkPackageCrewDto> crews(@PathVariable UUID id) {
        return service.crews(id);
    }

    @PostMapping("/{id}/crews")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkPackageCrewDto addCrew(@PathVariable UUID id, @RequestBody WorkPackageCrewDto dto) {
        return service.addCrew(id, dto);
    }

    @DeleteMapping("/crews/{crewLinkId}")
    public ResponseEntity<Void> removeCrew(@PathVariable UUID crewLinkId) {
        service.removeCrew(crewLinkId);
        return ResponseEntity.noContent().build();
    }
}
