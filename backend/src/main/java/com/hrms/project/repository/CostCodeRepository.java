package com.hrms.project.repository;

import com.hrms.project.domain.CostCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CostCodeRepository extends JpaRepository<CostCode, UUID> {

    List<CostCode> findByProjectIdOrderByCode(UUID projectId);

    List<CostCode> findByCompanyIdOrderByCode(UUID companyId);
}
