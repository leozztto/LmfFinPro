package com.lmf.finpro.repository;

import com.lmf.finpro.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Categorias do usuário + categorias padrão do sistema (user_id IS NULL)
    List<Category> findByUserIdOrUserIdIsNull(Long userId);
}
