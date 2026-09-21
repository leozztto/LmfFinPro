package com.lmf.finpro.application.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryApplicationServiceTest {

    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;

    @InjectMocks private CategoryApplicationService service;

    @Test
    void createSavesCategoryBuiltFromInput() {
        when(categoryRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category created = service.create(10L, "Consultoria", CategoryType.INCOME, "#fff", "icon");

        assertThat(created.userId()).isEqualTo(10L);
        assertThat(created.name()).isEqualTo("Consultoria");
        assertThat(created.type()).isEqualTo(CategoryType.INCOME);
    }

    @Test
    void listReturnsCategoriesVisibleToUser() {
        Category category = Category.create(10L, "Consultoria", CategoryType.INCOME, null, null);
        when(categoryRepositoryPort.findAllVisibleToUser(10L)).thenReturn(List.of(category));

        assertThat(service.list(10L)).containsExactly(category);
    }

    @Test
    void getByIdReturnsGlobalCategoryForAnyUser() {
        Category global = new Category(1L, null, "Moradia", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(1L)).thenReturn(Optional.of(global));

        assertThat(service.getById(10L, 1L)).isEqualTo(global);
    }

    @Test
    void getByIdThrowsWhenCategoryBelongsToAnotherUser() {
        Category owned = new Category(1L, 10L, "Consultoria", CategoryType.INCOME, null, null);
        when(categoryRepositoryPort.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> service.getById(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateKeepsOriginalTypeRegardlessOfInput() {
        Category existing = new Category(1L, 10L, "Antigo", CategoryType.EXPENSE, "#000", "old");
        when(categoryRepositoryPort.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = service.update(10L, 1L, "Novo", "#fff", "new");

        assertThat(updated.name()).isEqualTo("Novo");
        assertThat(updated.type()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void updateThrowsWhenCategoryIsGlobal() {
        Category global = new Category(1L, null, "Moradia", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(1L)).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> service.update(10L, 1L, "Novo", "#fff", "new"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesCategoryWhenNoLinkedTransactions() {
        Category existing = new Category(1L, 10L, "Consultoria", CategoryType.INCOME, null, null);
        when(categoryRepositoryPort.findById(1L)).thenReturn(Optional.of(existing));
        when(transactionRepositoryPort.existsByCategoryId(1L)).thenReturn(false);

        service.delete(10L, 1L);

        verify(categoryRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenCategoryHasLinkedTransactions() {
        Category existing = new Category(1L, 10L, "Consultoria", CategoryType.INCOME, null, null);
        when(categoryRepositoryPort.findById(1L)).thenReturn(Optional.of(existing));
        when(transactionRepositoryPort.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(EntityHasLinkedRecordsException.class);
        verify(categoryRepositoryPort, never()).deleteById(any());
    }

    @Test
    void deleteThrowsWhenCategoryIsGlobal() {
        Category global = new Category(1L, null, "Moradia", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(1L)).thenReturn(Optional.of(global));

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
