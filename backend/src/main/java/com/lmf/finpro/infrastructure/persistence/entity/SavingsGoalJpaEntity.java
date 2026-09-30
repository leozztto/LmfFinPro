package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.SavingsGoalType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "savings_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private SavingsGoalType type;

    @Column(name = "target_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "income_rate", precision = 6, scale = 4)
    private BigDecimal incomeRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "auto_contribute", nullable = false)
    private boolean autoContribute;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "funding_account_id", nullable = false)
    private Long fundingAccountId;
}
