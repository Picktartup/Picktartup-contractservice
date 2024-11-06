package com.picktartup.contractservice.repository;

import com.picktartup.contractservice.entity.Contract;
import com.picktartup.contractservice.entity.ContractStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.contractDetails WHERE c.contractId = :contractId")
    Optional<Contract> findByIdWithDetails(@Param("contractId") Long contractId);

    // 계약 상태에 따른 투자내역 조회
    @EntityGraph(attributePaths = {"contractDetails"})
    List<Contract> findByUserIdAndStatus(Long userId, ContractStatus status);

}
