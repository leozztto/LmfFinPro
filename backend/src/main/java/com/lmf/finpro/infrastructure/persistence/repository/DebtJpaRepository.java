package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.DebtJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtJpaRepository extends JpaRepository<DebtJpaEntity, Long> {
    List<DebtJpaEntity> findByUserIdOrderByNameAsc(Long userId);
}
