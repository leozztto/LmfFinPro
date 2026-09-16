package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {
    // Categorias do usuário + categorias padrão do sistema (user_id IS NULL)
    List<CategoryJpaEntity> findByUserIdOrUserIdIsNull(Long userId);
}
