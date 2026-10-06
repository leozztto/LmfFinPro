package com.lmf.finpro.application.household;

import com.lmf.finpro.application.support.SecureTokens;
import com.lmf.finpro.domain.exception.HouseholdPermissionException;
import com.lmf.finpro.domain.exception.HouseholdRuleException;
import com.lmf.finpro.domain.exception.InvalidHouseholdInviteException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdInvite;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.HouseholdInviteMailerPort;
import com.lmf.finpro.domain.port.out.HouseholdInviteRepositoryPort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grupos compartilhados (casal/família): criação, convites e gestão dos membros. O espaço pessoal
 * de cada usuário (criado no cadastro) é um grupo de um membro só e não aceita convites.
 *
 * <p>Só o dono convida, remove membros e transfere a posse. O dono não sai do grupo sem antes
 * transferir a posse, para o grupo (e os dados nele) nunca ficar sem responsável.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HouseholdApplicationService {

    private static final String INVALID_INVITE_MESSAGE =
            "Convite inválido ou expirado. Peça um novo ao dono do grupo.";

    private final HouseholdRepositoryPort householdRepositoryPort;
    private final HouseholdInviteRepositoryPort inviteRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final HouseholdInviteMailerPort mailerPort;
    private final HouseholdInviteSettings settings;

    @Transactional(readOnly = true)
    public List<HouseholdSummary> listMine(Long currentUserId) {
        return householdRepositoryPort.findMembershipsByUserId(currentUserId).stream()
                .map(membership -> summaryOf(membership))
                .toList();
    }

    @Transactional
    public HouseholdSummary createShared(Long currentUserId, String name) {
        Household household = householdRepositoryPort.save(Household.shared(name.trim()));
        HouseholdMembership owner =
                householdRepositoryPort.saveMembership(
                        new HouseholdMembership(
                                household.id(), currentUserId, HouseholdRole.OWNER));
        return new HouseholdSummary(
                household.id(), household.name(), household.type(), owner.role());
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberView> listMembers(Long currentUserId, Long householdId) {
        requireMember(currentUserId, householdId);
        return householdRepositoryPort.findMembershipsByHouseholdId(householdId).stream()
                .map(this::memberViewOf)
                .toList();
    }

    /** Os convites pendentes endereçados ao e-mail da conta de quem consulta. */
    @Transactional(readOnly = true)
    public List<ReceivedInviteView> listReceivedInvites(Long currentUserId) {
        User user = requireUser(currentUserId);
        return inviteRepositoryPort.findPendingByEmail(user.email(), LocalDateTime.now()).stream()
                .map(this::receivedViewOf)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    /**
     * Aceita, dentro do app, um convite endereçado ao e-mail da conta. Aqui o e-mail precisa
     * coincidir (diferente do link, que é o próprio segredo): o id do convite é fácil de adivinhar,
     * então sem essa checagem qualquer pessoa entraria em qualquer grupo.
     */
    @Transactional
    public HouseholdSummary acceptReceivedInvite(Long currentUserId, Long inviteId) {
        LocalDateTime now = LocalDateTime.now();
        HouseholdInvite invite = findReceivedOrThrow(currentUserId, inviteId);
        if (!invite.isUsable(now)) {
            throw new InvalidHouseholdInviteException(INVALID_INVITE_MESSAGE);
        }
        return join(currentUserId, invite, now);
    }

    /** Recusa um convite recebido: ele é apagado e o link do e-mail deixa de funcionar. */
    @Transactional
    public void declineReceivedInvite(Long currentUserId, Long inviteId) {
        HouseholdInvite invite = findReceivedOrThrow(currentUserId, inviteId);
        inviteRepositoryPort.deleteById(invite.id());
    }

    /** Convite que não é endereçado a este e-mail é tratado como inexistente, sem revelar nada. */
    private HouseholdInvite findReceivedOrThrow(Long currentUserId, Long inviteId) {
        User user = requireUser(currentUserId);
        return inviteRepositoryPort
                .findById(inviteId)
                .filter(found -> found.email().equalsIgnoreCase(user.email()))
                .orElseThrow(() -> new InvalidHouseholdInviteException(INVALID_INVITE_MESSAGE));
    }

    private User requireUser(Long userId) {
        return userRepositoryPort
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private java.util.Optional<ReceivedInviteView> receivedViewOf(HouseholdInvite invite) {
        return householdRepositoryPort
                .findById(invite.householdId())
                .filter(Household::isShared)
                .map(
                        household ->
                                new ReceivedInviteView(
                                        invite.id(),
                                        household.id(),
                                        household.name(),
                                        userRepositoryPort
                                                .findById(invite.createdBy())
                                                .map(User::name)
                                                .orElse("Alguém"),
                                        invite.expiresAt()));
    }

    /** Convida por e-mail; um novo convite para o mesmo e-mail substitui os pendentes. */
    @Transactional
    public HouseholdInvite invite(Long currentUserId, Long householdId, String email) {
        Household household = requireSharedOwner(currentUserId, householdId);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        userRepositoryPort
                .findByEmail(normalizedEmail)
                .flatMap(user -> householdRepositoryPort.findMembership(householdId, user.id()))
                .ifPresent(
                        member -> {
                            throw new HouseholdRuleException("Esta pessoa já participa do grupo");
                        });

        LocalDateTime now = LocalDateTime.now();
        inviteRepositoryPort.deletePendingByHouseholdIdAndEmail(householdId, normalizedEmail, now);

        String rawToken = SecureTokens.generate();
        HouseholdInvite invite =
                inviteRepositoryPort.save(
                        HouseholdInvite.issue(
                                householdId,
                                normalizedEmail,
                                SecureTokens.sha256(rawToken),
                                currentUserId,
                                now,
                                settings.ttlDays()));

        String inviterName =
                userRepositoryPort.findById(currentUserId).map(User::name).orElse("Alguém");
        // Se o envio falhar a exceção desfaz o convite: quem convidou fica sabendo e tenta de novo.
        mailerPort.sendInvite(
                normalizedEmail,
                inviterName,
                household.name(),
                settings.invitePageUrl() + "?token=" + rawToken,
                settings.ttlDays());
        return invite;
    }

    @Transactional(readOnly = true)
    public List<HouseholdInvite> listPendingInvites(Long currentUserId, Long householdId) {
        requireSharedOwner(currentUserId, householdId);
        return inviteRepositoryPort.findPendingByHouseholdId(householdId, LocalDateTime.now());
    }

    @Transactional
    public void revokeInvite(Long currentUserId, Long householdId, Long inviteId) {
        requireSharedOwner(currentUserId, householdId);
        HouseholdInvite invite =
                inviteRepositoryPort
                        .findById(inviteId)
                        .filter(found -> found.householdId().equals(householdId))
                        .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));
        inviteRepositoryPort.deleteById(invite.id());
    }

    /**
     * Quem tem o link entra no grupo, com qualquer e-mail. Funciona para quem já tem conta e mantém
     * o espaço pessoal intacto: nada é movido nem apagado.
     */
    @Transactional
    public HouseholdSummary acceptInvite(Long currentUserId, String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        HouseholdInvite invite =
                inviteRepositoryPort
                        .findByTokenHash(SecureTokens.sha256(rawToken))
                        .filter(found -> found.isUsable(now))
                        .orElseThrow(
                                () -> new InvalidHouseholdInviteException(INVALID_INVITE_MESSAGE));
        return join(currentUserId, invite, now);
    }

    /** Entra no grupo do convite (sem duplicar o vínculo) e o consome. */
    private HouseholdSummary join(Long currentUserId, HouseholdInvite invite, LocalDateTime now) {
        Household household =
                householdRepositoryPort
                        .findById(invite.householdId())
                        .filter(Household::isShared)
                        .orElseThrow(
                                () -> new InvalidHouseholdInviteException(INVALID_INVITE_MESSAGE));

        HouseholdMembership membership =
                householdRepositoryPort
                        .findMembership(household.id(), currentUserId)
                        .orElseGet(
                                () ->
                                        householdRepositoryPort.saveMembership(
                                                new HouseholdMembership(
                                                        household.id(),
                                                        currentUserId,
                                                        invite.role())));
        inviteRepositoryPort.save(invite.markAccepted(now));
        log.info("Convite aceito householdId={} userId={}", household.id(), currentUserId);
        return new HouseholdSummary(
                household.id(), household.name(), household.type(), membership.role());
    }

    @Transactional
    public void removeMember(Long currentUserId, Long householdId, Long memberUserId) {
        requireSharedOwner(currentUserId, householdId);
        if (currentUserId.equals(memberUserId)) {
            throw new HouseholdRuleException(
                    "O dono não pode se remover: transfira a posse do grupo antes");
        }
        householdRepositoryPort
                .findMembership(householdId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado no grupo"));
        householdRepositoryPort.deleteMembership(householdId, memberUserId);
    }

    @Transactional
    public void leave(Long currentUserId, Long householdId) {
        HouseholdMembership membership = requireMember(currentUserId, householdId);
        Household household = requireHousehold(householdId);
        if (!household.isShared()) {
            throw new HouseholdRuleException("O espaço pessoal não pode ser abandonado");
        }
        if (membership.isOwner()) {
            throw new HouseholdRuleException(
                    "O dono não pode sair do grupo: transfira a posse antes");
        }
        householdRepositoryPort.deleteMembership(householdId, currentUserId);
    }

    @Transactional
    public void transferOwnership(Long currentUserId, Long householdId, Long newOwnerUserId) {
        requireSharedOwner(currentUserId, householdId);
        if (currentUserId.equals(newOwnerUserId)) {
            throw new HouseholdRuleException("Você já é o dono do grupo");
        }
        householdRepositoryPort
                .findMembership(householdId, newOwnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado no grupo"));
        householdRepositoryPort.saveMembership(
                new HouseholdMembership(householdId, newOwnerUserId, HouseholdRole.OWNER));
        householdRepositoryPort.saveMembership(
                new HouseholdMembership(householdId, currentUserId, HouseholdRole.MEMBER));
    }

    private HouseholdMembership requireMember(Long userId, Long householdId) {
        return householdRepositoryPort
                .findMembership(householdId, userId)
                .orElseThrow(
                        () -> new HouseholdPermissionException("Você não participa deste grupo"));
    }

    private Household requireHousehold(Long householdId) {
        return householdRepositoryPort
                .findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado"));
    }

    /** Membro dono de um grupo compartilhado: quem pode convidar, remover e transferir a posse. */
    private Household requireSharedOwner(Long userId, Long householdId) {
        HouseholdMembership membership = requireMember(userId, householdId);
        Household household = requireHousehold(householdId);
        if (!household.isShared()) {
            throw new HouseholdRuleException(
                    "O espaço pessoal não aceita outros membros: crie um grupo compartilhado");
        }
        if (!membership.isOwner()) {
            throw new HouseholdPermissionException("Apenas o dono do grupo pode fazer isso");
        }
        return household;
    }

    private HouseholdSummary summaryOf(HouseholdMembership membership) {
        Household household = requireHousehold(membership.householdId());
        return new HouseholdSummary(
                household.id(), household.name(), household.type(), membership.role());
    }

    private HouseholdMemberView memberViewOf(HouseholdMembership membership) {
        User user =
                userRepositoryPort
                        .findById(membership.userId())
                        .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return new HouseholdMemberView(user.id(), user.name(), user.email(), membership.role());
    }
}
