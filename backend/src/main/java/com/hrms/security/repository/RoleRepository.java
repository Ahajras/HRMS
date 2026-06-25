package com.hrms.security.repository;

import com.hrms.security.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);

    List<Role> findByCompanyIdIsNullOrCompanyId(UUID companyId);
}
