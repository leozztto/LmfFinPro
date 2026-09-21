package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.model.TaxRegime;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 4)
    private DocumentType documentType;

    @Column(name = "document_number", unique = true, length = 14)
    private String documentNumber;

    @Column(length = 11)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", length = 50)
    private TaxRegime taxRegime;

    @Embedded private AddressEmbeddable address;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
