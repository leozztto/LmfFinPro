package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDocumentNumber(String documentNumber);

    @Query("SELECT u.id FROM UserJpaEntity u ORDER BY u.id")
    List<Long> findAllIds();

    @Query("SELECT u.sessionVersion FROM UserJpaEntity u WHERE u.id = :id")
    Optional<Integer> findSessionVersionById(@Param("id") Long id);
}
