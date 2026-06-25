package com.hrms.reference.repository;

import com.hrms.reference.domain.LookupValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LookupValueRepository extends JpaRepository<LookupValue, UUID> {

    List<LookupValue> findByCategoryOrderBySortOrderAscLabelAsc(String category);
}
