package com.lmf.finpro.application.support;

import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * O que, entre as pernas de transferência, é receita ou despesa de verdade para quem consulta.
 * Transferir entre contas do mesmo espaço só reorganiza o dinheiro e fica de fora; já uma
 * transferência com uma conta de outro espaço (pessoal x grupo) é dinheiro que entrou ou saiu
 * daqui: a entrada conta como receita e a saída como despesa.
 */
@Component
@RequiredArgsConstructor
public class TransferFlow {

    private final TransferRepositoryPort transferRepositoryPort;

    /** Fora as pernas de transferências entre contas do mesmo espaço; o resto permanece. */
    public List<Transaction> withoutInternalTransfers(List<Transaction> transactions) {
        Set<Long> transferIds =
                transactions.stream()
                        .map(Transaction::transferId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet());
        if (transferIds.isEmpty()) {
            return transactions;
        }
        Set<Long> crossSpace = transferRepositoryPort.findCrossSpaceIds(transferIds);
        return transactions.stream()
                .filter(
                        transaction ->
                                transaction.transferId() == null
                                        || crossSpace.contains(transaction.transferId()))
                .toList();
    }
}
