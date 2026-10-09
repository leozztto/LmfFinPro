package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.ActivationEmailKind;

public interface ActivationMailerPort {
    void send(String toEmail, String userName, ActivationEmailKind kind);
}
