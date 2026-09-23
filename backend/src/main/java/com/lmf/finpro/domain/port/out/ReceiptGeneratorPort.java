package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.ClientReceiptData;

public interface ReceiptGeneratorPort {
    byte[] generateClientReceipt(ClientReceiptData data);
}
