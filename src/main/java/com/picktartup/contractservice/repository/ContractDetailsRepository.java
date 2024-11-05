package com.picktartup.contractservice.repository;

import com.picktartup.contractservice.entity.Contract;
import com.picktartup.contractservice.entity.ContractDetails;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContractDetailsRepository extends JpaRepository<ContractDetails, Long> {
}
