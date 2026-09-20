package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.client.ClientApplicationService;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.client.ClientRequest;
import com.lmf.finpro.infrastructure.web.dto.client.ClientResponse;
import com.lmf.finpro.infrastructure.web.mapper.ClientWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientApplicationService clientApplicationService;
    private final ClientWebMapper mapper;

    @GetMapping
    public List<ClientResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return clientApplicationService.list(currentUser.userId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ClientResponse getById(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return mapper.toResponse(clientApplicationService.getById(currentUser.userId(), id));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ClientRequest request) {
        Client created =
                clientApplicationService.create(
                        currentUser.userId(),
                        request.name(),
                        request.email(),
                        request.phone(),
                        request.documentType(),
                        request.documentNumber(),
                        request.workType(),
                        request.notes(),
                        request.color(),
                        request.active());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @PutMapping("/{id}")
    public ClientResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request) {
        Client updated =
                clientApplicationService.update(
                        currentUser.userId(),
                        id,
                        request.name(),
                        request.email(),
                        request.phone(),
                        request.documentType(),
                        request.documentNumber(),
                        request.workType(),
                        request.notes(),
                        request.color(),
                        request.active());
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        clientApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
