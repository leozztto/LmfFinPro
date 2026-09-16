package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.mapper.CategoryPersistenceMapper;
import com.lmf.finpro.infrastructure.persistence.repository.CategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryPersistenceMapper mapper;

    @Override
    public Category save(Category category) {
        return mapper.toDomain(categoryJpaRepository.save(mapper.toEntity(category)));
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Category> findAllVisibleToUser(Long userId) {
        return categoryJpaRepository.findByUserIdOrUserIdIsNull(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        categoryJpaRepository.deleteById(id);
    }
}
