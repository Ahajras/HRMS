package com.hrms.reference.repository;

import com.hrms.reference.domain.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankRepository extends JpaRepository<Bank, UUID> {

    List<Bank> findAllByOrderByNameAsc();
}
