package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetRepository extends JpaRepository<Timesheet, UUID> {

    Optional<Timesheet> findByCompanyIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
            UUID companyId, UUID employeeId, int periodYear, int periodMonth);

    List<Timesheet> findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(
            UUID companyId, int periodYear, int periodMonth);

    @Query(value = """
            select t from Timesheet t
            join Employee e on e.id = t.employeeId
            join Assignment a on a.employeeId = e.id
            where t.companyId = :companyId
              and t.periodYear = :year
              and t.periodMonth = :month
              and a.projectId = :projectId
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            order by e.employeeNumber
            """,
            countQuery = """
            select count(t.id) from Timesheet t
            join Employee e on e.id = t.employeeId
            join Assignment a on a.employeeId = e.id
            where t.companyId = :companyId
              and t.periodYear = :year
              and t.periodMonth = :month
              and a.projectId = :projectId
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            """)
    Page<Timesheet> searchByProject(@Param("companyId") UUID companyId,
                                    @Param("year") int year,
                                    @Param("month") int month,
                                    @Param("projectId") UUID projectId,
                                    @Param("q") String q,
                                    Pageable pageable);

    @Query(value = """
            select t from Timesheet t
            join Employee e on e.id = t.employeeId
            join Assignment a on a.employeeId = e.id
            where t.companyId = :companyId
              and t.periodYear = :year
              and t.periodMonth = :month
              and a.projectId in :projectIds
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            order by e.employeeNumber
            """,
            countQuery = """
            select count(t.id) from Timesheet t
            join Employee e on e.id = t.employeeId
            join Assignment a on a.employeeId = e.id
            where t.companyId = :companyId
              and t.periodYear = :year
              and t.periodMonth = :month
              and a.projectId in :projectIds
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            """)
    Page<Timesheet> searchByProjects(@Param("companyId") UUID companyId,
                                     @Param("year") int year,
                                     @Param("month") int month,
                                     @Param("projectIds") Collection<UUID> projectIds,
                                     @Param("q") String q,
                                     Pageable pageable);

    List<Timesheet> findByCompanyIdAndEmployeeIdAndPeriodYearOrderByPeriodMonth(
            UUID companyId, UUID employeeId, int periodYear);

    List<Timesheet> findByPeriodId(UUID periodId);
}
