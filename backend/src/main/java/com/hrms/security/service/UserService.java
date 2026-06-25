package com.hrms.security.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.security.domain.AppUser;
import com.hrms.security.domain.Permission;
import com.hrms.security.domain.Role;
import com.hrms.security.dto.UserDto;
import com.hrms.security.repository.AppUserRepository;
import com.hrms.security.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Application user administration (FTDD Vol.2 Ch.31). Company users are scoped to
 * their tenant; platform admins (no tenant) see all users.
 */
@Service
@Transactional
public class UserService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        Optional<UUID> companyId = TenantContext.getCompanyId();
        List<AppUser> users = companyId.map(userRepository::findByCompanyId)
                .orElseGet(userRepository::findAll);
        return users.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public UserDto create(UserDto dto) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessRuleException("user.password.required", "Password is required for a new user");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessRuleException("user.username.duplicate",
                    "Username already exists: " + dto.getUsername());
        }
        AppUser user = new AppUser();
        user.setCompanyId(dto.getCompanyId() != null ? dto.getCompanyId()
                : TenantContext.getCompanyId().orElse(null));
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        applyMutable(dto, user);
        return toDto(userRepository.save(user));
    }

    public UserDto update(UUID id, UserDto dto) {
        AppUser user = getEntity(id);
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        applyMutable(dto, user);
        return toDto(userRepository.save(user));
    }

    public void delete(UUID id) {
        userRepository.delete(getEntity(id));
    }

    private void applyMutable(UserDto dto, AppUser user) {
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setEmployeeId(dto.getEmployeeId());
        if (StringUtils.hasText(dto.getStatus())) {
            user.setStatus(dto.getStatus());
        }
        user.setRoles(resolveRoles(dto.getRoles()));
    }

    private Set<Role> resolveRoles(List<String> roleCodes) {
        Set<Role> roles = new LinkedHashSet<>();
        if (roleCodes != null) {
            for (String code : roleCodes) {
                Role role = roleRepository.findByCode(code)
                        .orElseThrow(() -> new BusinessRuleException("role.not.found",
                                "Unknown role: " + code));
                roles.add(role);
            }
        }
        return roles;
    }

    private AppUser getEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public UserDto toDto(AppUser user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setCompanyId(user.getCompanyId());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setStatus(user.getStatus());
        dto.setLastLoginAt(user.getLastLoginAt());

        Set<String> authorities = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            dto.getRoles().add(role.getCode());
            for (Permission p : role.getPermissions()) {
                authorities.add(p.getCode());
            }
        }
        dto.setAuthorities(List.copyOf(authorities));
        return dto;
    }
}
