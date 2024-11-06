package com.picktartup.contractservice.repository;

import com.picktartup.contractservice.entity.ContractDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDetailsRepository extends JpaRepository<ContractDetails, Long> {

    // Contract 엔티티의 contractId를 통해 ContractDetails 조회
    ContractDetails findByContract_ContractId(Long contractId);
}
