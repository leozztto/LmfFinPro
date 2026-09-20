package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TransferJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, Long> {
    List<TransferJpaEntity> findByUserId(Long userId);

    boolean existsByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);
}
