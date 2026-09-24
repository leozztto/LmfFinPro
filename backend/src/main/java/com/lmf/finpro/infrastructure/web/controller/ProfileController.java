package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.auth.AddressCommand;
import com.lmf.finpro.application.auth.AuthResult;
import com.lmf.finpro.application.profile.ProfileApplicationService;
import com.lmf.finpro.application.profile.UpdateProfileCommand;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.auth.AddressRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.profile.ChangePasswordRequest;
import com.lmf.finpro.infrastructure.web.dto.profile.ProfileResponse;
import com.lmf.finpro.infrastructure.web.dto.profile.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "Meu perfil": sempre o usuário do token — não existe como editar o cadastro de outra pessoa. */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileApplicationService profileApplicationService;

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return toResponse(profileApplicationService.getProfile(currentUser.userId()));
    }

    @PutMapping
    public ProfileResponse update(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        User updated =
                profileApplicationService.updateProfile(
                        currentUser.userId(),
                        new UpdateProfileCommand(
                                request.name(),
                                request.email(),
                                request.documentType(),
                                request.documentNumber(),
                                request.phone(),
                                request.taxRegime(),
                                toAddressCommand(request.address()),
                                request.currentPassword()));
        return toResponse(updated);
    }

    /** Devolve um token novo: a troca encerra as outras sessões, mas mantém a de quem trocou. */
    @PutMapping("/password")
    public AuthResponse changePassword(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthResult result =
                profileApplicationService.changePassword(
                        currentUser.userId(), request.currentPassword(), request.newPassword());
        return new AuthResponse(result.token(), result.userId(), result.name(), result.email());
    }

    private ProfileResponse toResponse(User user) {
        Address address = user.address();
        return new ProfileResponse(
                user.id(),
                user.name(),
                user.email(),
                user.documentType(),
                user.documentNumber(),
                user.phone(),
                user.taxRegime(),
                address == null
                        ? null
                        : new ProfileResponse.AddressResponse(
                                address.zipCode(),
                                address.street(),
                                address.number(),
                                address.complement(),
                                address.neighborhood(),
                                address.city(),
                                address.state()),
                user.createdAt());
    }

    private AddressCommand toAddressCommand(AddressRequest address) {
        return new AddressCommand(
                address.zipCode(),
                address.street(),
                address.number(),
                address.complement(),
                address.neighborhood(),
                address.city(),
                address.state());
    }
}
