package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.PayrollPeriodProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollPeriodProjectRepository extends JpaRepository<PayrollPeriodProject, UUID> {

    List<PayrollPeriodProject> findByPeriodId(UUID periodId);

    Optional<PayrollPeriodProject> findByCompanyIdAndPeriodIdAndProjectId(UUID companyId, UUID periodId, UUID projectId);

    Optional<PayrollPeriodProject> findByCompanyIdAndPeriodIdAndProjectIdAndPayGroup(
            UUID companyId, UUID periodId, UUID projectId, String payGroup);
}
