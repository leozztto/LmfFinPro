package com.lmf.finpro.application.profile;

import com.lmf.finpro.application.auth.AddressCommand;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;

/**
 * @param currentPassword obrigatória só quando o e-mail muda (o e-mail é o login da conta)
 */
public record UpdateProfileCommand(
        String name,
        String email,
        DocumentType documentType,
        String documentNumber,
        String phone,
        TaxRegime taxRegime,
        AddressCommand address,
        String currentPassword) {}
