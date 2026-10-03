package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.PushMessage;
import com.lmf.finpro.domain.model.PushSubscription;
import java.util.Optional;

public interface PushSenderPort {

    enum Result {
        DELIVERED,
        /** O navegador não aceita mais a inscrição (410/404): ela deve ser removida. */
        EXPIRED,
        FAILED
    }

    /**
     * Chave pública VAPID para o navegador se inscrever; vazio quando o push não está configurado.
     */
    Optional<String> publicKey();

    Result send(PushSubscription subscription, PushMessage message);
}
