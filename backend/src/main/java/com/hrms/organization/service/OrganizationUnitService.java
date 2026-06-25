package com.hrms.organization.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.organization.domain.OrganizationUnit;
import com.hrms.organization.dto.OrgUnitTreeNode;
import com.hrms.organization.dto.OrganizationUnitDto;
import com.hrms.organization.repository.OrganizationUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the organisation tree (FTDD Vol.2 Ch.32.7). All operations are
 * company-scoped via {@link TenantContext}.
 */
@Service
@Transactional
public class OrganizationUnitService {

    private final OrganizationUnitRepository repository;

    public OrganizationUnitService(OrganizationUnitRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationUnitDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyId(companyId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationUnitDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    /** Builds the full hierarchy as a list of root nodes with nested children. */
    @Transactional(readOnly = true)
    public List<OrgUnitTreeNode> getTree() {
        UUID companyId = TenantContext.requireCompanyId();
        List<OrganizationUnit> units = repository.findByCompanyId(companyId);

        Map<UUID, OrgUnitTreeNode> nodes = new LinkedHashMap<>();
        for (OrganizationUnit u : units) {
            nodes.put(u.getId(), toNode(u));
        }
        List<OrgUnitTreeNode> roots = new ArrayList<>();
        for (OrganizationUnit u : units) {
            OrgUnitTreeNode node = nodes.get(u.getId());
            if (u.getParentId() != null && nodes.containsKey(u.getParentId())) {
                nodes.get(u.getParentId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    public OrganizationUnitDto create(OrganizationUnitDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        if (repository.existsByCompanyIdAndCode(companyId, dto.getCode())) {
            throw new BusinessRuleException("org.unit.code.duplicate",
                    "Organisation unit code already exists: " + dto.getCode());
        }
        OrganizationUnit entity = new OrganizationUnit();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public OrganizationUnitDto update(UUID id, OrganizationUnitDto dto) {
        OrganizationUnit entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private OrganizationUnit getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation unit not found: " + id));
    }

    private void apply(OrganizationUnitDto dto, OrganizationUnit entity) {
        entity.setParentId(dto.getParentId());
        entity.setTypeId(dto.getTypeId());
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private OrganizationUnitDto toDto(OrganizationUnit entity) {
        OrganizationUnitDto dto = new OrganizationUnitDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setParentId(entity.getParentId());
        dto.setTypeId(entity.getTypeId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private OrgUnitTreeNode toNode(OrganizationUnit entity) {
        OrgUnitTreeNode node = new OrgUnitTreeNode();
        node.setId(entity.getId());
        node.setParentId(entity.getParentId());
        node.setTypeId(entity.getTypeId());
        node.setCode(entity.getCode());
        node.setName(entity.getName());
        node.setEffectiveFrom(entity.getEffectiveFrom());
        node.setEffectiveTo(entity.getEffectiveTo());
        node.setStatus(entity.getStatus());
        return node;
    }
}
