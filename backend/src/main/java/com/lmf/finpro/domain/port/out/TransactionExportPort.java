package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.TransactionExportData;

public interface TransactionExportPort {
    byte[] exportTransactionsToCsv(TransactionExportData data);
}
