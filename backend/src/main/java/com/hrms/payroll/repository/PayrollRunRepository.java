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

    /** Every run for this period/project, any pay group — used to guard
     * against creating an overlapping scope (e.g. an "ALL" run alongside a
     * "MONTHLY only" run for the same period/project), which would let the
     * same employees be paid through two different runs. */
    @Query("""
            select r from PayrollRun r
            where r.companyId = :companyId
              and r.periodId = :periodId
              and ((:projectId is null and r.projectId is null) or r.projectId = :projectId)
            """)
    List<PayrollRun> findAllForScope(@Param("companyId") UUID companyId,
                                     @Param("periodId") UUID periodId,
                                     @Param("projectId") UUID projectId);

    @Query("""
            select count(r) > 0 from PayrollRun r
            where r.companyId = :companyId
              and r.periodId = :periodId
              and (r.projectId is null or r.projectId = :projectId)
              and (upper(r.payGroup) = 'ALL' or upper(:payGroup) = 'ALL' or upper(r.payGroup) = upper(:payGroup))
              and upper(r.status) in ('APPROVED', 'LOCKED')
            """)
    boolean existsApprovedOrLockedForScope(@Param("companyId") UUID companyId,
                                           @Param("periodId") UUID periodId,
                                           @Param("projectId") UUID projectId,
                                           @Param("payGroup") String payGroup);

    @Query("""
            select count(r) > 0 from PayrollRun r
            where r.companyId = :companyId
              and r.periodId = :periodId
              and (r.projectId is null or r.projectId = :projectId)
              and (upper(r.payGroup) = 'ALL' or upper(:payGroup) = 'ALL' or upper(r.payGroup) = upper(:payGroup))
              and upper(r.status) in ('CALCULATED', 'APPROVED', 'LOCKED')
            """)
    boolean existsProcessedForScope(@Param("companyId") UUID companyId,
                                    @Param("periodId") UUID periodId,
                                    @Param("projectId") UUID projectId,
                                    @Param("payGroup") String payGroup);

    @Query("""
            select count(r) > 0 from PayrollRun r
            where r.companyId = :companyId
              and r.periodId = :periodId
              and upper(r.status) in ('APPROVED', 'LOCKED')
            """)
    boolean existsApprovedOrLockedForPeriod(@Param("companyId") UUID companyId,
                                            @Param("periodId") UUID periodId);

    @Query("""
            select count(r) > 0 from PayrollRun r
            where r.companyId = :companyId
              and r.periodId = :periodId
              and upper(r.status) in ('CALCULATED', 'APPROVED', 'LOCKED')
            """)
    boolean existsProcessedForPeriod(@Param("companyId") UUID companyId,
                                     @Param("periodId") UUID periodId);
}
