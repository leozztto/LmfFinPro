package com.lmf.finpro.application.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.HouseholdRuleException;
import com.lmf.finpro.domain.exception.IncorrectCurrentPasswordException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.HouseholdType;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountErasurePort;
import com.lmf.finpro.domain.port.out.FileStoragePort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.PasswordHasherPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountDeletionApplicationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PERSONAL_ID = 10L;
    private static final Long SHARED_ID = 20L;

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private HouseholdRepositoryPort householdRepositoryPort;
    @Mock private AccountErasurePort accountErasurePort;
    @Mock private PasswordHasherPort passwordHasherPort;
    @Mock private FileStoragePort fileStoragePort;

    @InjectMocks private AccountDeletionApplicationService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = userWithPhoto(null);
    }

    private static User userWithPhoto(String photoKey) {
        return new User(
                USER_ID,
                "Ana Freelancer",
                "ana@finpro.test",
                "hashed",
                DocumentType.CPF,
                "52998224725",
                "11987654321",
                TaxRegime.AUTONOMO,
                null,
                LocalDateTime.now(),
                0,
                photoKey,
                photoKey == null ? null : "image/png");
    }

    private void givenUser() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private void givenCorrectPassword() {
        when(passwordHasherPort.matches("senha12345", "hashed")).thenReturn(true);
    }

    private void givenPersonalSpace() {
        when(householdRepositoryPort.findMembershipsByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new HouseholdMembership(
                                        PERSONAL_ID, USER_ID, HouseholdRole.OWNER)));
        when(householdRepositoryPort.findById(PERSONAL_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        PERSONAL_ID,
                                        "Ana",
                                        HouseholdType.PERSONAL,
                                        LocalDateTime.now())));
    }

    private void givenSharedGroup(HouseholdRole role, boolean withOtherMember) {
        when(householdRepositoryPort.findMembershipsByUserId(USER_ID))
                .thenReturn(
                        List.of(
                                new HouseholdMembership(PERSONAL_ID, USER_ID, HouseholdRole.OWNER),
                                new HouseholdMembership(SHARED_ID, USER_ID, role)));
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
                                        "Casa",
                                        HouseholdType.SHARED,
                                        LocalDateTime.now())));
        List<HouseholdMembership> members =
                withOtherMember
                        ? List.of(
                                new HouseholdMembership(SHARED_ID, USER_ID, role),
                                new HouseholdMembership(SHARED_ID, 2L, HouseholdRole.MEMBER))
                        : List.of(new HouseholdMembership(SHARED_ID, USER_ID, role));
        when(householdRepositoryPort.findMembershipsByHouseholdId(SHARED_ID)).thenReturn(members);
    }

    @Test
    void wrongPasswordRefusesAndErasesNothing() {
        givenUser();
        when(passwordHasherPort.matches("errada", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.delete(USER_ID, "errada"))
                .isInstanceOf(IncorrectCurrentPasswordException.class);

        verify(accountErasurePort, never()).deleteUser(any());
        verify(accountErasurePort, never()).deleteHouseholds(any());
    }

    @Test
    void blankPasswordIsRefused() {
        givenUser();

        assertThatThrownBy(() -> service.delete(USER_ID, " "))
                .isInstanceOf(IncorrectCurrentPasswordException.class);

        verify(accountErasurePort, never()).deleteUser(any());
    }

    @Test
    void unknownUserIsNotFound() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, "senha12345"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletesThePersonalSpaceTheInvitesTheUserAndLogsIt() {
        givenUser();
        givenCorrectPassword();
        givenPersonalSpace();

        service.delete(USER_ID, "senha12345");

        verify(accountErasurePort).deleteHouseholds(List.of(PERSONAL_ID));
        verify(accountErasurePort).deleteInvitesAddressedTo("ana@finpro.test");
        verify(accountErasurePort).deleteUser(USER_ID);
        verify(accountErasurePort).logDeletion(USER_ID);
    }

    @Test
    void filesAreDeletedOnlyAfterTheDatabaseWork() {
        user = userWithPhoto("profile-abc.png");
        givenUser();
        givenCorrectPassword();
        givenPersonalSpace();
        when(accountErasurePort.findAttachmentKeys(List.of(PERSONAL_ID)))
                .thenReturn(List.of("key-1", "key-2"));

        service.delete(USER_ID, "senha12345");

        InOrder order = inOrder(accountErasurePort, fileStoragePort);
        order.verify(accountErasurePort).deleteUser(USER_ID);
        order.verify(fileStoragePort).delete("key-1");
        order.verify(fileStoragePort).delete("key-2");
        order.verify(fileStoragePort).delete("profile-abc.png");
    }

    @Test
    void ownerOfASharedGroupWithOthersIsBlocked() {
        givenUser();
        givenCorrectPassword();
        givenSharedGroup(HouseholdRole.OWNER, true);

        assertThatThrownBy(() -> service.delete(USER_ID, "senha12345"))
                .isInstanceOf(HouseholdRuleException.class)
                .hasMessageContaining("Casa");

        verify(accountErasurePort, never()).deleteUser(any());
        verify(accountErasurePort, never()).deleteHouseholds(any());
    }

    @Test
    void memberLeavesTheSharedGroupAndOnlyThePersonalSpaceIsDeleted() {
        givenUser();
        givenCorrectPassword();
        givenSharedGroup(HouseholdRole.MEMBER, true);

        service.delete(USER_ID, "senha12345");

        verify(accountErasurePort).deleteHouseholds(List.of(PERSONAL_ID));
        verify(accountErasurePort).deleteUser(USER_ID);
    }

    @Test
    void sharedGroupWhereTheUserIsAloneIsDeletedToo() {
        givenUser();
        givenCorrectPassword();
        givenSharedGroup(HouseholdRole.OWNER, false);

        service.delete(USER_ID, "senha12345");

        verify(accountErasurePort).deleteHouseholds(List.of(PERSONAL_ID, SHARED_ID));
    }

    @Test
    void previewDescribesWhatWillHappenWithoutErasingAnything() {
        givenSharedGroup(HouseholdRole.MEMBER, true);
        when(accountErasurePort.countAccountsBroughtBy(USER_ID, PERSONAL_ID)).thenReturn(0);
        when(accountErasurePort.countAccountsBroughtBy(USER_ID, SHARED_ID)).thenReturn(2);
        when(accountErasurePort.findAttachmentKeys(List.of(PERSONAL_ID)))
                .thenReturn(List.of("a", "b", "c"));

        AccountDeletionPreview preview = service.preview(USER_ID);

        assertThat(preview.canDelete()).isTrue();
        assertThat(preview.deletedGroups()).hasSize(1);
        assertThat(preview.leftGroups()).hasSize(1);
        assertThat(preview.leftGroups().get(0).accountsBroughtByYou()).isEqualTo(2);
        assertThat(preview.attachmentCount()).isEqualTo(3);
        verify(accountErasurePort, never()).deleteUser(any());
    }

    @Test
    void previewReportsTheBlockerForAnOwnerWithOtherMembers() {
        givenSharedGroup(HouseholdRole.OWNER, true);

        AccountDeletionPreview preview = service.preview(USER_ID);

        assertThat(preview.canDelete()).isFalse();
        assertThat(preview.blockers()).singleElement().asString().contains("Casa");
    }
}
