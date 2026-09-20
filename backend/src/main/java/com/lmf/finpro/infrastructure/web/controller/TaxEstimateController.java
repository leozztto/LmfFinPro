package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.taxestimate.TaxEstimateApplicationService;
import com.lmf.finpro.domain.model.TaxEstimate;
import com.lmf.finpro.domain.model.TaxRegime;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.taxestimate.SuggestedRateResponse;
import com.lmf.finpro.infrastructure.web.dto.taxestimate.TaxEstimateRequest;
import com.lmf.finpro.infrastructure.web.dto.taxestimate.TaxEstimateResponse;
import com.lmf.finpro.infrastructure.web.mapper.TaxEstimateWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/tax-estimates")
@RequiredArgsConstructor
public class TaxEstimateController {

    private final TaxEstimateApplicationService taxEstimateApplicationService;
    private final TaxEstimateWebMapper mapper;

    @GetMapping
    public List<TaxEstimateResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return taxEstimateApplicationService.list(currentUser.userId()).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/suggested-rate")
    public SuggestedRateResponse suggestedRate(
        @RequestParam TaxRegime regime, @RequestParam BigDecimal grossRevenue
    ) {
        return new SuggestedRateResponse(taxEstimateApplicationService.suggestRate(regime, grossRevenue));
    }

    @PostMapping
    public ResponseEntity<TaxEstimateResponse> create(
        @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody TaxEstimateRequest request
    ) {
        TaxEstimate created = taxEstimateApplicationService.create(
            currentUser.userId(), request.referenceMonth(), request.regime(), request.grossRevenue(), request.appliedRate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        taxEstimateApplicationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
