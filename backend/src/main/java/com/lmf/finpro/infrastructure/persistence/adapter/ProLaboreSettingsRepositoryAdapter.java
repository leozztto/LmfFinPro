package com.lmf.finpro.infrastructure.persistence.adapter;

import com.lmf.finpro.domain.model.ProLaboreSettings;
import com.lmf.finpro.domain.port.out.ProLaboreSettingsRepositoryPort;
import com.lmf.finpro.infrastructure.persistence.entity.ProLaboreSettingsJpaEntity;
import com.lmf.finpro.infrastructure.persistence.repository.ProLaboreSettingsJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProLaboreSettingsRepositoryAdapter implements ProLaboreSettingsRepositoryPort {

    private final ProLaboreSettingsJpaRepository repository;

    @Override
    public ProLaboreSettings save(ProLaboreSettings settings) {
        return toDomain(
                repository.save(
                        ProLaboreSettingsJpaEntity.builder()
                                .userId(settings.userId())
                                .calculationBase(settings.calculationBase())
                                .cashCushionMonths(settings.cashCushionMonths())
                                .reserveRate(settings.reserveRate())
                                .taxMode(settings.taxMode())
                                .manualTaxRate(settings.manualTaxRate())
                                .fixedAmount(settings.fixedAmount())
                                .withholdingMode(settings.withholdingMode())
                                .employerInssRate(settings.employerInssRate())
                                .build()));
    }

    @Override
    public Optional<ProLaboreSettings> findByUserId(Long userId) {
        return repository.findById(userId).map(this::toDomain);
    }

    private ProLaboreSettings toDomain(ProLaboreSettingsJpaEntity entity) {
        return new ProLaboreSettings(
                entity.getUserId(),
                entity.getCalculationBase(),
                entity.getCashCushionMonths(),
                entity.getReserveRate(),
                entity.getTaxMode(),
                entity.getManualTaxRate(),
                entity.getFixedAmount(),
                entity.getWithholdingMode(),
                entity.getEmployerInssRate());
    }
}
