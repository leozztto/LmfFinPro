package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {
    List<AccountJpaEntity> findByUserId(Long userId);
}
