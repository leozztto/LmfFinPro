package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.auth.AddressCommand;
import com.lmf.finpro.application.auth.AuthResult;
import com.lmf.finpro.application.profile.ProfileApplicationService;
import com.lmf.finpro.application.profile.ProfileApplicationService.PhotoContent;
import com.lmf.finpro.application.profile.UpdateProfileCommand;
import com.lmf.finpro.domain.exception.AttachmentInvalidException;
import com.lmf.finpro.domain.model.Address;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.auth.AddressRequest;
import com.lmf.finpro.infrastructure.web.dto.auth.AuthResponse;
import com.lmf.finpro.infrastructure.web.dto.profile.ChangePasswordRequest;
import com.lmf.finpro.infrastructure.web.dto.profile.ProfileResponse;
import com.lmf.finpro.infrastructure.web.dto.profile.UpdateProfileRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /** Foto de perfil (JPG, PNG ou WEBP, até 2 MB). Substitui a anterior, se houver. */
    @PutMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadPhoto(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new AttachmentInvalidException("Não foi possível ler o arquivo enviado.");
        }
        return toResponse(profileApplicationService.updatePhoto(currentUser.userId(), content));
    }

    @DeleteMapping("/photo")
    public ProfileResponse removePhoto(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return toResponse(profileApplicationService.removePhoto(currentUser.userId()));
    }

    /** Imagem do próprio usuário; {@code nosniff} impede o navegador de adivinhar outro tipo. */
    @GetMapping("/photo")
    public ResponseEntity<byte[]> photo(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        PhotoContent photo = profileApplicationService.getPhoto(currentUser.userId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(photo.content());
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
                user.hasPhoto(),
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
