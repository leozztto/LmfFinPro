package com.lmf.finpro.application.transfer;

import com.lmf.finpro.domain.model.Transfer;

/**
 * @param fromAccountName nome da conta de origem, mesmo quando ela é de outro espaço (pessoal x
 *     grupo): é só o nome, nenhum outro dado da conta, para a tela dizer de onde veio a
 *     transferência
 * @param toAccountName nome da conta de destino, nas mesmas condições
 */
public record TransferResult(
        Transfer transfer,
        Long fromTransactionId,
        Long toTransactionId,
        String fromAccountName,
        String toAccountName) {

    public TransferResult(Transfer transfer, Long fromTransactionId, Long toTransactionId) {
        this(transfer, fromTransactionId, toTransactionId, null, null);
    }
}
