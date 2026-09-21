package com.lmf.finpro.infrastructure.web.dto.category;

import com.lmf.finpro.domain.model.CategoryType;

public record CategoryResponse(
        Long id, String name, CategoryType type, String color, String icon, boolean global) {}
