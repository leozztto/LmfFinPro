package com.lmf.finpro.domain.model;

/**
 * Regra de autoria dos lançamentos e transferências: numa conta compartilhada, só quem criou o
 * registro pode excluí-lo. Registro sem autor conhecido (criado pelo sistema ou antes de a autoria
 * existir) pode ser excluído por qualquer membro do grupo.
 */
public final class RecordAuthorship {

    private RecordAuthorship() {}

    /** Nenhum autor conhecido = ninguém em particular é dono do registro. */
    public static boolean canDelete(Long authorUserId, Long currentUserId) {
        return authorUserId == null || authorUserId.equals(currentUserId);
    }
}
