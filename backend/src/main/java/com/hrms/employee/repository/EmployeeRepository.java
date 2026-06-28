package com.hrms.employee.repository;

import com.hrms.employee.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Page<Employee> findByCompanyId(UUID companyId, Pageable pageable);

    Optional<Employee> findByCompanyIdAndEmployeeNumber(UUID companyId, String employeeNumber);

    boolean existsByCompanyIdAndEmployeeNumber(UUID companyId, String employeeNumber);

    /**
     * Free-text search across employee identity fields and the action sheet
     * number of any of the employee's contract pay items.
     */
    @Query("""
            select distinct e from Employee e
            where e.companyId = :companyId and (
                  lower(e.employeeNumber) like lower(concat('%', :q, '%'))
               or lower(e.firstName) like lower(concat('%', :q, '%'))
               or lower(e.lastName) like lower(concat('%', :q, '%'))
               or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(e.jobTitle, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(e.email, '')) like lower(concat('%', :q, '%'))
               or exists (
                    select 1 from ContractPayItem pi
                    where pi.employeeId = e.id and (
                          lower(coalesce(pi.actionSheetNo, '')) like lower(concat('%', :q, '%'))
                       or lower(coalesce(pi.remarks, '')) like lower(concat('%', :q, '%'))
                    )
               )
            )
            """)
    Page<Employee> search(@Param("companyId") UUID companyId, @Param("q") String q, Pageable pageable);

    /**
     * Unified list with optional free-text ({@code q}), optional pay-status
     * keyword (e.g. DAILY, MONTHLY — matched as a substring of pay_status, which
     * may hold legacy values like "DAILY PAID"), and optional project filter
     * (employees who have an assignment on that project). Null/blank params are
     * ignored.
     */
    @Query("""
            select distinct e from Employee e
            where e.companyId = :companyId
              and (:payStatus is null or :payStatus = ''
                   or lower(coalesce(e.payStatus, '')) like lower(concat('%', :payStatus, '%')))
              and (:projectId is null or exists (
                    select 1 from Assignment a
                    where a.employeeId = e.id and a.projectId = :projectId))
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.jobTitle, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.email, '')) like lower(concat('%', :q, '%'))
                 or exists (
                      select 1 from ContractPayItem pi
                      where pi.employeeId = e.id and (
                            lower(coalesce(pi.actionSheetNo, '')) like lower(concat('%', :q, '%'))
                         or lower(coalesce(pi.remarks, '')) like lower(concat('%', :q, '%'))
                      )
                 )
              ))
            """)
    Page<Employee> searchFiltered(@Param("companyId") UUID companyId, @Param("q") String q,
                                  @Param("payStatus") String payStatus,
                                  @Param("projectId") UUID projectId, Pageable pageable);

    /**
     * Headcount summary for the same filter scope (company + optional free-text +
     * optional project). Returns one row:
     * [0]=total, [1]=active, [2]=monthly, [3]=daily. Pay-status tab is NOT applied
     * here so the monthly/daily breakdown is always meaningful; "not active" is
     * derived as total - active in the service.
     */
    @Query("""
            select
              count(e),
              sum(case when upper(coalesce(e.status, '')) = 'ACTIVE' then 1 else 0 end),
              sum(case when lower(coalesce(e.payStatus, '')) like '%monthly%' then 1 else 0 end),
              sum(case when lower(coalesce(e.payStatus, '')) like '%daily%' then 1 else 0 end)
            from Employee e
            where e.companyId = :companyId
              and (:projectId is null or exists (
                    select 1 from Assignment a
                    where a.employeeId = e.id and a.projectId = :projectId))
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.jobTitle, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.email, '')) like lower(concat('%', :q, '%'))
                 or exists (
                      select 1 from ContractPayItem pi
                      where pi.employeeId = e.id and (
                            lower(coalesce(pi.actionSheetNo, '')) like lower(concat('%', :q, '%'))
                         or lower(coalesce(pi.remarks, '')) like lower(concat('%', :q, '%'))
                      )
                 )
              ))
            """)
    Object[] summaryCounts(@Param("companyId") UUID companyId, @Param("q") String q,
                           @Param("projectId") UUID projectId);
}
