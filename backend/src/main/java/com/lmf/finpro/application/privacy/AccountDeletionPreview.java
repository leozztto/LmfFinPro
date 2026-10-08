package com.lmf.finpro.application.privacy;

import com.lmf.finpro.domain.model.HouseholdType;
import java.util.List;

/**
 * O que a exclusão da conta faria, mostrado antes de a pessoa confirmar.
 *
 * @param deletedGroups os grupos apagados por inteiro (o espaço pessoal e os compartilhados em que
 *     ela é a única pessoa)
 * @param leftGroups os grupos compartilhados de que ela sai; os dados deles continuam com os outros
 *     membros, sem o nome dela como autora
 * @param blockers motivos que impedem a exclusão agora; vazio quando pode excluir
 * @param attachmentCount arquivos anexados que serão apagados junto com os grupos removidos
 */
public record AccountDeletionPreview(
        List<GroupImpact> deletedGroups,
        List<GroupImpact> leftGroups,
        List<String> blockers,
        int attachmentCount) {

    /**
     * @param accountsBroughtByYou contas que a pessoa trouxe para o grupo (só relevante nos grupos
     *     de que ela sai: essas contas ficam lá)
     */
    public record GroupImpact(
            Long householdId, String name, HouseholdType type, int accountsBroughtByYou) {}

    public boolean canDelete() {
        return blockers.isEmpty();
    }
}
