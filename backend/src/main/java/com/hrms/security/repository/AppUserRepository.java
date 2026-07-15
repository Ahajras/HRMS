package com.hrms.security.repository;

import com.hrms.security.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AppUser> findByCompanyId(UUID companyId);

    List<AppUser> findByCompanyIdAndEmployeeIdAndStatus(UUID companyId, UUID employeeId, String status);
}
