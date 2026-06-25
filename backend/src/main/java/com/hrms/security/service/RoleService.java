package com.hrms.security.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.security.domain.Permission;
import com.hrms.security.domain.Role;
import com.hrms.security.dto.RoleDto;
import com.hrms.security.repository.PermissionRepository;
import com.hrms.security.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Role administration: a role aggregates permissions and may be global or
 * company-scoped (FTDD Vol.2 Ch.31).
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> findAll() {
        UUID companyId = TenantContext.getCompanyId().orElse(null);
        return roleRepository.findByCompanyIdIsNullOrCompanyId(companyId).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RoleDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public RoleDto create(RoleDto dto) {
        UUID companyId = dto.getCompanyId() != null ? dto.getCompanyId()
                : TenantContext.getCompanyId().orElse(null);
        if (roleRepository.existsByCompanyIdAndCode(companyId, dto.getCode())) {
            throw new BusinessRuleException("role.code.duplicate",
                    "Role code already exists: " + dto.getCode());
        }
        Role role = new Role();
        role.setCompanyId(companyId);
        role.setCode(dto.getCode());
        apply(dto, role);
        return toDto(roleRepository.save(role));
    }

    public RoleDto update(UUID id, RoleDto dto) {
        Role role = getEntity(id);
        apply(dto, role);
        return toDto(roleRepository.save(role));
    }

    public void delete(UUID id) {
        roleRepository.delete(getEntity(id));
    }

    private void apply(RoleDto dto, Role role) {
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setPermissions(resolvePermissions(dto.getPermissions()));
    }

    private Set<Permission> resolvePermissions(List<String> codes) {
        Set<Permission> permissions = new LinkedHashSet<>();
        if (codes != null) {
            for (String code : codes) {
                Permission p = permissionRepository.findByCode(code)
                        .orElseThrow(() -> new BusinessRuleException("permission.not.found",
                                "Unknown permission: " + code));
                permissions.add(p);
            }
        }
        return permissions;
    }

    private Role getEntity(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<String> allPermissionCodes() {
        return permissionRepository.findAll().stream().map(Permission::getCode).toList();
    }

    private RoleDto toDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setCompanyId(role.getCompanyId());
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setPermissions(role.getPermissions().stream().map(Permission::getCode).toList());
        return dto;
    }
}
