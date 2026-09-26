package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.ProLaboreSettings;
import java.util.Optional;

public interface ProLaboreSettingsRepositoryPort {
    ProLaboreSettings save(ProLaboreSettings settings);

    Optional<ProLaboreSettings> findByUserId(Long userId);
}
