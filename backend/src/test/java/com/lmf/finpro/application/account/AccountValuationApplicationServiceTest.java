package com.lmf.finpro.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.AccountValuation;
import com.lmf.finpro.domain.port.out.AccountValuationRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountValuationApplicationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 27);
    private static final Long USER_ID = 10L;

    @Mock private AccountApplicationService accountApplicationService;
    @Mock private AccountValuationRepositoryPort accountValuationRepositoryPort;

    private AccountValuationApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new AccountValuationApplicationService(
                        accountApplicationService,
                        accountValuationRepositoryPort,
                        Clock.fixed(TODAY.atTime(10, 0).atZone(ZONE).toInstant(), ZONE));
    }

    @Test
    void savingTheSameDateReplacesTheValue() {
        givenAccount(AccountType.INVESTMENT);
        AccountValuation existing =
                new AccountValuation(7L, 1L, TODAY, new BigDecimal("1000"), null);
        when(accountValuationRepositoryPort.findByAccountIdAndDate(1L, TODAY))
                .thenReturn(Optional.of(existing));
        when(accountValuationRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountValuation saved = service.save(USER_ID, 1L, TODAY, new BigDecimal("1100"));

        assertThat(saved.id()).isEqualTo(7L);
        assertThat(saved.value()).isEqualByComparingTo("1100");
    }

    @Test
    void onlyInvestmentAccountsAcceptValuationsAndNeverInTheFuture() {
        givenAccount(AccountType.CHECKING);
        assertThatThrownBy(() -> service.save(USER_ID, 1L, TODAY, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);

        givenAccount(AccountType.INVESTMENT);
        assertThatThrownBy(() -> service.save(USER_ID, 1L, TODAY.plusDays(1), BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        verify(accountValuationRepositoryPort, never()).save(any());
    }

    @Test
    void deletingAValuationOfAnotherAccountIsNotFound() {
        givenAccount(AccountType.INVESTMENT);
        when(accountValuationRepositoryPort.findById(7L))
                .thenReturn(
                        Optional.of(new AccountValuation(7L, 99L, TODAY, BigDecimal.ONE, null)));

        assertThatThrownBy(() -> service.delete(USER_ID, 1L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(accountValuationRepositoryPort, never()).deleteById(any());
    }

    private void givenAccount(AccountType type) {
        when(accountApplicationService.getById(USER_ID, 1L))
                .thenReturn(new Account(1L, USER_ID, "Conta", type, BigDecimal.ZERO, null));
    }
}
