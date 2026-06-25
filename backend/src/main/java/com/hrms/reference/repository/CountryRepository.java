package com.hrms.reference.repository;

import com.hrms.reference.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, UUID> {

    Optional<Country> findByCode(String code);

    boolean existsByCode(String code);
}
