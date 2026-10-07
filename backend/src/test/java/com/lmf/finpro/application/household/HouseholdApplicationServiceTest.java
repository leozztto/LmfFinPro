package com.lmf.finpro.application.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.support.SecureTokens;
import com.lmf.finpro.domain.exception.HouseholdPermissionException;
import com.lmf.finpro.domain.exception.HouseholdRuleException;
import com.lmf.finpro.domain.exception.InvalidHouseholdInviteException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdInvite;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.HouseholdType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.HouseholdInviteMailerPort;
import com.lmf.finpro.domain.port.out.HouseholdInviteRepositoryPort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HouseholdApplicationServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long STRANGER_ID = 3L;
    private static final Long SHARED_ID = 10L;
    private static final Long PERSONAL_ID = 11L;

    @Mock private HouseholdRepositoryPort householdRepositoryPort;
    @Mock private HouseholdInviteRepositoryPort inviteRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private HouseholdInviteMailerPort mailerPort;

    private HouseholdApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new HouseholdApplicationService(
                        householdRepositoryPort,
                        inviteRepositoryPort,
                        userRepositoryPort,
                        mailerPort,
                        new HouseholdInviteSettings("http://app/convite", 7));
    }

    private void givenShared(Long userId, HouseholdRole role) {
        when(householdRepositoryPort.findMembership(SHARED_ID, userId))
                .thenReturn(Optional.of(new HouseholdMembership(SHARED_ID, userId, role)));
        when(householdRepositoryPort.findById(SHARED_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        SHARED_ID,
                                        "Família Silva",
                                        HouseholdType.SHARED,
                                        LocalDateTime.now())));
    }

    private static User user(Long id, String name, String email) {
        return new User(
                id,
                name,
                email,
                "hash",
                DocumentType.CPF,
                "52998224725",
                null,
                TaxRegime.AUTONOMO,
                null,
                LocalDateTime.now(),
                0);
    }

    private static HouseholdInvite pendingInvite(String rawToken, LocalDateTime expiresAt) {
        return new HouseholdInvite(
                5L,
                SHARED_ID,
                "bia@finpro.test",
                SecureTokens.sha256(rawToken),
                HouseholdRole.MEMBER,
                OWNER_ID,
                expiresAt,
                null,
                LocalDateTime.now());
    }

    @Test
    void createSharedMakesTheCreatorTheOwner() {
        when(householdRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            Household toSave = invocation.getArgument(0);
                            return new Household(
                                    SHARED_ID, toSave.name(), toSave.type(), toSave.createdAt());
                        });
        when(householdRepositoryPort.saveMembership(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HouseholdSummary created = service.createShared(OWNER_ID, "  Família Silva ");

        assertThat(created.name()).isEqualTo("Família Silva");
        assertThat(created.type()).isEqualTo(HouseholdType.SHARED);
        assertThat(created.role()).isEqualTo(HouseholdRole.OWNER);
        verify(householdRepositoryPort)
                .saveMembership(new HouseholdMembership(SHARED_ID, OWNER_ID, HouseholdRole.OWNER));
    }

    @Test
    void inviteStoresOnlyTheHashAndEmailsTheRawTokenInTheLink() {
        givenShared(OWNER_ID, HouseholdRole.OWNER);
        when(userRepositoryPort.findByEmail("bia@finpro.test")).thenReturn(Optional.empty());
        when(userRepositoryPort.findById(OWNER_ID))
                .thenReturn(Optional.of(user(OWNER_ID, "Ana Silva", "ana@finpro.test")));
        when(inviteRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.invite(OWNER_ID, SHARED_ID, "  Bia@FinPro.test ");

        ArgumentCaptor<HouseholdInvite> saved = ArgumentCaptor.forClass(HouseholdInvite.class);
        verify(inviteRepositoryPort).save(saved.capture());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailerPort)
                .sendInvite(
                        eq("bia@finpro.test"),
                        eq("Ana Silva"),
                        eq("Família Silva"),
                        link.capture(),
                        eq(7L));
        String rawToken = link.getValue().substring("http://app/convite?token=".length());
        assertThat(saved.getValue().tokenHash())
                .isEqualTo(SecureTokens.sha256(rawToken))
                .isNotEqualTo(rawToken);
        assertThat(saved.getValue().email()).isEqualTo("bia@finpro.test");
        verify(inviteRepositoryPort)
                .deletePendingByHouseholdIdAndEmail(eq(SHARED_ID), eq("bia@finpro.test"), any());
    }

    @Test
    void inviteIsRejectedWhenThePersonAlreadyBelongsToTheGroup() {
        givenShared(OWNER_ID, HouseholdRole.OWNER);
        when(userRepositoryPort.findByEmail("bia@finpro.test"))
                .thenReturn(Optional.of(user(MEMBER_ID, "Bia", "bia@finpro.test")));
        when(householdRepositoryPort.findMembership(SHARED_ID, MEMBER_ID))
                .thenReturn(
                        Optional.of(
                                new HouseholdMembership(
                                        SHARED_ID, MEMBER_ID, HouseholdRole.MEMBER)));

        assertThatThrownBy(() -> service.invite(OWNER_ID, SHARED_ID, "bia@finpro.test"))
                .isInstanceOf(HouseholdRuleException.class);
        verify(mailerPort, never()).sendInvite(any(), any(), any(), any(), anyLong());
    }

    @Test
    void onlyTheOwnerCanInvite() {
        givenShared(MEMBER_ID, HouseholdRole.MEMBER);

        assertThatThrownBy(() -> service.invite(MEMBER_ID, SHARED_ID, "x@finpro.test"))
                .isInstanceOf(HouseholdPermissionException.class);
        verify(inviteRepositoryPort, never()).save(any());
    }

    @Test
    void nonMembersCannotManageOrSeeTheGroup() {
        when(householdRepositoryPort.findMembership(SHARED_ID, STRANGER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listMembers(STRANGER_ID, SHARED_ID))
                .isInstanceOf(HouseholdPermissionException.class);
        assertThatThrownBy(() -> service.invite(STRANGER_ID, SHARED_ID, "x@finpro.test"))
                .isInstanceOf(HouseholdPermissionException.class);
    }

    @Test
    void personalHouseholdDoesNotAcceptInvites() {
        when(householdRepositoryPort.findMembership(PERSONAL_ID, OWNER_ID))
                .thenReturn(
                        Optional.of(
                                new HouseholdMembership(
                                        PERSONAL_ID, OWNER_ID, HouseholdRole.OWNER)));
        when(householdRepositoryPort.findById(PERSONAL_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        PERSONAL_ID,
                                        "Ana",
                                        HouseholdType.PERSONAL,
                                        LocalDateTime.now())));

        assertThatThrownBy(() -> service.invite(OWNER_ID, PERSONAL_ID, "bia@finpro.test"))
                .isInstanceOf(HouseholdRuleException.class);
    }

    @Test
    void acceptInviteAddsTheUserAsMemberAndConsumesTheInvite() {
        HouseholdInvite invite = pendingInvite("tok", LocalDateTime.now().plusDays(1));
        when(inviteRepositoryPort.findByTokenHash(SecureTokens.sha256("tok")))
                .thenReturn(Optional.of(invite));
        when(householdRepositoryPort.findById(SHARED_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        SHARED_ID,
                                        "Família Silva",
                                        HouseholdType.SHARED,
                                        LocalDateTime.now())));
        when(householdRepositoryPort.findMembership(SHARED_ID, MEMBER_ID))
                .thenReturn(Optional.empty());
        when(householdRepositoryPort.saveMembership(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HouseholdSummary joined = service.acceptInvite(MEMBER_ID, "tok");

        assertThat(joined.id()).isEqualTo(SHARED_ID);
        assertThat(joined.role()).isEqualTo(HouseholdRole.MEMBER);
        verify(householdRepositoryPort)
                .saveMembership(
                        new HouseholdMembership(SHARED_ID, MEMBER_ID, HouseholdRole.MEMBER));
        ArgumentCaptor<HouseholdInvite> consumed = ArgumentCaptor.forClass(HouseholdInvite.class);
        verify(inviteRepositoryPort).save(consumed.capture());
        assertThat(consumed.getValue().acceptedAt()).isNotNull();
    }

    @Test
    void acceptInviteRejectsExpiredAndUnknownTokens() {
        when(inviteRepositoryPort.findByTokenHash(SecureTokens.sha256("velho")))
                .thenReturn(
                        Optional.of(pendingInvite("velho", LocalDateTime.now().minusMinutes(1))));
        when(inviteRepositoryPort.findByTokenHash(SecureTokens.sha256("nada")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptInvite(MEMBER_ID, "velho"))
                .isInstanceOf(InvalidHouseholdInviteException.class);
        assertThatThrownBy(() -> service.acceptInvite(MEMBER_ID, "nada"))
                .isInstanceOf(InvalidHouseholdInviteException.class);
        verify(householdRepositoryPort, never()).saveMembership(any());
    }

    @Test
    void acceptInviteRejectsAnAlreadyUsedInvite() {
        HouseholdInvite used =
                pendingInvite("usado", LocalDateTime.now().plusDays(1))
                        .markAccepted(LocalDateTime.now());
        when(inviteRepositoryPort.findByTokenHash(SecureTokens.sha256("usado")))
                .thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.acceptInvite(MEMBER_ID, "usado"))
                .isInstanceOf(InvalidHouseholdInviteException.class);
    }

    @Test
    void ownerCanRemoveAMemberButNotThemselves() {
        givenShared(OWNER_ID, HouseholdRole.OWNER);
        when(householdRepositoryPort.findMembership(SHARED_ID, MEMBER_ID))
                .thenReturn(
                        Optional.of(
                                new HouseholdMembership(
                                        SHARED_ID, MEMBER_ID, HouseholdRole.MEMBER)));

        service.removeMember(OWNER_ID, SHARED_ID, MEMBER_ID);

        verify(householdRepositoryPort).deleteMembership(SHARED_ID, MEMBER_ID);
        assertThatThrownBy(() -> service.removeMember(OWNER_ID, SHARED_ID, OWNER_ID))
                .isInstanceOf(HouseholdRuleException.class);
    }

    @Test
    void removingAnUnknownMemberIsNotFound() {
        givenShared(OWNER_ID, HouseholdRole.OWNER);
        when(householdRepositoryPort.findMembership(SHARED_ID, STRANGER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(OWNER_ID, SHARED_ID, STRANGER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void memberCanLeaveButOwnerCannot() {
        givenShared(MEMBER_ID, HouseholdRole.MEMBER);
        service.leave(MEMBER_ID, SHARED_ID);
        verify(householdRepositoryPort).deleteMembership(SHARED_ID, MEMBER_ID);

        givenShared(OWNER_ID, HouseholdRole.OWNER);
        assertThatThrownBy(() -> service.leave(OWNER_ID, SHARED_ID))
                .isInstanceOf(HouseholdRuleException.class);
        verify(householdRepositoryPort, never()).deleteMembership(SHARED_ID, OWNER_ID);
    }

    @Test
    void personalHouseholdCannotBeLeft() {
        when(householdRepositoryPort.findMembership(PERSONAL_ID, OWNER_ID))
                .thenReturn(
                        Optional.of(
                                new HouseholdMembership(
                                        PERSONAL_ID, OWNER_ID, HouseholdRole.OWNER)));
        when(householdRepositoryPort.findById(PERSONAL_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        PERSONAL_ID,
                                        "Ana",
                                        HouseholdType.PERSONAL,
                                        LocalDateTime.now())));

        assertThatThrownBy(() -> service.leave(OWNER_ID, PERSONAL_ID))
                .isInstanceOf(HouseholdRuleException.class);
    }

    @Test
    void transferOwnershipSwapsTheRoles() {
        givenShared(OWNER_ID, HouseholdRole.OWNER);
        when(householdRepositoryPort.findMembership(SHARED_ID, MEMBER_ID))
                .thenReturn(
                        Optional.of(
                                new HouseholdMembership(
                                        SHARED_ID, MEMBER_ID, HouseholdRole.MEMBER)));

        service.transferOwnership(OWNER_ID, SHARED_ID, MEMBER_ID);

        verify(householdRepositoryPort)
                .saveMembership(new HouseholdMembership(SHARED_ID, MEMBER_ID, HouseholdRole.OWNER));
        verify(householdRepositoryPort)
                .saveMembership(new HouseholdMembership(SHARED_ID, OWNER_ID, HouseholdRole.MEMBER));
    }

    @Test
    void listMineReturnsEveryHouseholdWithTheUsersRole() {
        when(householdRepositoryPort.findMembershipsByUserId(OWNER_ID))
                .thenReturn(
                        List.of(
                                new HouseholdMembership(PERSONAL_ID, OWNER_ID, HouseholdRole.OWNER),
                                new HouseholdMembership(
                                        SHARED_ID, OWNER_ID, HouseholdRole.MEMBER)));
        when(householdRepositoryPort.findById(PERSONAL_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        PERSONAL_ID,
                                        "Ana",
                                        HouseholdType.PERSONAL,
                                        LocalDateTime.now())));
        when(householdRepositoryPort.findById(SHARED_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        SHARED_ID,
                                        "Família Silva",
                                        HouseholdType.SHARED,
                                        LocalDateTime.now())));

        List<HouseholdSummary> mine = service.listMine(OWNER_ID);

        assertThat(mine)
                .extracting(HouseholdSummary::type, HouseholdSummary::role)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                HouseholdType.PERSONAL, HouseholdRole.OWNER),
                        org.assertj.core.groups.Tuple.tuple(
                                HouseholdType.SHARED, HouseholdRole.MEMBER));
    }

    private void givenBiaIsLoggedIn() {
        when(userRepositoryPort.findById(MEMBER_ID))
                .thenReturn(Optional.of(user(MEMBER_ID, "Bia", "Bia@FinPro.test")));
    }

    private void givenSharedHousehold() {
        when(householdRepositoryPort.findById(SHARED_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        SHARED_ID,
                                        "Família Silva",
                                        HouseholdType.SHARED,
                                        LocalDateTime.now())));
    }

    @Test
    void listReceivedInvitesShowsThePendingInvitesAddressedToTheAccountEmail() {
        givenBiaIsLoggedIn();
        givenSharedHousehold();
        when(inviteRepositoryPort.findPendingByEmail(eq("Bia@FinPro.test"), any()))
                .thenReturn(List.of(pendingInvite("tok", LocalDateTime.now().plusDays(1))));
        when(userRepositoryPort.findById(OWNER_ID))
                .thenReturn(Optional.of(user(OWNER_ID, "Ana Silva", "ana@finpro.test")));

        List<ReceivedInviteView> received = service.listReceivedInvites(MEMBER_ID);

        assertThat(received)
                .singleElement()
                .satisfies(
                        invite -> {
                            assertThat(invite.inviteId()).isEqualTo(5L);
                            assertThat(invite.householdName()).isEqualTo("Família Silva");
                            assertThat(invite.inviterName()).isEqualTo("Ana Silva");
                        });
    }

    @Test
    void acceptReceivedInviteJoinsTheGroupWhenTheEmailMatchesIgnoringCase() {
        givenBiaIsLoggedIn();
        givenSharedHousehold();
        when(inviteRepositoryPort.findById(5L))
                .thenReturn(Optional.of(pendingInvite("tok", LocalDateTime.now().plusDays(1))));
        when(householdRepositoryPort.findMembership(SHARED_ID, MEMBER_ID))
                .thenReturn(Optional.empty());
        when(householdRepositoryPort.saveMembership(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HouseholdSummary joined = service.acceptReceivedInvite(MEMBER_ID, 5L);

        assertThat(joined.id()).isEqualTo(SHARED_ID);
        assertThat(joined.role()).isEqualTo(HouseholdRole.MEMBER);
        verify(householdRepositoryPort)
                .saveMembership(
                        new HouseholdMembership(SHARED_ID, MEMBER_ID, HouseholdRole.MEMBER));
        ArgumentCaptor<HouseholdInvite> consumed = ArgumentCaptor.forClass(HouseholdInvite.class);
        verify(inviteRepositoryPort).save(consumed.capture());
        assertThat(consumed.getValue().acceptedAt()).isNotNull();
    }

    @Test
    void acceptReceivedInviteIsRejectedWhenTheInviteIsForAnotherEmail() {
        // O id do convite é fácil de adivinhar: sem a checagem de e-mail qualquer um entraria.
        when(userRepositoryPort.findById(STRANGER_ID))
                .thenReturn(Optional.of(user(STRANGER_ID, "Carla", "carla@finpro.test")));
        when(inviteRepositoryPort.findById(5L))
                .thenReturn(Optional.of(pendingInvite("tok", LocalDateTime.now().plusDays(1))));

        assertThatThrownBy(() -> service.acceptReceivedInvite(STRANGER_ID, 5L))
                .isInstanceOf(InvalidHouseholdInviteException.class);
        verify(householdRepositoryPort, never()).saveMembership(any());
    }

    @Test
    void acceptReceivedInviteIsRejectedWhenExpired() {
        givenBiaIsLoggedIn();
        when(inviteRepositoryPort.findById(5L))
                .thenReturn(Optional.of(pendingInvite("tok", LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> service.acceptReceivedInvite(MEMBER_ID, 5L))
                .isInstanceOf(InvalidHouseholdInviteException.class);
        verify(householdRepositoryPort, never()).saveMembership(any());
    }

    @Test
    void declineReceivedInviteDeletesIt() {
        givenBiaIsLoggedIn();
        when(inviteRepositoryPort.findById(5L))
                .thenReturn(Optional.of(pendingInvite("tok", LocalDateTime.now().plusDays(1))));

        service.declineReceivedInvite(MEMBER_ID, 5L);

        verify(inviteRepositoryPort).deleteById(5L);
    }

    @Test
    void declineReceivedInviteOfAnotherEmailDeletesNothing() {
        when(userRepositoryPort.findById(STRANGER_ID))
                .thenReturn(Optional.of(user(STRANGER_ID, "Carla", "carla@finpro.test")));
        when(inviteRepositoryPort.findById(5L))
                .thenReturn(Optional.of(pendingInvite("tok", LocalDateTime.now().plusDays(1))));

        assertThatThrownBy(() -> service.declineReceivedInvite(STRANGER_ID, 5L))
                .isInstanceOf(InvalidHouseholdInviteException.class);
        verify(inviteRepositoryPort, never()).deleteById(any());
    }
}
