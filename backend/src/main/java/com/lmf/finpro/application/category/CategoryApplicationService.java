package com.lmf.finpro.application.category;

import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryApplicationService {

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    public Category create(Long currentUserId, String name, CategoryType type, String color, String icon) {
        return categoryRepositoryPort.save(Category.create(currentUserId, name, type, color, icon));
    }

    /** Categorias do usuário + categorias globais do sistema. */
    public List<Category> list(Long currentUserId) {
        return categoryRepositoryPort.findAllVisibleToUser(currentUserId);
    }

    public Category getById(Long currentUserId, Long categoryId) {
        return findVisibleOrThrow(currentUserId, categoryId);
    }

    /** Categorias globais são somente leitura via API neste MVP — só as próprias podem ser alteradas. */
    public Category update(Long currentUserId, Long categoryId, String name, CategoryType type, String color, String icon) {
        Category existing = findOwnedOrThrow(currentUserId, categoryId);
        return categoryRepositoryPort.save(existing.withDetails(name, type, color, icon));
    }

    public void delete(Long currentUserId, Long categoryId) {
        findOwnedOrThrow(currentUserId, categoryId);
        if (transactionRepositoryPort.existsByCategoryId(categoryId)) {
            throw new EntityHasLinkedRecordsException(
                "Esta categoria possui transações vinculadas. Exclua-as ou troque a categoria delas antes de remover."
            );
        }
        categoryRepositoryPort.deleteById(categoryId);
    }

    private Category findVisibleOrThrow(Long currentUserId, Long categoryId) {
        return categoryRepositoryPort.findById(categoryId)
            .filter(category -> category.isVisibleTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + categoryId));
    }

    private Category findOwnedOrThrow(Long currentUserId, Long categoryId) {
        return categoryRepositoryPort.findById(categoryId)
            .filter(category -> category.isOwnedBy(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + categoryId));
    }
}
