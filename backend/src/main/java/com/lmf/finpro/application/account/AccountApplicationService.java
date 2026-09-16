package com.lmf.finpro.application.account;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private final AccountRepositoryPort accountRepositoryPort;

    public Account create(Long currentUserId, String name, AccountType type, BigDecimal initialBalance) {
        return accountRepositoryPort.save(Account.create(currentUserId, name, type, initialBalance));
    }

    public List<Account> list(Long currentUserId) {
        return accountRepositoryPort.findAllByUserId(currentUserId);
    }

    public Account getById(Long currentUserId, Long accountId) {
        return findOwnedOrThrow(currentUserId, accountId);
    }

    public Account update(Long currentUserId, Long accountId, String name, AccountType type, BigDecimal initialBalance) {
        Account existing = findOwnedOrThrow(currentUserId, accountId);
        return accountRepositoryPort.save(existing.withDetails(name, type, initialBalance));
    }

    public void delete(Long currentUserId, Long accountId) {
        findOwnedOrThrow(currentUserId, accountId);
        accountRepositoryPort.deleteById(accountId);
    }

    /** Acesso a conta de outro usuário é tratado como inexistente (404), não como 403. */
    private Account findOwnedOrThrow(Long currentUserId, Long accountId) {
        return accountRepositoryPort.findById(accountId)
            .filter(account -> account.belongsTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }
}
