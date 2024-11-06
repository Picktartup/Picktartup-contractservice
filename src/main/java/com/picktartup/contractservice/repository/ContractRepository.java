package com.picktartup.contractservice.repository;

import com.picktartup.contractservice.entity.Contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.contractDetails WHERE c.contractId = :contractId")
    Optional<Contract> findByIdWithDetails(@Param("contractId") Long contractId);

}
