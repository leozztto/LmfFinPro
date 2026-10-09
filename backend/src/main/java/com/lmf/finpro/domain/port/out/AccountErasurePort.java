package com.lmf.finpro.domain.port.out;

import java.util.Collection;
import java.util.List;

/** Remoção definitiva dos dados de uma conta (LGPD, art. 18, VI). */
public interface AccountErasurePort {

    /** Chaves, no armazenamento de arquivos, dos anexos dos grupos. */
    List<String> findAttachmentKeys(Collection<Long> householdIds);

    /** Quantas contas do grupo foram trazidas por esta pessoa. */
    int countAccountsBroughtBy(Long userId, Long householdId);

    /** Apaga os grupos e, em cascata, todos os dados financeiros deles. */
    void deleteHouseholds(Collection<Long> householdIds);

    /** Convites pendentes endereçados ao e-mail da pessoa (em outros grupos). */
    void deleteInvitesAddressedTo(String email);

    /** Apaga o usuário e, em cascata, sessões, push, preferências e consentimentos. */
    void deleteUser(Long userId);

    /** Registra que a conta foi excluída (sem nenhum dado pessoal). */
    void logDeletion(Long userId);
}
