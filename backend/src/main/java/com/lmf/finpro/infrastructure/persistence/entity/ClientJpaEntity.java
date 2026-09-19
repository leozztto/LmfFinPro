package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 11)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 4)
    private DocumentType documentType;

    @Column(name = "document_number", length = 14)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", length = 10)
    private ClientWorkType workType;

    @Column(length = 1000)
    private String notes;

    @Column(length = 20)
    private String color;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
