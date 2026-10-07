package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.CategoryRuleJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRuleJpaRepository extends JpaRepository<CategoryRuleJpaEntity, Long> {

    /**
     * Regras do usuário + regras padrão do sistema (household_id nulo). As do usuário vêm primeiro
     * (mesmo com peso menor): se ele já corrigiu manualmente, isso deve prevalecer sobre a sugestão
     * genérica do sistema. Dentro de cada grupo, as de maior peso vêm primeiro.
     */
    @Query(
            "SELECT r FROM CategoryRuleJpaEntity r WHERE r.householdId = :householdId OR"
                    + " r.householdId IS NULL ORDER BY CASE WHEN r.householdId IS NULL THEN 1 ELSE 0"
                    + " END, r.weight DESC")
    List<CategoryRuleJpaEntity> findVisibleToUserOrderByPriorityDesc(
            @Param("householdId") Long householdId);

    Optional<CategoryRuleJpaEntity> findByHouseholdIdAndPatternIgnoreCase(
            Long householdId, String pattern);
}
