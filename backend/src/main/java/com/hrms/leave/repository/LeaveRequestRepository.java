package com.hrms.leave.repository;

import com.hrms.leave.domain.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByCompanyIdOrderByStartDateDesc(UUID companyId);
    List<LeaveRequest> findByCompanyIdAndEmployeeIdOrderByStartDateDesc(UUID companyId, UUID employeeId);

    @Query(value = """
            select lr from LeaveRequest lr
            join Employee e on e.id = lr.employeeId
            join Assignment a on a.employeeId = e.id
            where lr.companyId = :companyId
              and (:employeeId is null or lr.employeeId = :employeeId)
              and (:projectId is null or a.projectId = :projectId)
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:status is null or :status = '' or lr.status = :status)
              and (:leaveTypeId is null or lr.leaveTypeId = :leaveTypeId)
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            order by lr.startDate desc
            """,
            countQuery = """
            select count(lr.id) from LeaveRequest lr
            join Employee e on e.id = lr.employeeId
            join Assignment a on a.employeeId = e.id
            where lr.companyId = :companyId
              and (:employeeId is null or lr.employeeId = :employeeId)
              and (:projectId is null or a.projectId = :projectId)
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:status is null or :status = '' or lr.status = :status)
              and (:leaveTypeId is null or lr.leaveTypeId = :leaveTypeId)
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            """)
    Page<LeaveRequest> search(@Param("companyId") UUID companyId,
                              @Param("employeeId") UUID employeeId,
                              @Param("projectId") UUID projectId,
                              @Param("status") String status,
                              @Param("leaveTypeId") UUID leaveTypeId,
                              @Param("q") String q,
                              Pageable pageable);

    @Query(value = """
            select lr from LeaveRequest lr
            join Employee e on e.id = lr.employeeId
            join Assignment a on a.employeeId = e.id
            where lr.companyId = :companyId
              and (:employeeId is null or lr.employeeId = :employeeId)
              and a.projectId in :projectIds
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:status is null or :status = '' or lr.status = :status)
              and (:leaveTypeId is null or lr.leaveTypeId = :leaveTypeId)
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            order by lr.startDate desc
            """,
            countQuery = """
            select count(lr.id) from LeaveRequest lr
            join Employee e on e.id = lr.employeeId
            join Assignment a on a.employeeId = e.id
            where lr.companyId = :companyId
              and (:employeeId is null or lr.employeeId = :employeeId)
              and a.projectId in :projectIds
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:status is null or :status = '' or lr.status = :status)
              and (:leaveTypeId is null or lr.leaveTypeId = :leaveTypeId)
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            """)
    Page<LeaveRequest> searchByProjects(@Param("companyId") UUID companyId,
                                        @Param("employeeId") UUID employeeId,
                                        @Param("projectIds") Collection<UUID> projectIds,
                                        @Param("status") String status,
                                        @Param("leaveTypeId") UUID leaveTypeId,
                                        @Param("q") String q,
                                        Pageable pageable);

    @Query(value = """
            select
              p.id as project_id,
              p.code as project_code,
              p.name as project_name,
              coalesce(nullif(upper(e.pay_status), ''), 'UNASSIGNED') as pay_group,
              count(lr.id) as total,
              sum(case when lr.status in ('DRAFT', 'SUBMITTED') then 1 else 0 end) as pending,
              sum(case when lr.status = 'APPROVED' then 1 else 0 end) as approved,
              sum(case when lr.status = 'REJECTED' then 1 else 0 end) as rejected,
              sum(case when lr.status = 'APPROVED' then coalesce(lr.total_days, 0) else 0 end) as approved_days
            from project p
            left join assignment a on a.project_id = p.id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
            left join employee e on e.id = a.employee_id
              and e.company_id = :companyId
            left join leave_request lr on lr.employee_id = a.employee_id
              and lr.company_id = :companyId
              and (:status is null or :status = '' or lr.status = :status)
              and (:leaveTypeId is null or lr.leave_type_id = :leaveTypeId)
              and (:fromDate is null or lr.end_date >= :fromDate)
              and (:toDate is null or lr.start_date <= :toDate)
            where p.company_id = :companyId
              and (:projectId is null or p.id = :projectId)
            group by p.id, p.code, p.name, coalesce(nullif(upper(e.pay_status), ''), 'UNASSIGNED')
            order by p.code, pay_group
            """, nativeQuery = true)
    List<Object[]> projectSummary(@Param("companyId") UUID companyId,
                                  @Param("projectId") UUID projectId,
                                  @Param("status") String status,
                                  @Param("leaveTypeId") UUID leaveTypeId,
                                  @Param("fromDate") LocalDate fromDate,
                                  @Param("toDate") LocalDate toDate);

    List<LeaveRequest> findByCompanyIdAndEmployeeIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
            UUID companyId, UUID employeeId, String status, LocalDate start, LocalDate end);
    List<LeaveRequest> findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndStatusInAndStartDateLessThanEqual(
            UUID companyId, UUID employeeId, UUID leaveTypeId, Collection<String> statuses, LocalDate asOfDate);
}
