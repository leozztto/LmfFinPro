package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.tag.TagApplicationService;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.tag.TagRequest;
import com.lmf.finpro.infrastructure.web.dto.tag.TagResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Gestão das tags (renomear, cor, excluir). A criação normalmente acontece sozinha, ao digitar uma
 * tag nova numa transação; o POST existe para criar direto pela tela de gestão.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagApplicationService tagApplicationService;

    @GetMapping
    public List<TagResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return tagApplicationService.list(currentUser.userId()).stream()
                .map(usage -> toResponse(usage.tag(), usage.transactionCount()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TagRequest request) {
        Tag created =
                tagApplicationService.create(currentUser.userId(), request.name(), request.color());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created, 0));
    }

    @PutMapping("/{id}")
    public TagResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody TagRequest request) {
        Tag updated =
                tagApplicationService.update(
                        currentUser.userId(), id, request.name(), request.color());
        long count =
                tagApplicationService.list(currentUser.userId()).stream()
                        .filter(usage -> usage.tag().id().equals(id))
                        .mapToLong(TagApplicationService.TagUsage::transactionCount)
                        .findFirst()
                        .orElse(0);
        return toResponse(updated, count);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        tagApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private TagResponse toResponse(Tag tag, long transactionCount) {
        return new TagResponse(tag.id(), tag.name(), tag.color(), transactionCount);
    }
}
