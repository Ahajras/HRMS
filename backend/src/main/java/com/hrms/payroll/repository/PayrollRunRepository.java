package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {

    List<PayrollRun> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<PayrollRun> findByCompanyIdAndPeriodIdOrderByCreatedAtDesc(UUID companyId, UUID periodId);

    @Query("""
            select r from PayrollRun r
            where r.companyId = :companyId
              and r.periodId = :periodId
              and ((:projectId is null and r.projectId is null) or r.projectId = :projectId)
              and upper(r.payGroup) = upper(:payGroup)
            order by r.createdAt desc
            """)
    List<PayrollRun> findExistingScope(@Param("companyId") UUID companyId,
                                       @Param("periodId") UUID periodId,
                                       @Param("projectId") UUID projectId,
                                       @Param("payGroup") String payGroup);
}
