package com.lmf.finpro.repository;

import com.lmf.finpro.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdAndTransactionDateBetween(Long accountId, LocalDate start, LocalDate end);
    List<Transaction> findByClientId(Long clientId);
}
