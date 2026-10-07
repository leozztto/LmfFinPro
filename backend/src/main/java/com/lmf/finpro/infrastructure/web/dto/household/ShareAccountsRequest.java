package com.lmf.finpro.infrastructure.web.dto.household;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record ShareAccountsRequest(
        @NotEmpty(message = "escolha ao menos uma conta") Set<Long> accountIds) {}
