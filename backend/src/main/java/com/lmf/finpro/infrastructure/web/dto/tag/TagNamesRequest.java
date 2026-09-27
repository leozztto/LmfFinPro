package com.lmf.finpro.infrastructure.web.dto.tag;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Troca as tags de uma transação; lista vazia tira todas. Tags novas são criadas na hora. */
public record TagNamesRequest(@NotNull(message = "informe as tags") List<String> tagNames) {}
