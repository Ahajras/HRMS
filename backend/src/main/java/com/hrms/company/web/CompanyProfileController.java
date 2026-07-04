package com.hrms.company.web;

import com.hrms.company.dto.CompanyProfileDto;
import com.hrms.company.service.CompanyProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/company-profile")
public class CompanyProfileController {

    private final CompanyProfileService service;

    public CompanyProfileController(CompanyProfileService service) {
        this.service = service;
    }

    @GetMapping
    public CompanyProfileDto get() {
        return service.get();
    }

    @PutMapping
    public CompanyProfileDto save(@RequestBody CompanyProfileDto dto) {
        return service.save(dto);
    }
}
