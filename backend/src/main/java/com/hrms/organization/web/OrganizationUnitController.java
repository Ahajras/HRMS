package com.hrms.organization.web;

import com.hrms.organization.dto.OrgUnitTreeNode;
import com.hrms.organization.dto.OrganizationUnitDto;
import com.hrms.organization.service.OrganizationUnitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organization-units")
public class OrganizationUnitController {

    private final OrganizationUnitService service;

    public OrganizationUnitController(OrganizationUnitService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrganizationUnitDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/tree")
    public List<OrgUnitTreeNode> getTree() {
        return service.getTree();
    }

    @GetMapping("/{id}")
    public OrganizationUnitDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationUnitDto create(@Valid @RequestBody OrganizationUnitDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public OrganizationUnitDto update(@PathVariable UUID id, @Valid @RequestBody OrganizationUnitDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
