package com.lmf.finpro.application.transfer;

import com.lmf.finpro.application.account.AccountApplicationService;
import com.lmf.finpro.application.attachment.TransactionAttachmentApplicationService;
import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.domain.exception.HouseholdPermissionException;
import com.lmf.finpro.domain.exception.InsufficientBalanceException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.SameAccountTransferException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecordAuthorship;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.RecordAuthorshipPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransferRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferApplicationService {

    private final TransferRepositoryPort transferRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final AccountApplicationService accountApplicationService;
    private final TransactionAttachmentApplicationService transactionAttachmentApplicationService;
    private final ExchangeRateApplicationService exchangeRateApplicationService;
    private final RecordAuthorshipPort recordAuthorshipPort;
    private final HouseholdRepositoryPort householdRepositoryPort;

    public TransferResult create(
            Long currentHouseholdId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            LocalDate transferDate,
            String description) {
        return create(
                currentHouseholdId,
                fromAccountId,
                toAccountId,
                amount,
                transferDate,
                description,
                null);
    }

    /**
     * Entre contas de moedas diferentes, {@code receivedAmount} é o valor que entra no destino, na
     * moeda dele (o câmbio efetivo, com spread e taxas); entre contas da mesma moeda é ignorado. As
     * duas pernas recebem o mesmo valor em reais, para continuarem se anulando no consolidado.
     *
     * <p>Sem autor conhecido (uso do sistema, como o aporte automático de uma meta): qualquer
     * membro do grupo pode excluir depois.
     */
    public TransferResult create(
            Long currentHouseholdId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            LocalDate transferDate,
            String description,
            BigDecimal receivedAmount) {
        return create(
                currentHouseholdId,
                null,
                fromAccountId,
                toAccountId,
                amount,
                transferDate,
                description,
                receivedAmount);
    }

    /**
     * {@code currentUserId} fica gravado como autor: numa conta compartilhada, só ele poderá
     * excluir.
     */
    @Transactional
    public TransferResult create(
            Long currentHouseholdId,
            Long currentUserId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            LocalDate transferDate,
            String description,
            BigDecimal receivedAmount) {
        log.debug(
                "Criando transferência da conta={} para a conta={} do usuário={}",
                fromAccountId,
                toAccountId,
                currentHouseholdId);
        if (fromAccountId.equals(toAccountId)) {
            throw new SameAccountTransferException(
                    "A conta de origem e destino não podem ser a mesma.");
        }

        Account fromAccount =
                findReachableOrThrow(currentHouseholdId, currentUserId, fromAccountId);
        Account toAccount = findReachableOrThrow(currentHouseholdId, currentUserId, toAccountId);
        if (!fromAccount.belongsTo(currentHouseholdId)
                && !toAccount.belongsTo(currentHouseholdId)) {
            // Ao menos uma ponta é do espaço de quem transfere; duas contas de fora não valem.
            throw new ResourceNotFoundException("Conta não encontrada: " + fromAccountId);
        }
        boolean crossCurrency = fromAccount.currency() != toAccount.currency();
        if (crossCurrency && (receivedAmount == null || receivedAmount.signum() <= 0)) {
            throw new IllegalArgumentException(
                    "As contas têm moedas diferentes: informe o valor recebido em "
                            + toAccount.currency()
                            + ".");
        }
        BigDecimal creditedAmount = crossCurrency ? receivedAmount : amount;
        BigDecimal baseAmount =
                baseAmount(fromAccount, toAccount, amount, creditedAmount, transferDate);

        BigDecimal fromAccountBalance =
                accountApplicationService.calculateCurrentBalance(fromAccount);
        if (amount.compareTo(fromAccountBalance) > 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente na conta de origem. Saldo disponível: "
                            + fromAccountBalance.setScale(2, RoundingMode.HALF_UP));
        }

        Transfer saved =
                transferRepositoryPort.save(
                        Transfer.create(
                                currentHouseholdId,
                                fromAccountId,
                                toAccountId,
                                amount,
                                crossCurrency ? receivedAmount : null,
                                transferDate,
                                description));

        // Transferência entre espaços (pessoal x grupo): cada espaço fica com a sua perna, ligada a
        // uma transferência própria — o mesmo modelo que o compartilhamento de conta produz.
        boolean crossSpace = !fromAccount.householdId().equals(toAccount.householdId());
        Transfer mirror = null;
        if (crossSpace) {
            Long otherHouseholdId =
                    fromAccount.belongsTo(currentHouseholdId)
                            ? toAccount.householdId()
                            : fromAccount.householdId();
            mirror =
                    transferRepositoryPort.save(
                            Transfer.create(
                                    otherHouseholdId,
                                    fromAccountId,
                                    toAccountId,
                                    amount,
                                    crossCurrency ? receivedAmount : null,
                                    transferDate,
                                    description));
        }
        Long fromTransferId =
                mirror == null || fromAccount.belongsTo(currentHouseholdId)
                        ? saved.id()
                        : mirror.id();
        Long toTransferId =
                mirror == null || toAccount.belongsTo(currentHouseholdId)
                        ? saved.id()
                        : mirror.id();

        boolean hasCustomDescription = description != null && !description.isBlank();
        String outDescription =
                hasCustomDescription ? description : "Transferência para " + toAccount.name();
        String inDescription =
                hasCustomDescription ? description : "Transferência de " + fromAccount.name();

        Transaction fromTransaction =
                transactionRepositoryPort.save(
                        Transaction.createForTransfer(
                                        fromAccountId,
                                        outDescription,
                                        amount,
                                        transferDate,
                                        CategoryType.EXPENSE,
                                        fromTransferId)
                                .withBaseAmount(baseAmount));
        Transaction toTransaction =
                transactionRepositoryPort.save(
                        Transaction.createForTransfer(
                                        toAccountId,
                                        inDescription,
                                        creditedAmount,
                                        transferDate,
                                        CategoryType.INCOME,
                                        toTransferId)
                                .withBaseAmount(baseAmount));

        if (currentUserId != null) {
            recordAuthorshipPort.recordTransferAuthor(saved.id(), currentUserId);
            if (mirror != null) {
                recordAuthorshipPort.recordTransferAuthor(mirror.id(), currentUserId);
            }
            recordAuthorshipPort.recordTransactionAuthors(
                    List.of(fromTransaction.id(), toTransaction.id()), currentUserId);
        }
        log.debug("Transferência={} criada para o usuário={}", saved.id(), currentHouseholdId);
        return new TransferResult(
                saved,
                fromTransaction.id(),
                toTransaction.id(),
                fromAccount.name(),
                toAccount.name());
    }

    public List<TransferResult> list(Long currentHouseholdId) {
        log.debug("Listando transferências do usuário={}", currentHouseholdId);
        List<Transfer> transfers = transferRepositoryPort.findAllByHouseholdId(currentHouseholdId);
        List<Long> transferIds = transfers.stream().map(Transfer::id).toList();
        Map<Long, List<Transaction>> legsByTransferId =
                transactionRepositoryPort.findAllByTransferIds(transferIds).stream()
                        .collect(Collectors.groupingBy(Transaction::transferId));

        Map<Long, String> accountNames = accountNames(transfers);
        return transfers.stream()
                .map(
                        transfer ->
                                toResult(
                                        transfer,
                                        legsByTransferId.getOrDefault(transfer.id(), List.of()),
                                        accountNames))
                .toList();
    }

    public void delete(Long currentHouseholdId, Long currentUserId, Long transferId) {
        log.debug("Removendo transferência={} do usuário={}", transferId, currentHouseholdId);
        Transfer transfer =
                transferRepositoryPort
                        .findById(transferId)
                        .filter(t -> t.belongsTo(currentHouseholdId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transferência não encontrada: " + transferId));
        // Numa conta compartilhada só quem criou a transferência pode excluí-la.
        Long author = recordAuthorshipPort.findTransferAuthor(transferId).orElse(null);
        if (!RecordAuthorship.canDelete(author, currentUserId)) {
            throw new HouseholdPermissionException(
                    "Só quem criou esta transferência pode excluí-la.");
        }
        // As duas transações da transferência saem em cascata no banco, com os registros dos
        // anexos; os arquivos dos anexos precisam ser apagados do disco à parte.
        List<Long> legIds =
                transactionRepositoryPort.findAllByTransferIds(List.of(transfer.id())).stream()
                        .map(Transaction::id)
                        .toList();
        List<String> attachmentKeys = transactionAttachmentApplicationService.storageKeysOf(legIds);
        transferRepositoryPort.deleteById(transfer.id());
        transactionAttachmentApplicationService.deleteStoredFiles(attachmentKeys);
    }

    /**
     * Valor em reais da transferência: o lado em reais, se houver (o câmbio efetivo); senão, a
     * conversão do valor enviado pela cotação do dia.
     */
    private BigDecimal baseAmount(
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            BigDecimal creditedAmount,
            LocalDate transferDate) {
        if (fromAccount.currency().isBase()) {
            return amount;
        }
        if (toAccount.currency().isBase()) {
            return creditedAmount;
        }
        return exchangeRateApplicationService.toBrl(fromAccount.currency(), amount, transferDate);
    }

    /**
     * Nome das contas de cada ponta, inclusive a que está em outro espaço (pessoal x grupo): uma
     * transferência que cruza a fronteira só tem uma conta no espaço de quem consulta, e a tela
     * precisa dizer de onde o dinheiro veio. Só o nome sai daqui, nenhum outro dado da conta.
     */
    private Map<Long, String> accountNames(List<Transfer> transfers) {
        return transfers.stream()
                .flatMap(
                        transfer ->
                                java.util.stream.Stream.of(
                                        transfer.fromAccountId(), transfer.toAccountId()))
                .distinct()
                .map(accountRepositoryPort::findById)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(Account::id, Account::name));
    }

    private TransferResult toResult(
            Transfer transfer, List<Transaction> legs, Map<Long, String> accountNames) {
        Long fromTransactionId =
                legs.stream()
                        .filter(
                                t ->
                                        t.accountId().equals(transfer.fromAccountId())
                                                && t.type() == CategoryType.EXPENSE)
                        .map(Transaction::id)
                        .findFirst()
                        .orElse(null);
        Long toTransactionId =
                legs.stream()
                        .filter(
                                t ->
                                        t.accountId().equals(transfer.toAccountId())
                                                && t.type() == CategoryType.INCOME)
                        .map(Transaction::id)
                        .findFirst()
                        .orElse(null);
        return new TransferResult(
                transfer,
                fromTransactionId,
                toTransactionId,
                accountNames.get(transfer.fromAccountId()),
                accountNames.get(transfer.toAccountId()));
    }

    /**
     * Contas dos outros espaços dos quais a pessoa participa (o pessoal e os grupos), com as quais
     * ela pode transferir a partir do espaço atual. Só id, nome, tipo e moeda saem daqui.
     */
    public List<LinkableAccount> linkableAccounts(Long currentHouseholdId, Long currentUserId) {
        return householdRepositoryPort.findMembershipsByUserId(currentUserId).stream()
                .filter(membership -> !membership.householdId().equals(currentHouseholdId))
                .flatMap(
                        membership -> {
                            String householdName =
                                    householdRepositoryPort
                                            .findById(membership.householdId())
                                            .map(household -> household.name())
                                            .orElse("");
                            return accountRepositoryPort
                                    .findAllByHouseholdId(membership.householdId())
                                    .stream()
                                    .map(account -> new LinkableAccount(account, householdName));
                        })
                .toList();
    }

    /**
     * Conta do espaço atual ou de outro espaço do qual a pessoa participa. Qualquer outra é tratada
     * como inexistente (404).
     */
    private Account findReachableOrThrow(
            Long currentHouseholdId, Long currentUserId, Long accountId) {
        Account account =
                accountRepositoryPort
                        .findById(accountId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Conta não encontrada: " + accountId));
        boolean reachable =
                account.belongsTo(currentHouseholdId)
                        || (currentUserId != null
                                && householdRepositoryPort
                                        .findMembership(account.householdId(), currentUserId)
                                        .isPresent());
        if (!reachable) {
            throw new ResourceNotFoundException("Conta não encontrada: " + accountId);
        }
        return account;
    }
}
