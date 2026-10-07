package com.lmf.finpro.application.category;

import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringBudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryApplicationService {

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final BudgetRepositoryPort budgetRepositoryPort;
    private final RecurringBudgetRepositoryPort recurringBudgetRepositoryPort;

    public Category create(
            Long currentHouseholdId, String name, CategoryType type, String color, String icon) {
        log.debug("Criando categoria do tipo={} para o usuário={}", type, currentHouseholdId);
        Category saved =
                categoryRepositoryPort.save(
                        Category.create(currentHouseholdId, name, type, color, icon));
        log.debug("Categoria={} criada para o usuário={}", saved.id(), currentHouseholdId);
        return saved;
    }

    /** Categorias do usuário + categorias globais do sistema. */
    public List<Category> list(Long currentHouseholdId) {
        log.debug("Listando categorias do usuário={}", currentHouseholdId);
        return categoryRepositoryPort.findAllVisibleToUser(currentHouseholdId);
    }

    public Category getById(Long currentHouseholdId, Long categoryId) {
        log.debug("Buscando categoria={} do usuário={}", categoryId, currentHouseholdId);
        return findVisibleOrThrow(currentHouseholdId, categoryId);
    }

    /**
     * Categorias globais são somente leitura via API neste MVP — só as próprias podem ser
     * alteradas. O tipo (receita/despesa) não pode ser alterado na edição: transações já lançadas
     * nessa categoria foram validadas contra o tipo original, e trocá-lo depois as deixaria
     * inconsistentes.
     */
    public Category update(
            Long currentHouseholdId, Long categoryId, String name, String color, String icon) {
        Category existing = findOwnedOrThrow(currentHouseholdId, categoryId);
        log.debug("Atualizando categoria={} do usuário={}", categoryId, currentHouseholdId);
        return categoryRepositoryPort.save(
                existing.withDetails(name, existing.type(), color, icon));
    }

    public void delete(Long currentHouseholdId, Long categoryId) {
        log.debug("Removendo categoria={} do usuário={}", categoryId, currentHouseholdId);
        findOwnedOrThrow(currentHouseholdId, categoryId);
        if (transactionRepositoryPort.existsByCategoryId(categoryId)) {
            throw new EntityHasLinkedRecordsException(
                    "Esta categoria possui transações vinculadas. Exclua-as ou troque a categoria"
                            + " delas antes de remover.");
        }
        if (recurringBudgetRepositoryPort.existsByCategoryId(categoryId)) {
            throw new EntityHasLinkedRecordsException(
                    "Esta categoria possui um orçamento recorrente vinculado. Exclua-o antes de"
                            + " remover a categoria.");
        }
        // budgets.category_id é ON DELETE CASCADE: sem essa checagem, excluir a categoria apagaria
        // orçamentos existentes em silêncio, sem o usuário perceber.
        if (budgetRepositoryPort.existsByCategoryId(categoryId)) {
            throw new EntityHasLinkedRecordsException(
                    "Esta categoria possui orçamentos vinculados. Exclua-os antes de remover a"
                            + " categoria.");
        }
        categoryRepositoryPort.deleteById(categoryId);
    }

    private Category findVisibleOrThrow(Long currentHouseholdId, Long categoryId) {
        return categoryRepositoryPort
                .findById(categoryId)
                .filter(category -> category.isVisibleTo(currentHouseholdId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Categoria não encontrada: " + categoryId));
    }

    private Category findOwnedOrThrow(Long currentHouseholdId, Long categoryId) {
        return categoryRepositoryPort
                .findById(categoryId)
                .filter(category -> category.isOwnedBy(currentHouseholdId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Categoria não encontrada: " + categoryId));
    }
}
