package com.lmf.finpro.application.cep;

import com.lmf.finpro.domain.model.CepAddress;
import com.lmf.finpro.domain.port.out.CepLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CepApplicationService {

    private final CepLookupPort cepLookupPort;

    public CepAddress lookup(String rawZipCode) {
        String zipCode = rawZipCode == null ? "" : rawZipCode.replaceAll("\\D", "");
        if (!zipCode.matches("\\d{8}")) {
            throw new IllegalArgumentException("CEP deve ter 8 dígitos");
        }
        return cepLookupPort.lookup(zipCode);
    }
}
