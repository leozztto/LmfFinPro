package com.lmf.finpro.application.cep;

import com.lmf.finpro.domain.model.CepAddress;
import com.lmf.finpro.domain.port.out.CepLookupPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CepApplicationService {

    private final CepLookupPort cepLookupPort;

    public CepAddress lookup(String rawZipCode) {
        log.debug("Consultando endereço por CEP");
        String zipCode = rawZipCode == null ? "" : rawZipCode.replaceAll("\\D", "");
        if (!zipCode.matches("\\d{8}")) {
            throw new IllegalArgumentException("CEP deve ter 8 dígitos");
        }
        return cepLookupPort.lookup(zipCode);
    }
}
