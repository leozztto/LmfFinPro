package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {
    // Categorias do usuário + categorias padrão do sistema (user_id IS NULL)
    List<CategoryJpaEntity> findByUserIdOrUserIdIsNull(Long userId);
}
