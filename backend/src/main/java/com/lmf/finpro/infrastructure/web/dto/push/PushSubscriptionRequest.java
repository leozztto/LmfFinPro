package com.lmf.finpro.infrastructure.web.dto.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formato do {@code PushSubscription.toJSON()} do navegador. */
public record PushSubscriptionRequest(
        @NotBlank(message = "endpoint é obrigatório")
                @Size(max = 1000, message = "endpoint muito longo")
                @Pattern(regexp = "^https://.*", message = "endpoint deve usar https")
                String endpoint,
        @NotBlank(message = "p256dh é obrigatório") @Size(max = 200, message = "p256dh muito longo")
                String p256dh,
        @NotBlank(message = "auth é obrigatório") @Size(max = 100, message = "auth muito longo")
                String auth) {}
