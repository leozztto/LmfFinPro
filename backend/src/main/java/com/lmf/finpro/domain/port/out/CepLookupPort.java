package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.CepAddress;

public interface CepLookupPort {
    CepAddress lookup(String zipCode);
}
