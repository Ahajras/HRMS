package com.hrms.payroll.repository;

import com.hrms.payroll.domain.ProvisionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProvisionRunRepository extends JpaRepository<ProvisionRun, UUID> {

    List<ProvisionRun> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<ProvisionRun> findByCompanyIdAndPeriodIdOrderByCreatedAtDesc(UUID companyId, UUID periodId);
}
