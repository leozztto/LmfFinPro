package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.AccountPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.AccountJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository accountJpaRepository;
    private final AccountPersistenceMapper mapper;

    @Override
    public Account save(Account account) {
        return mapper.toDomain(accountJpaRepository.save(mapper.toEntity(account)));
    }

    @Override
    public Optional<Account> findById(Long id) {
        return accountJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Account> findAllByUserId(Long userId) {
        return accountJpaRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        accountJpaRepository.deleteById(id);
    }
}
