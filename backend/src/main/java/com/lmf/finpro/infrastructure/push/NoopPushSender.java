package com.lmf.finpro.infrastructure.push;

import com.lmf.finpro.domain.model.PushMessage;
import com.lmf.finpro.domain.model.PushSubscription;
import com.lmf.finpro.domain.port.out.PushSenderPort;
import java.util.Optional;

/** Usado quando as chaves VAPID não estão configuradas: o push fica desligado. */
public class NoopPushSender implements PushSenderPort {

    @Override
    public Optional<String> publicKey() {
        return Optional.empty();
    }

    @Override
    public Result send(PushSubscription subscription, PushMessage message) {
        return Result.FAILED;
    }
}
