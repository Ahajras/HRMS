package com.hrms.reference.repository;

import com.hrms.reference.domain.OvertimeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OvertimeCategoryRepository extends JpaRepository<OvertimeCategory, UUID> {

    List<OvertimeCategory> findByCompanyIdOrderByCode(UUID companyId);

    Optional<OvertimeCategory> findByCompanyIdAndCode(UUID companyId, String code);
}
