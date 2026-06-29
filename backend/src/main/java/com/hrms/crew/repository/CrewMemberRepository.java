package com.hrms.crew.repository;

import com.hrms.crew.domain.CrewMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrewMemberRepository extends JpaRepository<CrewMember, UUID> {

    List<CrewMember> findByCrewIdOrderByEffectiveFromDesc(UUID crewId);

    List<CrewMember> findByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);
}
