package com.hrms.rule.web;

import com.hrms.rule.dto.RulePackageDto;
import com.hrms.rule.service.RuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rule-packages")
public class RulePackageController {

    private final RuleService service;

    public RulePackageController(RuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<RulePackageDto> list() {
        return service.listPackages();
    }

    @GetMapping("/active")
    public Map<String, String> getActive() {
        return Map.of("packageCode", service.getActivePackageCode());
    }

    @PutMapping("/active")
    public Map<String, String> setActive(@RequestBody Map<String, String> body) {
        return Map.of("packageCode", service.setActivePackage(body.get("packageCode")));
    }
}
