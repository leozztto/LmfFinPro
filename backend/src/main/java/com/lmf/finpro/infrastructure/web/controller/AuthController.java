package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.auth.AddressCommand;
import com.lmf.finpro.application.auth.AuthApplicationService;
import com.lmf.finpro.application.auth.AuthResult;
import com.lmf.finpro.application.auth.LoginCommand;
import com.lmf.finpro.application.auth.PasswordResetApplicationService;
import com.lmf.finpro.application.auth.RefreshTokenApplicationService;
import com.lmf.finpro.application.auth.RegisterCommand;
import com.lmf.finpro.domain.exception.InvalidTokenException;
import com.lmf.finpro.infrastructure.config.RefreshTokenProperties;
import com.lmf.finpro.infrastructure.security.AuthEmailRateLimiter;
import com.lmf.finpro.infrastructure.web.RefreshCookieFactory;
import com.lmf.finpro.infrastructure.web.dto.auth.AddressRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.auth.ForgotPasswordRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.LoginRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.RegisterRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.ResetPasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final AuthEmailRateLimiter authEmailRateLimiter;
    private final RefreshTokenApplicationService refreshTokenApplicationService;
    private final RefreshCookieFactory refreshCookieFactory;

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
                                toAddressCommand(request.address()),
                                request.inviteToken()));
        return withRefreshCookie(ResponseEntity.status(HttpStatus.CREATED), result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authEmailRateLimiter.checkLogin(request.email());
        AuthResult result =
                authApplicationService.login(new LoginCommand(request.email(), request.password()));
        return withRefreshCookie(ResponseEntity.ok(), result);
    }

    /**
     * Troca o refresh token (cookie httpOnly) por um access token novo e rotaciona o cookie. Sem
     * cookie válido responde 401 e apaga o cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenProperties.COOKIE_NAME, required = false)
                    String refreshToken) {
        try {
            return withRefreshCookie(
                    ResponseEntity.ok(), refreshTokenApplicationService.refresh(refreshToken));
        } catch (InvalidTokenException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString())
                    .build();
        }
    }

    /** Encerra a sessão do navegador: revoga o refresh token e apaga o cookie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenProperties.COOKIE_NAME, required = false)
                    String refreshToken) {
        refreshTokenApplicationService.endSession(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString())
                .build();
    }

    /** Sempre 202, exista ou não conta com o e-mail — ver PasswordResetApplicationService. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authEmailRateLimiter.checkForgotPassword(request.email());
        passwordResetApplicationService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetApplicationService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    /**
     * Refresh token nulo = manter o cookie que o navegador já tem (ver
     * RefreshTokenApplicationService).
     */
    private ResponseEntity<AuthResponse> withRefreshCookie(
            ResponseEntity.BodyBuilder builder, AuthResult result) {
        if (result.refreshToken() != null) {
            builder.header(
                    HttpHeaders.SET_COOKIE,
                    refreshCookieFactory.create(result.refreshToken()).toString());
        }
        return builder.body(
                new AuthResponse(result.token(), result.userId(), result.name(), result.email()));
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
