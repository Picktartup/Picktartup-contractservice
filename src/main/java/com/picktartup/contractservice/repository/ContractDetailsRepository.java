package com.picktartup.contractservice.repository;

import com.picktartup.contractservice.entity.ContractDetails;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDetailsRepository extends JpaRepository<ContractDetails, Long> {

}
