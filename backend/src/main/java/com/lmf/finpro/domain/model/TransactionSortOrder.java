package com.lmf.finpro.domain.model;

/** Ordem da listagem de transações; o desempate é sempre pelo id, na mesma direção. */
public enum TransactionSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}
