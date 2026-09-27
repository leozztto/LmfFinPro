package com.lmf.finpro.infrastructure.web.dto.tag;

/** Tag na tela de gestão, com quantas transações a usam. */
public record TagResponse(Long id, String name, String color, long transactionCount) {}
