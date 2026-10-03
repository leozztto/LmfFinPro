package com.lmf.finpro.infrastructure.push;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.finpro.domain.model.PushMessage;
import com.lmf.finpro.domain.model.PushSubscription;
import com.lmf.finpro.domain.port.out.PushSenderPort;
import com.lmf.finpro.infrastructure.logging.SafeErrors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Envia Web Push cifrado e assinado com as chaves VAPID, direto ao serviço de push do navegador.
 */
@Slf4j
public class WebPushSender implements PushSenderPort {

    private final String publicKey;
    private final PushService pushService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry registry;

    public WebPushSender(
            String publicKey,
            String privateKey,
            String subject,
            ObjectMapper objectMapper,
            MeterRegistry registry) {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            this.pushService = new PushService(publicKey, privateKey, subject);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Chaves VAPID inválidas (finpro.push.*)", ex);
        }
        this.publicKey = publicKey;
        this.objectMapper = objectMapper;
        this.registry = registry;
        // Série de falha existe (em 0) desde a subida: um contador que nasce já em 1 não gera
        // increase() no Prometheus e o primeiro erro passaria sem alerta.
        registry.counter("finpro.push.send", "outcome", "failed", "status", "error");
    }

    @Override
    public Optional<String> publicKey() {
        return Optional.of(publicKey);
    }

    @Override
    public Result send(PushSubscription subscription, PushMessage message) {
        Timer.Sample sample = Timer.start(registry);
        Result result = Result.FAILED;
        String statusClass = "error";
        long start = System.nanoTime();
        try {
            byte[] payload =
                    objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
            Notification notification =
                    new Notification(
                            subscription.endpoint(),
                            subscription.p256dh(),
                            subscription.auth(),
                            payload);
            HttpResponse response = pushService.send(notification, Encoding.AES128GCM);
            int status = response.getStatusLine().getStatusCode();
            statusClass = statusClass(status);
            if (status == 404 || status == 410) {
                result = Result.EXPIRED;
                log.info("Inscrição de push expirada {}", kv("httpStatus", status));
            } else if (status >= 200 && status < 300) {
                result = Result.DELIVERED;
            } else {
                // O corpo é a explicação do serviço de push (ex.: chave VAPID não confere); não
                // traz
                // dado do usuário.
                log.warn(
                        "Serviço de push recusou a notificação {} {}",
                        kv("httpStatus", status),
                        kv("reason", responseBody(response)));
            }
        } catch (Exception ex) {
            // Sem o endpoint no log: ele identifica o aparelho do usuário.
            log.warn("Falha ao enviar push: {}", SafeErrors.describe(ex));
        } finally {
            String outcome = result.name().toLowerCase(Locale.ROOT);
            sample.stop(registry.timer("finpro.push.send.duration", "outcome", outcome));
            registry.counter("finpro.push.send", "outcome", outcome, "status", statusClass)
                    .increment();
            log.debug(
                    "Push enviado {} {}",
                    kv("outcome", outcome),
                    kv("durationMs", (System.nanoTime() - start) / 1_000_000));
        }
        return result;
    }

    private static String responseBody(HttpResponse response) {
        try {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return body.length() > 300 ? body.substring(0, 300) : body;
        } catch (Exception ex) {
            return "(corpo ilegível)";
        }
    }

    /** Classe do status HTTP (2xx, 4xx...) para não explodir a cardinalidade da métrica. */
    private static String statusClass(int status) {
        return (status / 100) + "xx";
    }
}
