package com.hrms.employee.repository;

import com.hrms.employee.domain.ContractPayItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContractPayItemRepository extends JpaRepository<ContractPayItem, UUID> {

    List<ContractPayItem> findByContractIdOrderByEffectiveFromDesc(UUID contractId);

    List<ContractPayItem> findByContractIdAndPayComponentIdAndStatus(UUID contractId, UUID payComponentId, String status);
}
