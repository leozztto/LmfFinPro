package com.lmf.finpro.application.legal;

import java.time.LocalDateTime;

/**
 * Situação do aceite de um usuário.
 *
 * @param pending verdadeiro se algum documento tem versão vigente ainda não aceita
 */
public record ConsentStatus(DocumentStatus terms, DocumentStatus privacy, boolean pending) {

    /**
     * @param currentVersion a versão vigente do documento
     * @param acceptedVersion a última versão que o usuário aceitou; nulo se nunca aceitou
     * @param acceptedAt quando aceitou essa última versão
     * @param accepted verdadeiro se a versão vigente já foi aceita
     */
    public record DocumentStatus(
            String currentVersion,
            String acceptedVersion,
            LocalDateTime acceptedAt,
            boolean accepted) {}
}
