package com.lmf.finpro.application.legal;

/**
 * Versões vigentes dos documentos legais. O texto vive no frontend (páginas /termos e
 * /privacidade); ao alterá-lo, mude a versão aqui e a mesma constante lá: todos os usuários voltam
 * a ver a tela de aceite no próximo acesso. A versão é a data da vigência (AAAA-MM-DD).
 */
public final class LegalDocuments {

    public static final String TERMS_VERSION = "2026-10-08";
    public static final String PRIVACY_VERSION = "2026-10-07";

    private LegalDocuments() {}
}
