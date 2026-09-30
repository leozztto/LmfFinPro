package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.ContributionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "goal_contributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalContributionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ContributionType type;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "transfer_id", nullable = false)
    private Long transferId;
}
