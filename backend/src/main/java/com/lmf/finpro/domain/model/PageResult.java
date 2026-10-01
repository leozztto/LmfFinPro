package com.lmf.finpro.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Uma página de resultados, independente de framework. {@code page} começa em 0.
 *
 * @param totalElements quantidade de itens em todas as páginas, com os mesmos filtros
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public <R> PageResult<R> map(Function<? super T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0);
    }
}
