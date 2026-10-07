package com.lmf.finpro.application.household;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.HouseholdPermissionException;
import com.lmf.finpro.domain.exception.HouseholdRuleException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.BlockingLink;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Household;
import com.lmf.finpro.domain.model.HouseholdMembership;
import com.lmf.finpro.domain.model.HouseholdRole;
import com.lmf.finpro.domain.model.HouseholdType;
import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.port.out.AccountOwnershipPort;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.AccountSharingPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.HouseholdRepositoryPort;
import com.lmf.finpro.domain.port.out.TagRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountSharingApplicationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PERSONAL_ID = 11L;
    private static final Long SHARED_ID = 10L;
    private static final Long ACCOUNT_ID = 100L;

    @Mock private HouseholdRepositoryPort householdRepositoryPort;
    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;
    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private TagRepositoryPort tagRepositoryPort;
    @Mock private AccountSharingPort accountSharingPort;
    @Mock private AccountOwnershipPort accountOwnershipPort;

    private AccountSharingApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new AccountSharingApplicationService(
                        householdRepositoryPort,
                        accountRepositoryPort,
                        categoryRepositoryPort,
                        clientRepositoryPort,
                        tagRepositoryPort,
                        accountSharingPort,
                        accountOwnershipPort);
    }

    private void givenMemberOfShared() {
        when(householdRepositoryPort.findMembership(SHARED_ID, USER_ID))
                .thenReturn(
                        Optional.of(
                                new HouseholdMembership(SHARED_ID, USER_ID, HouseholdRole.MEMBER)));
        when(householdRepositoryPort.findById(SHARED_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        SHARED_ID,
                                        "Família Silva",
                                        HouseholdType.SHARED,
                                        LocalDateTime.now())));
        when(householdRepositoryPort.findPersonalMembership(USER_ID))
                .thenReturn(
                        Optional.of(
                                new HouseholdMembership(
                                        PERSONAL_ID, USER_ID, HouseholdRole.OWNER)));
    }

    private void givenPersonalAccount(Long accountId, Long householdId) {
        when(accountRepositoryPort.findById(accountId))
                .thenReturn(
                        Optional.of(
                                new Account(
                                        accountId,
                                        householdId,
                                        "Conta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
    }

    @Test
    void rejectsAnEmptySelection() {
        assertThatThrownBy(() -> service.shareAccounts(USER_ID, SHARED_ID, Set.of()))
                .isInstanceOf(HouseholdRuleException.class);
    }

    @Test
    void rejectsWhoIsNotAMemberOfTheTargetGroup() {
        when(householdRepositoryPort.findMembership(SHARED_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.shareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(HouseholdPermissionException.class);
        verify(accountSharingPort, never()).moveToHousehold(any(), any());
    }

    @Test
    void rejectsSharingWithAPersonalHousehold() {
        when(householdRepositoryPort.findMembership(PERSONAL_ID, USER_ID))
                .thenReturn(
                        Optional.of(
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

        assertThatThrownBy(() -> service.shareAccounts(USER_ID, PERSONAL_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(HouseholdRuleException.class);
    }

    @Test
    void anAccountOutsideThePersonalSpaceIsNotFound() {
        givenMemberOfShared();
        givenPersonalAccount(ACCOUNT_ID, 999L);

        assertThatThrownBy(() -> service.shareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(accountSharingPort, never()).moveToHousehold(any(), any());
    }

    @Test
    void blockedByALinkToAnAccountLeftBehind() {
        givenMemberOfShared();
        givenPersonalAccount(ACCOUNT_ID, PERSONAL_ID);
        when(accountSharingPort.findBlockingLinks(Set.of(ACCOUNT_ID)))
                .thenReturn(
                        List.of(
                                new BlockingLink(
                                        BlockingLink.Type.SAVINGS_GOAL, "Corrente", "Poupança")));

        assertThatThrownBy(() -> service.shareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(HouseholdRuleException.class)
                .hasMessageContaining("Corrente")
                .hasMessageContaining("Poupança");
        verify(accountSharingPort, never()).moveToHousehold(any(), any());
        verify(categoryRepositoryPort, never()).save(any());
    }

    @Test
    void movesTheAccountAndCopiesWhatItsTransactionsUse() {
        givenMemberOfShared();
        givenPersonalAccount(ACCOUNT_ID, PERSONAL_ID);
        Set<Long> ids = Set.of(ACCOUNT_ID);
        when(accountSharingPort.findBlockingLinks(ids)).thenReturn(List.of());
        when(accountSharingPort.countTransactions(ids)).thenReturn(42);

        // Duas categorias pessoais: "Mercado" já existe no grupo (reaproveita), "Lazer" não
        // (copia).
        // E uma padrão do sistema (sem grupo), que não muda.
        Category mercadoPersonal =
                new Category(5L, PERSONAL_ID, "Mercado", CategoryType.EXPENSE, "#111111", null);
        Category lazerPersonal =
                new Category(6L, PERSONAL_ID, "Lazer", CategoryType.EXPENSE, "#222222", "star");
        Category global = new Category(7L, null, "Moradia", CategoryType.EXPENSE, null, null);
        Category mercadoShared =
                new Category(50L, SHARED_ID, "mercado", CategoryType.EXPENSE, null, null);
        when(accountSharingPort.findCategoryIdsUsedBy(ids)).thenReturn(Set.of(5L, 6L, 7L));
        when(categoryRepositoryPort.findAllVisibleToUser(SHARED_ID))
                .thenReturn(List.of(mercadoShared, global));
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(mercadoPersonal));
        when(categoryRepositoryPort.findById(6L)).thenReturn(Optional.of(lazerPersonal));
        when(categoryRepositoryPort.findById(7L)).thenReturn(Optional.of(global));
        when(categoryRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            Category toSave = invocation.getArgument(0);
                            return new Category(
                                    60L,
                                    toSave.householdId(),
                                    toSave.name(),
                                    toSave.type(),
                                    toSave.color(),
                                    toSave.icon());
                        });

        Tag viagemPersonal = new Tag(8L, PERSONAL_ID, "viagem", "#abcdef", LocalDateTime.now());
        when(accountSharingPort.findTagIdsUsedBy(ids)).thenReturn(Set.of(8L));
        when(tagRepositoryPort.findAllByHouseholdId(SHARED_ID)).thenReturn(List.of());
        when(tagRepositoryPort.findById(8L)).thenReturn(Optional.of(viagemPersonal));
        when(tagRepositoryPort.save(any()))
                .thenAnswer(
                        invocation -> {
                            Tag toSave = invocation.getArgument(0);
                            return new Tag(
                                    80L,
                                    toSave.householdId(),
                                    toSave.name(),
                                    toSave.color(),
                                    toSave.createdAt());
                        });
        when(accountSharingPort.findClientIdsUsedBy(ids)).thenReturn(Set.of());

        AccountSharingResult result = service.shareAccounts(USER_ID, SHARED_ID, ids);

        assertThat(result).isEqualTo(new AccountSharingResult(1, 42));
        // "Mercado" reaproveita a do grupo (50); "Lazer" vira uma cópia nova (60); a global fica.
        verify(accountSharingPort).remapCategories(ids, Map.of(5L, 50L, 6L, 60L));
        verify(categoryRepositoryPort)
                .save(Category.create(SHARED_ID, "Lazer", CategoryType.EXPENSE, "#222222", "star"));
        verify(accountSharingPort).remapTags(ids, Map.of(8L, 80L));
        verify(accountSharingPort).moveToHousehold(ids, SHARED_ID);
        // Quem traz a conta para o grupo passa a ser o dono dela.
        verify(accountOwnershipPort).recordOwner(ids, USER_ID);
    }

    private void givenGroupAccount(Long accountId, Long householdId) {
        when(accountRepositoryPort.findById(accountId))
                .thenReturn(
                        Optional.of(
                                new Account(
                                        accountId,
                                        householdId,
                                        "Conjunta",
                                        AccountType.CHECKING,
                                        BigDecimal.ZERO,
                                        null)));
    }

    private void givenMemberOfSharedWithRole(HouseholdRole role) {
        when(householdRepositoryPort.findMembership(SHARED_ID, USER_ID))
                .thenReturn(Optional.of(new HouseholdMembership(SHARED_ID, USER_ID, role)));
        when(householdRepositoryPort.findById(SHARED_ID))
                .thenReturn(
                        Optional.of(
                                new Household(
                                        SHARED_ID,
                                        "Família Silva",
                                        HouseholdType.SHARED,
                                        LocalDateTime.now())));
        when(householdRepositoryPort.findPersonalMembership(USER_ID))
                .thenReturn(
                        Optional.of(
                                new HouseholdMembership(
                                        PERSONAL_ID, USER_ID, HouseholdRole.OWNER)));
    }

    @Test
    void unshareMovesTheAccountBackToThePersonalSpaceOfItsOwner() {
        givenMemberOfSharedWithRole(HouseholdRole.MEMBER);
        givenGroupAccount(ACCOUNT_ID, SHARED_ID);
        Set<Long> ids = Set.of(ACCOUNT_ID);
        when(accountOwnershipPort.findOwner(ACCOUNT_ID)).thenReturn(Optional.of(USER_ID));
        when(accountSharingPort.findBlockingLinks(ids)).thenReturn(List.of());
        when(accountSharingPort.countTransactions(ids)).thenReturn(7);

        AccountSharingResult result = service.unshareAccounts(USER_ID, SHARED_ID, ids);

        assertThat(result).isEqualTo(new AccountSharingResult(1, 7));
        verify(accountSharingPort).moveToHousehold(ids, PERSONAL_ID);
    }

    @Test
    void unshareIsRejectedWhenAnotherMemberOwnsTheAccount() {
        givenMemberOfSharedWithRole(HouseholdRole.OWNER);
        givenGroupAccount(ACCOUNT_ID, SHARED_ID);
        when(accountOwnershipPort.findOwner(ACCOUNT_ID)).thenReturn(Optional.of(99L));

        // Nem o dono do grupo leva embora a conta que outra pessoa trouxe.
        assertThatThrownBy(() -> service.unshareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(HouseholdPermissionException.class);
        verify(accountSharingPort, never()).moveToHousehold(any(), any());
    }

    @Test
    void unshareOfAnAccountWithUnknownOwnerIsAllowedOnlyForTheGroupOwner() {
        givenMemberOfSharedWithRole(HouseholdRole.OWNER);
        givenGroupAccount(ACCOUNT_ID, SHARED_ID);
        when(accountOwnershipPort.findOwner(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(accountSharingPort.findBlockingLinks(Set.of(ACCOUNT_ID))).thenReturn(List.of());

        service.unshareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID));

        verify(accountSharingPort).moveToHousehold(Set.of(ACCOUNT_ID), PERSONAL_ID);
        verify(accountOwnershipPort).recordOwner(Set.of(ACCOUNT_ID), USER_ID);
    }

    @Test
    void unshareOfAnAccountWithUnknownOwnerIsRejectedForARegularMember() {
        givenMemberOfSharedWithRole(HouseholdRole.MEMBER);
        givenGroupAccount(ACCOUNT_ID, SHARED_ID);
        when(accountOwnershipPort.findOwner(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unshareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(HouseholdPermissionException.class);
        verify(accountSharingPort, never()).moveToHousehold(any(), any());
    }

    @Test
    void unshareOfAnAccountThatIsNotInTheGroupIsNotFound() {
        givenMemberOfSharedWithRole(HouseholdRole.OWNER);
        givenGroupAccount(ACCOUNT_ID, PERSONAL_ID);

        assertThatThrownBy(() -> service.unshareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unshareByANonMemberIsRejected() {
        when(householdRepositoryPort.findMembership(SHARED_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unshareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(HouseholdPermissionException.class);
    }

    @Test
    void unshareIsBlockedByASavingsGoalLinkingAnAccountThatStays() {
        givenMemberOfSharedWithRole(HouseholdRole.MEMBER);
        givenGroupAccount(ACCOUNT_ID, SHARED_ID);
        when(accountOwnershipPort.findOwner(ACCOUNT_ID)).thenReturn(Optional.of(USER_ID));
        when(accountSharingPort.findBlockingLinks(Set.of(ACCOUNT_ID)))
                .thenReturn(
                        List.of(
                                new BlockingLink(
                                        BlockingLink.Type.SAVINGS_GOAL, "Reserva", "Corrente")));

        assertThatThrownBy(() -> service.unshareAccounts(USER_ID, SHARED_ID, Set.of(ACCOUNT_ID)))
                .isInstanceOf(HouseholdRuleException.class)
                .hasMessageContaining("Reserva");
        verify(accountSharingPort, never()).moveToHousehold(any(), any());
    }
}
