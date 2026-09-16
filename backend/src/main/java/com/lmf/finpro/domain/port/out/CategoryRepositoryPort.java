package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {
    Category save(Category category);
    Optional<Category> findById(Long id);

    /** Categorias do usuário + categorias padrão do sistema (globais). */
    List<Category> findAllVisibleToUser(Long userId);

    void deleteById(Long id);
}
