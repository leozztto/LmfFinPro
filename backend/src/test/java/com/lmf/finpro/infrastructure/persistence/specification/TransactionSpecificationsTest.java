package com.lmf.finpro.infrastructure.persistence.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionSearchCriteria;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Garante a regra principal da busca: filtro nulo não vira predicado — a consulta só recebe as
 * condições que o usuário informou.
 */
class TransactionSpecificationsTest {

    private Root<TransactionJpaEntity> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class, RETURNS_DEEP_STUBS);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        Join<TransactionJpaEntity, AccountJpaEntity> account = mock(Join.class, RETURNS_DEEP_STUBS);
        when(root.<TransactionJpaEntity, AccountJpaEntity>join("account")).thenReturn(account);
    }

    @Test
    void onlyUserFilterWhenEverythingElseIsNull() {
        Predicate[] predicates = predicatesFor(criteria(null, null, null, null, false));

        assertThat(predicates).hasSize(1);
        verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(LocalDate.class));
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(LocalDate.class));
        verify(cb, never()).like(any(), anyString(), anyChar());
        verify(cb, never()).isNull(any());
    }

    @Test
    void addsOnePredicatePerInformedFilter() {
        Predicate[] predicates =
                predicatesFor(
                        criteria(
                                CategoryType.EXPENSE,
                                LocalDate.of(2026, 9, 1),
                                null,
                                "aluguel",
                                true));

        // usuário + sem transferências + tipo + data inicial + descrição
        assertThat(predicates).hasSize(5);
        verify(cb).like(any(), eq("%aluguel%"), eq('\\'));
    }

    @Test
    void allFiltersInformedAddsAllPredicates() {
        TransactionSearchCriteria all =
                new TransactionSearchCriteria(
                        10L,
                        CategoryType.INCOME,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        1L,
                        AccountScope.BUSINESS,
                        2L,
                        3L,
                        TransactionStatus.PAID,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        "projeto",
                        true);

        assertThat(predicatesFor(all)).hasSize(13);
    }

    @Test
    void blankDescriptionIsTreatedAsNotInformed() {
        predicatesFor(criteria(null, null, null, "   ", false));

        verify(cb, never()).like(any(), anyString(), anyChar());
    }

    @Test
    void likeWildcardsTypedByTheUserAreEscaped() {
        predicatesFor(criteria(null, null, null, "50%_off", false));

        verify(cb).like(any(), eq("%50\\%\\_off%"), eq('\\'));
    }

    private Predicate[] predicatesFor(TransactionSearchCriteria criteria) {
        TransactionSpecifications.matching(criteria).toPredicate(root, query, cb);
        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        return captor.getValue();
    }

    private static TransactionSearchCriteria criteria(
            CategoryType type,
            LocalDate startDate,
            LocalDate endDate,
            String description,
            boolean excludeTransfers) {
        return new TransactionSearchCriteria(
                10L,
                type,
                startDate,
                endDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                description,
                excludeTransfers);
    }
}
