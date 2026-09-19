package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.importbatch.ImportApplicationService;
import com.lmf.finpro.domain.exception.ImportFileInvalidException;
import com.lmf.finpro.domain.model.ImportBatch;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.importbatch.ImportBatchResponse;
import com.lmf.finpro.infrastructure.web.dto.importbatch.TransactionReviewRequest;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import com.lmf.finpro.infrastructure.web.mapper.ImportBatchWebMapper;
import com.lmf.finpro.infrastructure.web.mapper.TransactionWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/import-batches")
@RequiredArgsConstructor
public class ImportBatchController {

    private final ImportApplicationService importApplicationService;
    private final ImportBatchWebMapper mapper;
    private final TransactionWebMapper transactionMapper;

    @GetMapping
    public List<ImportBatchResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return importApplicationService.list(currentUser.userId()).stream()
            .map(batch -> mapper.toResponse(batch, importApplicationService.listTransactions(currentUser.userId(), batch.id())))
            .toList();
    }

    @GetMapping("/{id}")
    public ImportBatchResponse getById(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        ImportBatch batch = importApplicationService.getById(currentUser.userId(), id);
        return mapper.toResponse(batch, importApplicationService.listTransactions(currentUser.userId(), id));
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionResponse> listTransactions(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return importApplicationService.listTransactions(currentUser.userId(), id).stream()
            .map(transactionMapper::toResponse)
            .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchResponse> upload(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @RequestParam("accountId") Long accountId,
        @RequestParam("file") MultipartFile file
    ) {
        try (InputStream content = file.getInputStream()) {
            ImportBatch batch = importApplicationService.importCsv(currentUser.userId(), accountId, file.getOriginalFilename(), content);
            List<Transaction> transactions = importApplicationService.listTransactions(currentUser.userId(), batch.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(batch, transactions));
        } catch (IOException e) {
            throw new ImportFileInvalidException("Não foi possível ler o arquivo enviado.");
        }
    }

    @PutMapping("/{batchId}/transactions/{transactionId}")
    public TransactionResponse reviewTransaction(
        @AuthenticationPrincipal AuthenticatedUser currentUser,
        @PathVariable Long batchId,
        @PathVariable Long transactionId,
        @RequestBody TransactionReviewRequest request
    ) {
        Transaction updated = importApplicationService.reviewTransaction(
            currentUser.userId(), batchId, transactionId, request.categoryId(), request.clientId()
        );
        return transactionMapper.toResponse(updated);
    }
}
