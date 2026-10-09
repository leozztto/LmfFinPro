package com.lmf.finpro.domain.port.out;

/** Verificações de saúde das dependências da plataforma, para a página pública de status. */
public interface PlatformHealthPort {

    /** O banco de dados aceita conexões e responde dentro de um prazo curto. */
    boolean isDatabaseAvailable();
}
