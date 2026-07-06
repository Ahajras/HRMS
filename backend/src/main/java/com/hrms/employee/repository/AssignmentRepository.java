package com.hrms.employee.repository;

import com.hrms.employee.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);

    List<Assignment> findByOrganizationUnitId(UUID organizationUnitId);

    /** All active, project-bearing assignments company-wide, most recent first —
     * used to build an in-memory employee-to-project map in one query instead of
     * one query per employee (that N+1 pattern is what made bulk operations
     * crawl once the tables grew large). */
    @org.springframework.data.jpa.repository.Query(
        "select a from Assignment a join Employee e on e.id = a.employeeId " +
        "where e.companyId = :companyId and a.status = 'ACTIVE' and a.projectId is not null " +
        "order by a.effectiveFrom desc")
    List<Assignment> findActiveWithProjectByCompanyId(@org.springframework.data.repository.query.Param("companyId") UUID companyId);

    @org.springframework.data.jpa.repository.Query(
        "select a from Assignment a join Employee e on e.id = a.employeeId " +
        "where e.companyId = :companyId and a.employeeId in :employeeIds " +
        "and a.status = 'ACTIVE' and a.projectId is not null " +
        "order by a.employeeId, a.effectiveFrom desc")
    List<Assignment> findActiveWithProjectByCompanyIdAndEmployeeIdIn(
            @org.springframework.data.repository.query.Param("companyId") UUID companyId,
            @org.springframework.data.repository.query.Param("employeeIds") Collection<UUID> employeeIds);
}
