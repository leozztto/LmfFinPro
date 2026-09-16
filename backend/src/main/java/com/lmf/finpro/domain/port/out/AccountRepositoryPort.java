package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepositoryPort {
    Account save(Account account);
    Optional<Account> findById(Long id);
    List<Account> findAllByUserId(Long userId);
    void deleteById(Long id);
}
