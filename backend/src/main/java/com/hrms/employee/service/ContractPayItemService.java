package com.hrms.employee.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.domain.ContractPayItem;
import com.hrms.employee.dto.ContractPayItemDto;
import com.hrms.employee.repository.ContractPayItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for contract pay items, with effective-dated supersession.
 *
 * <p>Creating a new item for a component that already has an active item ends
 * the previous one the day before the new effective date and marks it
 * {@code INACTIVE}, keeping it as history. The new item becomes the active
 * version (FTDD: a change creates a new version; history is never overwritten).
 */
@Service
@Transactional
public class ContractPayItemService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final ContractPayItemRepository repository;

    public ContractPayItemService(ContractPayItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ContractPayItemDto> findByContract(UUID contractId) {
        return repository.findByContractIdOrderByEffectiveFromDesc(contractId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ContractPayItemDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    /**
     * Adds a new pay item. If an active item already exists for the same
     * contract + component, it is superseded: its effective_to is set to the
     * day before the new item's effective_from and its status becomes INACTIVE.
     */
    public ContractPayItemDto create(ContractPayItemDto dto) {
        List<ContractPayItem> current = repository
                .findByContractIdAndPayComponentIdAndStatus(dto.getContractId(), dto.getPayComponentId(), ACTIVE);

        for (ContractPayItem existing : current) {
            if (!dto.getEffectiveFrom().isAfter(existing.getEffectiveFrom())) {
                throw new BusinessRuleException("payitem.effective.order",
                        "New effective date must be after the current item's effective date ("
                                + existing.getEffectiveFrom() + ").");
            }
            existing.setEffectiveTo(dto.getEffectiveFrom().minusDays(1));
            existing.setStatus(INACTIVE);
            repository.save(existing);
        }

        ContractPayItem entity = new ContractPayItem();
        entity.setContractId(dto.getContractId());
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setPayComponentId(dto.getPayComponentId());
        apply(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(ACTIVE);
        }
        return toDto(repository.save(entity));
    }

    /** In-place correction of an existing item (no supersession). */
    public ContractPayItemDto update(UUID id, ContractPayItemDto dto) {
        ContractPayItem entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private ContractPayItem getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pay item not found: " + id));
    }

    private void apply(ContractPayItemDto dto, ContractPayItem entity) {
        entity.setAmount(dto.getAmount());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setRemarks(dto.getRemarks());
        entity.setActionSheetNo(dto.getActionSheetNo());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private ContractPayItemDto toDto(ContractPayItem entity) {
        ContractPayItemDto dto = new ContractPayItemDto();
        dto.setId(entity.getId());
        dto.setContractId(entity.getContractId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setPayComponentId(entity.getPayComponentId());
        dto.setAmount(entity.getAmount());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        dto.setActionSheetNo(entity.getActionSheetNo());
        return dto;
    }
}
