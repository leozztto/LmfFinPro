package com.lmf.finpro.infrastructure.persistence.entity;

import com.lmf.finpro.domain.model.ImportFormat;
import com.lmf.finpro.domain.model.ImportStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "import_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportBatchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "household_id", nullable = false)
    private Long householdId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountJpaEntity account;

    @Column(name = "original_file")
    private String originalFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ImportFormat format;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportStatus status;

    @Column(name = "duplicate_count", nullable = false)
    @Builder.Default
    private int duplicateCount = 0;

    @PrePersist
    void onCreate() {
        if (importedAt == null) {
            importedAt = LocalDateTime.now();
        }
    }
}
