package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.AlertDigest;

public interface AlertMailerPort {
    void sendDigest(String toEmail, String userName, AlertDigest digest);
}
