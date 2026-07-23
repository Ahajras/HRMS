package com.hrms.employee.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.domain.EmployeeDocument;
import com.hrms.employee.dto.EmployeeDocumentDto;
import com.hrms.employee.repository.EmployeeDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

/**
 * CRUD for employee identity documents.
 */
@Service
@Transactional
public class EmployeeDocumentService {

    private final EmployeeDocumentRepository repository;
    private final DocumentExpiryNotificationService notificationService;

    public EmployeeDocumentService(EmployeeDocumentRepository repository,
                                   DocumentExpiryNotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentDto> findByEmployee(UUID employeeId) {
        return repository.findByEmployeeIdOrderByDocumentType(employeeId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDocumentDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public EmployeeDocumentDto create(EmployeeDocumentDto dto) {
        EmployeeDocument entity = new EmployeeDocument();
        entity.setEmployeeId(dto.getEmployeeId());
        apply(dto, entity);
        EmployeeDocument saved = repository.saveAndFlush(entity);
        notificationService.sendIfDue(saved.getId());
        return toDto(saved);
    }

    public EmployeeDocumentDto update(UUID id, EmployeeDocumentDto dto) {
        EmployeeDocument entity = getEntity(id);
        apply(dto, entity);
        EmployeeDocument saved = repository.saveAndFlush(entity);
        notificationService.sendIfDue(saved.getId());
        return toDto(saved);
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private EmployeeDocument getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    private void apply(EmployeeDocumentDto dto, EmployeeDocument entity) {
        if (dto.getExpiryDate() != null && dto.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("DOCUMENT_EXPIRED", "This document is already expired.");
        }
        entity.setDocumentType(dto.getDocumentType());
        entity.setDocumentNumber(dto.getDocumentNumber());
        entity.setIssuingCountryCode(dto.getIssuingCountryCode());
        entity.setIssueDate(dto.getIssueDate());
        entity.setExpiryDate(dto.getExpiryDate());
        entity.setIssuingAuthority(dto.getIssuingAuthority());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private EmployeeDocumentDto toDto(EmployeeDocument entity) {
        EmployeeDocumentDto dto = new EmployeeDocumentDto();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setDocumentType(entity.getDocumentType());
        dto.setDocumentNumber(entity.getDocumentNumber());
        dto.setIssuingCountryCode(entity.getIssuingCountryCode());
        dto.setIssueDate(entity.getIssueDate());
        dto.setExpiryDate(entity.getExpiryDate());
        dto.setIssuingAuthority(entity.getIssuingAuthority());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
