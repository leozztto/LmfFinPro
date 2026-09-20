package com.lmf.finpro.infrastructure.web.dto.client;

import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;

public record ClientResponse(
        Long id,
        String name,
        String email,
        String phone,
        DocumentType documentType,
        String documentNumber,
        ClientWorkType workType,
        String notes,
        String color,
        boolean active) {}
