package com.lmf.finpro.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import org.junit.jupiter.api.Test;

/**
 * Categorias globais (sem dono) só existem via inserção direta no banco — nenhum teste de
 * integração passa por esse caminho, então a lógica de mapeamento é coberta aqui isoladamente.
 */
class CategoryPersistenceMapperTest {

    private final CategoryPersistenceMapper mapper = new CategoryPersistenceMapper();

    @Test
    void toEntityLeavesHouseholdNullForAGlobalCategory() {
        Category globalCategory =
                Category.create(null, "Moradia", CategoryType.EXPENSE, null, null);

        CategoryJpaEntity entity = mapper.toEntity(globalCategory);

        assertThat(entity.getHouseholdId()).isNull();
    }

    @Test
    void toEntitySetsTheHouseholdForAnOwnedCategory() {
        Category ownedCategory =
                Category.create(10L, "Consultoria", CategoryType.INCOME, null, null);

        CategoryJpaEntity entity = mapper.toEntity(ownedCategory);

        assertThat(entity.getHouseholdId()).isEqualTo(10L);
    }

    @Test
    void toDomainMapsANullHouseholdToANullHouseholdId() {
        CategoryJpaEntity entity =
                CategoryJpaEntity.builder()
                        .id(1L)
                        .householdId(null)
                        .name("Moradia")
                        .type(CategoryType.EXPENSE)
                        .build();

        Category domain = mapper.toDomain(entity);

        assertThat(domain.householdId()).isNull();
        assertThat(domain.isGlobal()).isTrue();
    }

    @Test
    void toDomainMapsAnExistingHouseholdToItsId() {
        CategoryJpaEntity entity =
                CategoryJpaEntity.builder()
                        .id(1L)
                        .householdId(10L)
                        .name("Consultoria")
                        .type(CategoryType.INCOME)
                        .build();

        Category domain = mapper.toDomain(entity);

        assertThat(domain.householdId()).isEqualTo(10L);
        assertThat(domain.isGlobal()).isFalse();
    }
}
