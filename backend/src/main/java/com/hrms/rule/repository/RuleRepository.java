package com.hrms.rule.repository;

import com.hrms.rule.domain.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RuleRepository extends JpaRepository<Rule, UUID> {

    List<Rule> findByPackageIdOrderByCategoryAscNameAscEffectiveFromDesc(UUID packageId);

    List<Rule> findByPackageIdAndCodeAndStatus(UUID packageId, String code, String status);
}
