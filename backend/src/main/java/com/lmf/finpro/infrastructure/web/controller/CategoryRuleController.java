package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.categoryrule.CategoryRuleApplicationService;
import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.categoryrule.CategoryRuleRequest;
import com.lmf.finpro.infrastructure.web.dto.categoryrule.CategoryRuleResponse;
import com.lmf.finpro.infrastructure.web.mapper.CategoryRuleWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-rules")
@RequiredArgsConstructor
public class CategoryRuleController {

    private final CategoryRuleApplicationService categoryRuleApplicationService;
    private final CategoryRuleWebMapper mapper;

    @GetMapping
    public List<CategoryRuleResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return categoryRuleApplicationService.list(currentUser.userId()).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<CategoryRuleResponse> create(
        @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody CategoryRuleRequest request
    ) {
        CategoryRule created = categoryRuleApplicationService.create(currentUser.userId(), request.pattern(), request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        categoryRuleApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
