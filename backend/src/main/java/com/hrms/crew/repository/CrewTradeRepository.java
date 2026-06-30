package com.hrms.crew.repository;

import com.hrms.crew.domain.CrewTrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrewTradeRepository extends JpaRepository<CrewTrade, UUID> {

    List<CrewTrade> findByCrewIdOrderByTradeCode(UUID crewId);
}
