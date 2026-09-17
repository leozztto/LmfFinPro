package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.cep.CepApplicationService;
import com.lmf.finpro.domain.model.CepAddress;
import com.lmf.finpro.infrastructure.web.dto.cep.CepResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cep")
@RequiredArgsConstructor
public class CepController {

    private final CepApplicationService cepApplicationService;

    @GetMapping("/{zipCode}")
    public CepResponse getByZipCode(@PathVariable String zipCode) {
        return toResponse(cepApplicationService.lookup(zipCode));
    }

    private CepResponse toResponse(CepAddress address) {
        return new CepResponse(
            address.zipCode(), address.street(), address.complement(),
            address.neighborhood(), address.city(), address.state()
        );
    }
}
