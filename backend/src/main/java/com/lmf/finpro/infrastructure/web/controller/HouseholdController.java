package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.household.AccountSharingApplicationService;
import com.lmf.finpro.application.household.HouseholdApplicationService;
import com.lmf.finpro.application.household.HouseholdMemberView;
import com.lmf.finpro.application.household.HouseholdSummary;
import com.lmf.finpro.domain.model.HouseholdInvite;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.household.AcceptInviteRequest;
import com.lmf.finpro.infrastructure.web.dto.household.AccountSharingResponse;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdInviteResponse;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdMemberResponse;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdRequest;
import com.lmf.finpro.infrastructure.web.dto.household.HouseholdResponse;
import com.lmf.finpro.infrastructure.web.dto.household.InviteRequest;
import com.lmf.finpro.infrastructure.web.dto.household.ReceivedInviteResponse;
import com.lmf.finpro.infrastructure.web.dto.household.ShareAccountsRequest;
import com.lmf.finpro.infrastructure.web.dto.household.TransferOwnershipRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Grupos compartilhados (casal/família). Aqui o grupo vem sempre explícito no caminho, e não do
 * header de grupo ativo: gerenciar um grupo não depende de qual deles o usuário está vendo.
 */
@RestController
@RequestMapping("/api/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdApplicationService householdApplicationService;
    private final AccountSharingApplicationService accountSharingApplicationService;

    /** Os grupos de que o usuário participa: o espaço pessoal e os compartilhados. */
    @GetMapping
    public List<HouseholdResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return householdApplicationService.listMine(currentUser.userId()).stream()
                .map(HouseholdController::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<HouseholdResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody HouseholdRequest request) {
        HouseholdSummary created =
                householdApplicationService.createShared(currentUser.userId(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping("/{householdId}/members")
    public List<HouseholdMemberResponse> members(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId) {
        return householdApplicationService.listMembers(currentUser.userId(), householdId).stream()
                .map(HouseholdController::toResponse)
                .toList();
    }

    @PostMapping("/{householdId}/invites")
    public ResponseEntity<HouseholdInviteResponse> invite(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId,
            @Valid @RequestBody InviteRequest request) {
        HouseholdInvite invite =
                householdApplicationService.invite(
                        currentUser.userId(), householdId, request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(invite));
    }

    @GetMapping("/{householdId}/invites")
    public List<HouseholdInviteResponse> pendingInvites(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId) {
        return householdApplicationService
                .listPendingInvites(currentUser.userId(), householdId)
                .stream()
                .map(HouseholdController::toResponse)
                .toList();
    }

    @DeleteMapping("/{householdId}/invites/{inviteId}")
    public ResponseEntity<Void> revokeInvite(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId,
            @PathVariable Long inviteId) {
        householdApplicationService.revokeInvite(currentUser.userId(), householdId, inviteId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Os convites pendentes endereçados ao e-mail da conta: aceitar ou recusar sem precisar do
     * link.
     */
    @GetMapping("/invites/received")
    public List<ReceivedInviteResponse> receivedInvites(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return householdApplicationService.listReceivedInvites(currentUser.userId()).stream()
                .map(
                        invite ->
                                new ReceivedInviteResponse(
                                        invite.inviteId(),
                                        invite.householdId(),
                                        invite.householdName(),
                                        invite.inviterName(),
                                        invite.expiresAt()))
                .toList();
    }

    @PostMapping("/invites/{inviteId}/accept")
    public HouseholdResponse acceptReceivedInvite(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long inviteId) {
        return toResponse(
                householdApplicationService.acceptReceivedInvite(currentUser.userId(), inviteId));
    }

    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<Void> declineReceivedInvite(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long inviteId) {
        householdApplicationService.declineReceivedInvite(currentUser.userId(), inviteId);
        return ResponseEntity.noContent().build();
    }

    /** O usuário logado aceita o convite cujo token veio no link recebido por e-mail. */
    @PostMapping("/invites/accept")
    public HouseholdResponse acceptInvite(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AcceptInviteRequest request) {
        return toResponse(
                householdApplicationService.acceptInvite(currentUser.userId(), request.token()));
    }

    @DeleteMapping("/{householdId}/members/{memberUserId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId,
            @PathVariable Long memberUserId) {
        householdApplicationService.removeMember(currentUser.userId(), householdId, memberUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{householdId}/leave")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId) {
        householdApplicationService.leave(currentUser.userId(), householdId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{householdId}/owner")
    public ResponseEntity<Void> transferOwnership(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId,
            @Valid @RequestBody TransferOwnershipRequest request) {
        householdApplicationService.transferOwnership(
                currentUser.userId(), householdId, request.newOwnerUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Passa contas do espaço pessoal do usuário para este grupo, com o histórico. É uma mudança de
     * dono, não uma cópia: as contas deixam de aparecer no espaço pessoal.
     */
    /**
     * Devolve contas do grupo ao espaço pessoal de quem pede (o inverso de compartilhar). Só quem
     * trouxe ou criou a conta pode; o histórico inteiro vai junto.
     */
    @PostMapping("/{householdId}/accounts/unshare")
    public AccountSharingResponse unshareAccounts(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId,
            @Valid @RequestBody ShareAccountsRequest request) {
        var result =
                accountSharingApplicationService.unshareAccounts(
                        currentUser.userId(), householdId, request.accountIds());
        return new AccountSharingResponse(result.accounts(), result.transactions());
    }

    @PostMapping("/{householdId}/accounts/share")
    public AccountSharingResponse shareAccounts(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long householdId,
            @Valid @RequestBody ShareAccountsRequest request) {
        var result =
                accountSharingApplicationService.shareAccounts(
                        currentUser.userId(), householdId, request.accountIds());
        return new AccountSharingResponse(result.accounts(), result.transactions());
    }

    private static HouseholdResponse toResponse(HouseholdSummary summary) {
        return new HouseholdResponse(summary.id(), summary.name(), summary.type(), summary.role());
    }

    private static HouseholdMemberResponse toResponse(HouseholdMemberView member) {
        return new HouseholdMemberResponse(
                member.userId(), member.name(), member.email(), member.role());
    }

    private static HouseholdInviteResponse toResponse(HouseholdInvite invite) {
        return new HouseholdInviteResponse(
                invite.id(), invite.email(), invite.expiresAt(), invite.createdAt());
    }
}
