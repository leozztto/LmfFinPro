package com.lmf.finpro.application.support;

/**
 * Quem criou um lançamento ou transferência, para a tela dizer "lançado por" e travar a exclusão.
 */
public record RecordAuthor(Long userId, String name) {}
