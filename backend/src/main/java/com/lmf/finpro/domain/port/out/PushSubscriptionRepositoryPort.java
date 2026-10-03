package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.PushSubscription;
import java.util.List;

public interface PushSubscriptionRepositoryPort {

    /** Grava a inscrição; se o endpoint já existe, passa a pertencer ao usuário informado. */
    void upsert(PushSubscription subscription);

    List<PushSubscription> findAllByUserId(Long userId);

    void deleteByUserIdAndEndpoint(Long userId, String endpoint);
}
