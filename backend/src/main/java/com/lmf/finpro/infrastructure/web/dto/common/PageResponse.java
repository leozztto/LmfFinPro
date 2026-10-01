package com.lmf.finpro.infrastructure.web.dto.common;

import com.lmf.finpro.domain.model.PageResult;
import java.util.List;

/** Corpo das listagens paginadas. {@code page} começa em 0. */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(PageResult<T> result) {
        return of(result.content(), result);
    }

    /** Conteúdo já convertido (ex.: para DTOs), com a paginação da página de origem. */
    public static <T> PageResponse<T> of(List<T> content, PageResult<?> source) {
        return new PageResponse<>(
                content, source.page(), source.size(), source.totalElements(), source.totalPages());
    }
}
