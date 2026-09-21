package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.application.transfer.TransferResult;
import com.lmf.finpro.infrastructure.web.dto.transfer.TransferResponse;
import org.springframework.stereotype.Component;

@Component
public class TransferWebMapper {

    public TransferResponse toResponse(TransferResult result) {
        return new TransferResponse(
                result.transfer().id(),
                result.transfer().fromAccountId(),
                result.transfer().toAccountId(),
                result.transfer().amount(),
                result.transfer().transferDate(),
                result.transfer().description(),
                result.fromTransactionId(),
                result.toTransactionId(),
                result.transfer().createdAt());
    }
}
