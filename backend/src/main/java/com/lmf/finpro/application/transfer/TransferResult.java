package com.lmf.finpro.application.transfer;

import com.lmf.finpro.domain.model.Transfer;

public record TransferResult(Transfer transfer, Long fromTransactionId, Long toTransactionId) {}
