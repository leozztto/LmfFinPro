package com.lmf.finpro.application.transfer;

import com.lmf.finpro.application.account.AccountApplicationService;
import com.lmf.finpro.domain.exception.InsufficientBalanceException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.SameAccountTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferApplicationService {

    private final TransferRepositoryPort transferRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountApplicationService accountApplicationService;

    @Transactional
    public TransferResult create(
        Long currentUserId, Long fromAccountId, Long toAccountId,
        BigDecimal amount, LocalDate transferDate, String description
    ) {
        if (fromAccountId.equals(toAccountId)) {
            throw new SameAccountTransferException("A conta de origem e destino não podem ser a mesma.");
        }

        Account fromAccount = findOwnedOrThrow(currentUserId, fromAccountId);
        Account toAccount = findOwnedOrThrow(currentUserId, toAccountId);

        BigDecimal fromAccountBalance = accountApplicationService.calculateCurrentBalance(fromAccount);
        if (amount.compareTo(fromAccountBalance) > 0) {
            throw new InsufficientBalanceException(
                "Saldo insuficiente na conta de origem. Saldo disponível: " + fromAccountBalance.setScale(2, RoundingMode.HALF_UP)
            );
        }

        Transfer saved = transferRepositoryPort.save(
            Transfer.create(currentUserId, fromAccountId, toAccountId, amount, transferDate, description)
        );

        boolean hasCustomDescription = description != null && !description.isBlank();
        String outDescription = hasCustomDescription ? description : "Transferência para " + toAccount.name();
        String inDescription = hasCustomDescription ? description : "Transferência de " + fromAccount.name();

        Transaction fromTransaction = transactionRepositoryPort.save(
            Transaction.createForTransfer(fromAccountId, outDescription, amount, transferDate, CategoryType.EXPENSE, saved.id())
        );
        Transaction toTransaction = transactionRepositoryPort.save(
            Transaction.createForTransfer(toAccountId, inDescription, amount, transferDate, CategoryType.INCOME, saved.id())
        );

        return new TransferResult(saved, fromTransaction.id(), toTransaction.id());
    }

    public List<TransferResult> list(Long currentUserId) {
        List<Transfer> transfers = transferRepositoryPort.findAllByUserId(currentUserId);
        List<Long> transferIds = transfers.stream().map(Transfer::id).toList();
        Map<Long, List<Transaction>> legsByTransferId = transactionRepositoryPort.findAllByTransferIds(transferIds).stream()
            .collect(Collectors.groupingBy(Transaction::transferId));

        return transfers.stream()
            .map(transfer -> toResult(transfer, legsByTransferId.getOrDefault(transfer.id(), List.of())))
            .toList();
    }

    public void delete(Long currentUserId, Long transferId) {
        Transfer transfer = transferRepositoryPort.findById(transferId)
            .filter(t -> t.belongsTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Transferência não encontrada: " + transferId));
        transferRepositoryPort.deleteById(transfer.id());
    }

    private TransferResult toResult(Transfer transfer, List<Transaction> legs) {
        Long fromTransactionId = legs.stream()
            .filter(t -> t.accountId().equals(transfer.fromAccountId()) && t.type() == CategoryType.EXPENSE)
            .map(Transaction::id)
            .findFirst()
            .orElse(null);
        Long toTransactionId = legs.stream()
            .filter(t -> t.accountId().equals(transfer.toAccountId()) && t.type() == CategoryType.INCOME)
            .map(Transaction::id)
            .findFirst()
            .orElse(null);
        return new TransferResult(transfer, fromTransactionId, toTransactionId);
    }

    /** Acesso a conta de outro usuário é tratado como inexistente (404), não como 403 — mesmo padrão de AccountApplicationService/TransactionApplicationService. */
    private Account findOwnedOrThrow(Long currentUserId, Long accountId) {
        return accountRepositoryPort.findById(accountId)
            .filter(account -> account.belongsTo(currentUserId))
            .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + accountId));
    }
}
