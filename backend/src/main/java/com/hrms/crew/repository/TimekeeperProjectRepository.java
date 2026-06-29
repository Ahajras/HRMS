package com.hrms.crew.repository;

import com.hrms.crew.domain.TimekeeperProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimekeeperProjectRepository extends JpaRepository<TimekeeperProject, UUID> {

    List<TimekeeperProject> findByCompanyIdOrderById(UUID companyId);

    List<TimekeeperProject> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);
}
