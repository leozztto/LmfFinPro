package com.lmf.finpro.application.categoryrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRuleRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryRuleApplicationServiceTest {

    @Mock private CategoryRuleRepositoryPort categoryRuleRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;

    @InjectMocks private CategoryRuleApplicationService service;

    @Test
    void createSavesRuleWithTrimmedPatternWhenCategoryIsVisible() {
        Category category = Category.create(10L, "Transporte", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(category));
        when(categoryRuleRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryRule created = service.create(10L, "  UBER  ", 5L);

        assertThat(created.pattern()).isEqualTo("UBER");
        assertThat(created.categoryId()).isEqualTo(5L);
        assertThat(created.weight()).isEqualTo(1);
    }

    @Test
    void createThrowsWhenCategoryIsNotVisibleToUser() {
        Category othersCategory =
                Category.create(999L, "Transporte", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(othersCategory));

        assertThatThrownBy(() -> service.create(10L, "UBER", 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsRulesOrderedByWeightDescending() {
        CategoryRule rule = new CategoryRule(1L, 10L, "UBER", 5L, 3);
        when(categoryRuleRepositoryPort.findAllByUserIdOrderByWeightDesc(10L))
                .thenReturn(List.of(rule));

        assertThat(service.list(10L)).containsExactly(rule);
    }

    @Test
    void deleteRemovesRuleWhenOwned() {
        CategoryRule rule = new CategoryRule(1L, 10L, "UBER", 5L, 3);
        when(categoryRuleRepositoryPort.findById(1L)).thenReturn(Optional.of(rule));

        service.delete(10L, 1L);

        verify(categoryRuleRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenRuleBelongsToAnotherUser() {
        CategoryRule rule = new CategoryRule(1L, 10L, "UBER", 5L, 3);
        when(categoryRuleRepositoryPort.findById(1L)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.delete(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRuleRepositoryPort, never()).deleteById(any());
    }
}
