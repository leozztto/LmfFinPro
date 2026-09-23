package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientReceiptData;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.ReceiptGeneratorPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportApplicationService {

    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ReceiptGeneratorPort receiptGeneratorPort;

    public byte[] generateClientReceipt(
            Long currentUserId, Long clientId, YearMonth referenceMonth) {
        Client client = findOwnedClientOrThrow(currentUserId, clientId);
        User issuer =
                userRepositoryPort
                        .findById(currentUserId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Usuário não encontrado: " + currentUserId));

        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);
        List<Transaction> transactions =
                transactionRepositoryPort.findAllByClientIdAndTypeAndDateBetween(
                        clientId, CategoryType.INCOME, start, end);

        BigDecimal total =
                transactions.stream()
                        .map(Transaction::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return receiptGeneratorPort.generateClientReceipt(
                new ClientReceiptData(issuer, client, referenceMonth, transactions, total));
    }

    private Client findOwnedClientOrThrow(Long currentUserId, Long clientId) {
        return clientRepositoryPort
                .findById(clientId)
                .filter(candidate -> candidate.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
    }
}
