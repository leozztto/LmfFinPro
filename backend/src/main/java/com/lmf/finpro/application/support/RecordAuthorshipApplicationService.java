package com.lmf.finpro.application.support;

import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountOwnershipPort;
import com.lmf.finpro.domain.port.out.RecordAuthorshipPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolve quem criou cada lançamento e transferência de uma lista, para a tela. */
@Service
@RequiredArgsConstructor
public class RecordAuthorshipApplicationService {

    private final RecordAuthorshipPort recordAuthorshipPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AccountOwnershipPort accountOwnershipPort;

    @Transactional(readOnly = true)
    public Map<Long, RecordAuthor> authorsOfTransactions(Collection<Long> transactionIds) {
        return withNames(recordAuthorshipPort.findTransactionAuthors(transactionIds));
    }

    @Transactional(readOnly = true)
    public Map<Long, RecordAuthor> authorsOfTransfers(Collection<Long> transferIds) {
        return withNames(recordAuthorshipPort.findTransferAuthors(transferIds));
    }

    /** O dono de cada conta (quem a criou ou trouxe para o grupo), para a tela. */
    @Transactional(readOnly = true)
    public Map<Long, RecordAuthor> ownersOfAccounts(Collection<Long> accountIds) {
        return withNames(accountOwnershipPort.findOwners(accountIds));
    }

    /** Um usuário que já não existe fica sem nome, mas o registro continua sendo dele. */
    private Map<Long, RecordAuthor> withNames(Map<Long, Long> authorIdByRecordId) {
        Map<Long, String> names = new HashMap<>();
        authorIdByRecordId.values().stream()
                .distinct()
                .forEach(
                        userId ->
                                userRepositoryPort
                                        .findById(userId)
                                        .map(User::name)
                                        .ifPresent(name -> names.put(userId, name)));
        Map<Long, RecordAuthor> result = new HashMap<>();
        authorIdByRecordId.forEach(
                (recordId, userId) ->
                        result.put(recordId, new RecordAuthor(userId, names.get(userId))));
        return result;
    }
}
