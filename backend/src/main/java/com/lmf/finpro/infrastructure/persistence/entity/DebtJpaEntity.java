package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.DebtType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "debts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DebtType type;

    @Column(name = "creditor", length = 100)
    private String creditor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
