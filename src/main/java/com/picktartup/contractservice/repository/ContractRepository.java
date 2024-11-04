package com.picktartup.contractservice.repository;

import com.picktartup.contractservice.entity.Contract;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

}
