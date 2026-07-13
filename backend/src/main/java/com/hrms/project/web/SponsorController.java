package com.hrms.project.web;

import com.hrms.project.dto.SponsorDto;
import com.hrms.project.service.SponsorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sponsors")
public class SponsorController {

    private final SponsorService service;

    public SponsorController(SponsorService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organization.read')")
    public List<SponsorDto> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organization.write')")
    public SponsorDto save(@RequestBody SponsorDto dto) {
        return service.save(dto);
    }
}
