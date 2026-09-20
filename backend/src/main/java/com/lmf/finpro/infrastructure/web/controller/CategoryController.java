package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.category.CategoryApplicationService;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryRequest;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import com.lmf.finpro.infrastructure.web.mapper.CategoryWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryApplicationService categoryApplicationService;
    private final CategoryWebMapper mapper;

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return categoryApplicationService.list(currentUser.userId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return mapper.toResponse(categoryApplicationService.getById(currentUser.userId(), id));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CategoryRequest request) {
        Category created =
                categoryApplicationService.create(
                        currentUser.userId(),
                        request.name(),
                        request.type(),
                        request.color(),
                        request.icon());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        Category updated =
                categoryApplicationService.update(
                        currentUser.userId(), id, request.name(), request.color(), request.icon());
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        categoryApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
