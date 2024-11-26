package com.picktartup.contractservice.repository;

import com.picktartup.contractservice.entity.TokenTransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenTransferTransactionRepository extends JpaRepository<TokenTransferTransaction, Long> {
}
