package com.lmf.finpro.application.privacy;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.application.privacy.AccountDeletionPreview.GroupImpact;
import com.lmf.finpro.domain.exception.HouseholdRuleException;
import com.lmf.finpro.domain.exception.IncorrectCurrentPasswordException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountErasurePort;
import com.lmf.finpro.domain.port.out.FileStoragePort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.PasswordHasherPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Exclusão definitiva da conta (LGPD, art. 18, VI).
 *
 * <ul>
 *   <li>O espaço pessoal é apagado com tudo o que há nele.
 *   <li>Grupo compartilhado em que a pessoa é a única participante também é apagado.
 *   <li>Grupo compartilhado com outras pessoas: quem é só membro sai e os dados continuam com o
 *       grupo, sem autor nem dono (as chaves de autoria viram nulas). Quem é o dono precisa antes
 *       transferir a posse; sem isso o grupo ficaria sem responsável, então a exclusão é recusada.
 *   <li>Os arquivos (anexos e foto) saem do armazenamento só depois de o banco confirmar a
 *       exclusão: se a transação falhar, nenhum arquivo é perdido.
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionApplicationService {

    private final UserRepositoryPort userRepositoryPort;
    private final HouseholdRepositoryPort householdRepositoryPort;
    private final AccountErasurePort accountErasurePort;
    private final PasswordHasherPort passwordHasherPort;
    private final FileStoragePort fileStoragePort;

    /** Resultado da análise dos grupos da pessoa, usado pela prévia e pela exclusão. */
    private record Plan(List<GroupImpact> deleted, List<GroupImpact> left, List<String> blockers) {}

    public AccountDeletionPreview preview(Long userId) {
        log.debug("Prévia da exclusão da conta do usuário={}", userId);
        Plan plan = plan(userId);
        List<Long> deletedIds = plan.deleted().stream().map(GroupImpact::householdId).toList();
        return new AccountDeletionPreview(
                plan.deleted(),
                plan.left(),
                plan.blockers(),
                accountErasurePort.findAttachmentKeys(deletedIds).size());
    }

    @Transactional
    public void delete(Long userId, String password) {
        log.debug("Excluindo a conta do usuário={}", userId);
        User user = findUser(userId);
        if (password == null
                || password.isBlank()
                || !passwordHasherPort.matches(password, user.passwordHash())) {
            FlowLog.detail("reason", "wrongPassword");
            throw new IncorrectCurrentPasswordException("Senha incorreta");
        }

        Plan plan = plan(userId);
        if (!plan.blockers().isEmpty()) {
            FlowLog.detail("reason", "blocked");
            throw new HouseholdRuleException(String.join(" ", plan.blockers()));
        }

        List<Long> deletedIds = plan.deleted().stream().map(GroupImpact::householdId).toList();
        List<String> filesToDelete =
                new ArrayList<>(accountErasurePort.findAttachmentKeys(deletedIds));
        if (user.hasPhoto()) {
            filesToDelete.add(user.photoKey());
        }

        accountErasurePort.deleteHouseholds(deletedIds);
        accountErasurePort.deleteInvitesAddressedTo(user.email());
        accountErasurePort.deleteUser(userId);
        accountErasurePort.logDeletion(userId);

        FlowLog.detail("deletedGroups", deletedIds.size());
        FlowLog.detail("leftGroups", plan.left().size());
        FlowLog.detail("files", filesToDelete.size());
        deleteFilesAfterCommit(filesToDelete);
    }

    private Plan plan(Long userId) {
        List<GroupImpact> deleted = new ArrayList<>();
        List<GroupImpact> left = new ArrayList<>();
        List<String> blockers = new ArrayList<>();

        for (HouseholdMembership membership :
                householdRepositoryPort.findMembershipsByUserId(userId)) {
            Household household =
                    householdRepositoryPort
                            .findById(membership.householdId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Grupo não encontrado"));
            int brought = accountErasurePort.countAccountsBroughtBy(userId, household.id());
            GroupImpact impact =
                    new GroupImpact(household.id(), household.name(), household.type(), brought);

            if (!household.isShared()) {
                deleted.add(impact);
                continue;
            }
            boolean hasOthers =
                    householdRepositoryPort.findMembershipsByHouseholdId(household.id()).stream()
                            .anyMatch(other -> !other.userId().equals(userId));
            if (!hasOthers) {
                deleted.add(impact);
            } else if (membership.isOwner()) {
                blockers.add(
                        "Você é o dono do grupo \""
                                + household.name()
                                + "\", que tem outros membros. Transfira a posse a outro membro"
                                + " ou remova os membros antes de excluir a conta.");
            } else {
                left.add(impact);
            }
        }
        return new Plan(deleted, left, blockers);
    }

    private User findUser(Long userId) {
        return userRepositoryPort
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private void deleteFilesAfterCommit(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            keys.forEach(fileStoragePort::delete);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        keys.forEach(fileStoragePort::delete);
                    }
                });
    }
}
