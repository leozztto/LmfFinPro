package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.LegalDocumentType;
import com.lmf.finpro.domain.model.UserConsent;
import java.util.List;

public interface UserConsentRepositoryPort {

    /** Registra o aceite. Aceitar de novo a mesma versão não duplica nem muda a data original. */
    void save(Long userId, LegalDocumentType documentType, String version);

    /** Todos os aceites do usuário, do mais antigo para o mais novo. */
    List<UserConsent> findByUserId(Long userId);
}
