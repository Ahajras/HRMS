package com.hrms.rule.repository;

import com.hrms.rule.domain.RulePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RulePackageRepository extends JpaRepository<RulePackage, UUID> {

    List<RulePackage> findByCompanyIdIsNullOrderByName();

    Optional<RulePackage> findByCompanyIdIsNullAndCode(String code);
}
