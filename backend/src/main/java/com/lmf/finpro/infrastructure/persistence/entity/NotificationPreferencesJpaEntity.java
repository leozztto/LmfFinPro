package com.lmf.finpro.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferencesJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "bills_enabled", nullable = false)
    private boolean billsEnabled;

    @Column(name = "bill_days_before", nullable = false)
    private int billDaysBefore;

    @Column(name = "budgets_enabled", nullable = false)
    private boolean budgetsEnabled;

    @Column(name = "das_enabled", nullable = false)
    private boolean dasEnabled;
}
