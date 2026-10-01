package com.lmf.finpro.domain.model;

/**
 * Pedido de uma página, independente de framework. {@code page} começa em 0; quem decide padrões e
 * limites é a camada de aplicação.
 */
public record PageQuery(int page, int size) {

    public PageQuery {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("página deve ser >= 0 e tamanho >= 1");
        }
    }
}
