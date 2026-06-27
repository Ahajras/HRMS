package com.hrms.rule.repository;

import com.hrms.rule.domain.CompanyRulePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRulePackageRepository extends JpaRepository<CompanyRulePackage, UUID> {
}
