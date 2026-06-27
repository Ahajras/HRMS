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
}
