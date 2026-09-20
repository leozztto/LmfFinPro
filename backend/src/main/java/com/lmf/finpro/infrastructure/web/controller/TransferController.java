package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.transfer.TransferApplicationService;
import com.lmf.finpro.application.transfer.TransferResult;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.infrastructure.web.mapper.TransferWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferApplicationService transferApplicationService;
    private final TransferWebMapper mapper;

    @GetMapping
    public List<TransferResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return transferApplicationService.list(currentUser.userId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TransferRequest request) {
        TransferResult created =
                transferApplicationService.create(
                        currentUser.userId(),
                        request.fromAccountId(),
                        request.toAccountId(),
                        request.amount(),
                        request.transferDate(),
                        request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        transferApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
