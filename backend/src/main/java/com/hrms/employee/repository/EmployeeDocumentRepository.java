package com.hrms.employee.repository;

import com.hrms.employee.domain.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {

    List<EmployeeDocument> findByEmployeeIdOrderByDocumentType(UUID employeeId);

    Optional<EmployeeDocument> findByEmployeeIdAndDocumentTypeAndDocumentNumber(
            UUID employeeId, String documentType, String documentNumber);
}
