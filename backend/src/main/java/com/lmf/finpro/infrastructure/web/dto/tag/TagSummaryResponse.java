package com.lmf.finpro.infrastructure.web.dto.tag;

import com.lmf.finpro.domain.model.Tag;
import java.util.Collection;
import java.util.List;

/** Tag como aparece dentro de uma transação ou recorrência. */
public record TagSummaryResponse(Long id, String name, String color) {

    public static List<TagSummaryResponse> of(Collection<Tag> tags) {
        return tags.stream()
                .map(tag -> new TagSummaryResponse(tag.id(), tag.name(), tag.color()))
                .toList();
    }
}
