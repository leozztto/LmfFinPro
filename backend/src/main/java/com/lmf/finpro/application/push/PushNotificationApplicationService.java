package com.lmf.finpro.application.push;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.PushMessage;
import com.lmf.finpro.domain.model.PushSubscription;
import com.lmf.finpro.domain.port.out.PushSenderPort;
import com.lmf.finpro.domain.port.out.PushSubscriptionRepositoryPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inscrições de Web Push do usuário e envio do resumo diário como notificação no aparelho. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationApplicationService {

    private final PushSubscriptionRepositoryPort subscriptionRepositoryPort;
    private final PushSenderPort pushSenderPort;
    private final MeterRegistry meterRegistry;

    public Optional<String> publicKey() {
        return pushSenderPort.publicKey();
    }

    @Transactional
    public void subscribe(Long userId, String endpoint, String p256dh, String auth) {
        subscriptionRepositoryPort.upsert(new PushSubscription(userId, endpoint, p256dh, auth));
        subscriptionCounter("subscribed").increment();
        // Sem o endpoint no log: ele identifica o aparelho do usuário.
        log.info("Inscrição de push registrada {}", kv("userId", userId));
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        subscriptionRepositoryPort.deleteByUserIdAndEndpoint(userId, endpoint);
        subscriptionCounter("unsubscribed").increment();
        log.info("Inscrição de push removida {}", kv("userId", userId));
    }

    /**
     * Envia o resumo a todos os aparelhos do usuário. Nunca lança: o push é um complemento do
     * e-mail e uma falha aqui não pode impedir o registro dos avisos como enviados.
     *
     * @return se ao menos um aparelho recebeu
     */
    public boolean sendDigest(Long userId, AlertDigest digest) {
        if (pushSenderPort.publicKey().isEmpty()) {
            digestCounter("disabled").increment();
            return false;
        }
        String outcome = "error";
        try {
            List<PushSubscription> subscriptions =
                    subscriptionRepositoryPort.findAllByUserId(userId);
            if (subscriptions.isEmpty()) {
                outcome = "no_subscriptions";
                return false;
            }
            PushMessage message = PushMessage.fromDigest(digest);
            int delivered = 0;
            int expired = 0;
            int failed = 0;
            for (PushSubscription subscription : subscriptions) {
                PushSenderPort.Result result = pushSenderPort.send(subscription, message);
                switch (result) {
                    case DELIVERED -> delivered++;
                    case EXPIRED -> {
                        expired++;
                        subscriptionRepositoryPort.deleteByUserIdAndEndpoint(
                                userId, subscription.endpoint());
                        subscriptionCounter("expired_removed").increment();
                    }
                    case FAILED -> failed++;
                }
            }
            outcome = delivered > 0 ? "delivered" : "not_delivered";
            log.info(
                    "Push do resumo diário {} {} {} {} {}",
                    kv("userId", userId),
                    kv("devices", subscriptions.size()),
                    kv("delivered", delivered),
                    kv("expired", expired),
                    kv("failed", failed));
            return delivered > 0;
        } catch (RuntimeException ex) {
            log.warn(
                    "Falha ao enviar o push do usuário {}: {}",
                    userId,
                    ex.getClass().getSimpleName());
            return false;
        } finally {
            digestCounter(outcome).increment();
        }
    }

    private Counter digestCounter(String outcome) {
        return meterRegistry.counter("finpro.push.digest", "outcome", outcome);
    }

    private Counter subscriptionCounter(String action) {
        return meterRegistry.counter("finpro.push.subscriptions", "action", action);
    }
}
