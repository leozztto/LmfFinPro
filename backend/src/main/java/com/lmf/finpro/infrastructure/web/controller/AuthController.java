package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.auth.AddressCommand;
import com.lmf.finpro.application.auth.AuthApplicationService;
import com.lmf.finpro.application.auth.AuthResult;
import com.lmf.finpro.application.auth.LoginCommand;
import com.lmf.finpro.application.auth.PasswordResetApplicationService;
import com.lmf.finpro.application.auth.RegisterCommand;
import com.lmf.finpro.infrastructure.web.dto.auth.AddressRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.ForgotPasswordRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.ResetPasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final PasswordResetApplicationService passwordResetApplicationService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result =
                authApplicationService.register(
                        new RegisterCommand(
                                request.name(),
                                request.email(),
                                request.password(),
                                request.documentType(),
                                request.documentNumber(),
                                request.phone(),
                                request.taxRegime(),
                                toAddressCommand(request.address())));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result =
                authApplicationService.login(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(toResponse(result));
    }

    /** Sempre 202, exista ou não conta com o e-mail — ver PasswordResetApplicationService. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetApplicationService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetApplicationService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(result.token(), result.userId(), result.name(), result.email());
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
