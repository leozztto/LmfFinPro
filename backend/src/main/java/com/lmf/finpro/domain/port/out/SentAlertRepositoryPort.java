package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.domain.model.SentAlert;

public interface SentAlertRepositoryPort {
    boolean exists(Long userId, AlertType type, String referenceKey);

    void save(SentAlert sentAlert);
}
