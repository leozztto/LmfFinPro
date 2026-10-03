package com.lmf.finpro.application.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.PushMessage;
import com.lmf.finpro.domain.model.PushSubscription;
import com.lmf.finpro.domain.port.out.PushSenderPort;
import com.lmf.finpro.domain.port.out.PushSubscriptionRepositoryPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationApplicationServiceTest {

    private static final Long USER_ID = 10L;
    private static final PushSubscription PHONE =
            new PushSubscription(USER_ID, "https://push.example/phone", "p256", "auth");
    private static final PushSubscription LAPTOP =
            new PushSubscription(USER_ID, "https://push.example/laptop", "p256", "auth");
    private static final AlertDigest DIGEST =
            new AlertDigest(
                    List.of(
                            new AlertDigest.BillDue(
                                    "Aluguel", BigDecimal.TEN, LocalDate.of(2026, 9, 20))),
                    List.of(),
                    null,
                    List.of());

    @Mock private PushSubscriptionRepositoryPort subscriptionRepositoryPort;
    @Mock private PushSenderPort pushSenderPort;
    @Spy private MeterRegistry meterRegistry = new SimpleMeterRegistry();
    @InjectMocks private PushNotificationApplicationService service;

    @Test
    void naoEnviaQuandoOPushNaoEstaConfigurado() {
        when(pushSenderPort.publicKey()).thenReturn(Optional.empty());

        assertThat(service.sendDigest(USER_ID, DIGEST)).isFalse();

        verify(subscriptionRepositoryPort, never()).findAllByUserId(any());
    }

    @Test
    void enviaParaTodosOsAparelhosERemoveOsExpirados() {
        when(pushSenderPort.publicKey()).thenReturn(Optional.of("chave"));
        when(subscriptionRepositoryPort.findAllByUserId(USER_ID))
                .thenReturn(List.of(PHONE, LAPTOP));
        when(pushSenderPort.send(any(PushSubscription.class), any(PushMessage.class)))
                .thenReturn(PushSenderPort.Result.DELIVERED, PushSenderPort.Result.EXPIRED);

        assertThat(service.sendDigest(USER_ID, DIGEST)).isTrue();

        verify(subscriptionRepositoryPort).deleteByUserIdAndEndpoint(USER_ID, LAPTOP.endpoint());
        verify(subscriptionRepositoryPort, never())
                .deleteByUserIdAndEndpoint(USER_ID, PHONE.endpoint());
        assertThat(meterRegistry.counter("finpro.push.digest", "outcome", "delivered").count())
                .isEqualTo(1);
        assertThat(
                        meterRegistry
                                .counter("finpro.push.subscriptions", "action", "expired_removed")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void falhaNoEnvioNaoPropagaNemRemoveAInscricao() {
        when(pushSenderPort.publicKey()).thenReturn(Optional.of("chave"));
        when(subscriptionRepositoryPort.findAllByUserId(USER_ID)).thenReturn(List.of(PHONE));
        when(pushSenderPort.send(any(PushSubscription.class), any(PushMessage.class)))
                .thenReturn(PushSenderPort.Result.FAILED);

        assertThat(service.sendDigest(USER_ID, DIGEST)).isFalse();

        verify(subscriptionRepositoryPort, never()).deleteByUserIdAndEndpoint(any(), any());
        assertThat(meterRegistry.counter("finpro.push.digest", "outcome", "not_delivered").count())
                .isEqualTo(1);
    }

    @Test
    void excecaoNoBancoNaoPropagaParaOEnvioDoEmail() {
        when(pushSenderPort.publicKey()).thenReturn(Optional.of("chave"));
        when(subscriptionRepositoryPort.findAllByUserId(USER_ID))
                .thenThrow(new IllegalStateException("banco fora"));

        assertThat(service.sendDigest(USER_ID, DIGEST)).isFalse();
        assertThat(meterRegistry.counter("finpro.push.digest", "outcome", "error").count())
                .isEqualTo(1);
    }

    @Test
    void mensagemResumeSoOQueHaDeNovo() {
        AlertDigest digest =
                new AlertDigest(
                        List.of(
                                new AlertDigest.BillDue("A", BigDecimal.ONE, LocalDate.now()),
                                new AlertDigest.BillDue("B", BigDecimal.ONE, LocalDate.now())),
                        List.of(
                                new AlertDigest.BudgetAlert(
                                        "Mercado", BigDecimal.TEN, BigDecimal.TEN, 100)),
                        new AlertDigest.DasReminder(
                                java.time.YearMonth.of(2026, 8), LocalDate.of(2026, 9, 20), null),
                        List.of());

        PushMessage message = PushMessage.fromDigest(digest);

        assertThat(message.body())
                .isEqualTo("2 contas a vencer · 1 orçamento no limite · DAS perto do vencimento");
        assertThat(message.url()).isEqualTo("/");
    }
}
