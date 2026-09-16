package com.lmf.finpro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Regra usada pelo motor de categorização automática: quando o descritivo de uma
 * transação importada contém {@code pattern}, ela é categorizada como {@code category}.
 * O peso é reforçado quando o usuário confirma/corrige a categorização.
 */
@Entity
@Table(name = "category_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(nullable = false)
    private String pattern;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryJpaEntity category;

    @Column(nullable = false)
    @Builder.Default
    private Integer weight = 1;
}
