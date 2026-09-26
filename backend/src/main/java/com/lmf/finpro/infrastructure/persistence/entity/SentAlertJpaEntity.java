package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.AlertType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "sent_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentAlertJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Column(name = "reference_key", nullable = false, length = 50)
    private String referenceKey;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
