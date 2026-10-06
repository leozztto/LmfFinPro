package com.lmf.finpro.infrastructure.persistence.repository;

import com.lmf.finpro.infrastructure.persistence.entity.TagJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagJpaRepository extends JpaRepository<TagJpaEntity, Long> {

    List<TagJpaEntity> findByHouseholdIdOrderByNameAsc(Long householdId);

    List<TagJpaEntity> findByHouseholdIdAndNameIn(Long householdId, Collection<String> names);
}
