package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.support.RecordAuthor;
import com.lmf.finpro.application.support.RecordAuthorshipApplicationService;
import com.lmf.finpro.application.transfer.TransferApplicationService;
import com.lmf.finpro.application.transfer.TransferResult;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.transfer.LinkableAccountResponse;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferRequest;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import com.lmf.finpro.infrastructure.web.mapper.TransferWebMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
    private final RecordAuthorshipApplicationService recordAuthorshipApplicationService;

    @GetMapping
    public List<TransferResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<TransferResult> transfers = transferApplicationService.list(currentUser.householdId());
        Map<Long, RecordAuthor> authors =
                recordAuthorshipApplicationService.authorsOfTransfers(
                        transfers.stream().map(result -> result.transfer().id()).toList());
        return transfers.stream()
                .map(result -> mapper.toResponse(result, authors.get(result.transfer().id())))
                .toList();
    }

    /**
     * Contas dos outros espaços da pessoa (pessoal e grupos) que ela pode usar numa transferência.
     */
    @GetMapping("/linkable-accounts")
    public List<LinkableAccountResponse> linkableAccounts(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return transferApplicationService
                .linkableAccounts(currentUser.householdId(), currentUser.userId())
                .stream()
                .map(
                        linkable ->
                                new LinkableAccountResponse(
                                        linkable.account().id(),
                                        linkable.account().name(),
                                        linkable.account().type(),
                                        linkable.account().currency(),
                                        linkable.householdName()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody TransferRequest request) {
        TransferResult created =
                transferApplicationService.create(
                        currentUser.householdId(),
                        currentUser.userId(),
                        request.fromAccountId(),
                        request.toAccountId(),
                        request.amount(),
                        request.transferDate(),
                        request.description(),
                        request.receivedAmount());
        RecordAuthor author =
                recordAuthorshipApplicationService
                        .authorsOfTransfers(List.of(created.transfer().id()))
                        .get(created.transfer().id());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created, author));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        transferApplicationService.delete(currentUser.householdId(), currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
