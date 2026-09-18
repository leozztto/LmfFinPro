package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Transfer;

import java.util.List;
import java.util.Optional;

public interface TransferRepositoryPort {
    Transfer save(Transfer transfer);
    Optional<Transfer> findById(Long id);
    List<Transfer> findAllByUserId(Long userId);
    boolean existsByAccountId(Long accountId);
    void deleteById(Long id);
}
