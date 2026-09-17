package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepositoryPort {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    List<Transaction> findAllByAccountIds(List<Long> accountIds);
    List<Transaction> findAllByTransferId(Long transferId);
    void deleteById(Long id);
}
