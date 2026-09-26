package com.lmf.finpro.domain.model;

/**
 * Registro de um aviso já enviado. {@code referenceKey} identifica o que foi avisado dentro do
 * tipo: o id da transação (conta a vencer), o id do orçamento (80%/100%) ou a competência do DAS
 * ({@code 2026-09}).
 */
public record SentAlert(Long userId, AlertType type, String referenceKey) {}
