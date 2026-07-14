package com.hrms.project.web;

import com.hrms.project.dto.ProjectApprovalRoleDto;
import com.hrms.project.dto.RoleEmployeeCandidateDto;
import com.hrms.project.service.ProjectApprovalRoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project-approval-roles")
public class ProjectApprovalRoleController {

    private final ProjectApprovalRoleService service;

    public ProjectApprovalRoleController(ProjectApprovalRoleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('employee.read')")
    public List<ProjectApprovalRoleDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/candidates")
    @PreAuthorize("hasAuthority('employee.read')")
    public List<RoleEmployeeCandidateDto> candidates(@RequestParam String roleCode) {
        return service.candidates(roleCode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('employee.write')")
    public ProjectApprovalRoleDto create(@Valid @RequestBody ProjectApprovalRoleDto dto) {
        return service.create(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
