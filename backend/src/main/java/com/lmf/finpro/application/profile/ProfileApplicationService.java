package com.lmf.finpro.application.profile;

import com.lmf.finpro.application.auth.AuthResult;
import com.lmf.finpro.application.auth.RefreshTokenApplicationService;
import com.lmf.finpro.domain.exception.AttachmentInvalidException;
import com.lmf.finpro.domain.exception.DocumentAlreadyInUseException;
import com.lmf.finpro.domain.exception.EmailAlreadyInUseException;
import com.lmf.finpro.domain.exception.IncorrectCurrentPasswordException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.AttachmentFileType;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.FileStoragePort;
import com.lmf.finpro.domain.port.out.PasswordHasherPort;
import com.lmf.finpro.domain.port.out.TokenPort;
import com.lmf.finpro.domain.port.out.UserRepositoryPort;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados cadastrais e senha do próprio usuário logado ("Meu perfil"). */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileApplicationService {

    public static final long MAX_PHOTO_SIZE_BYTES = 2L * 1024 * 1024;
    private static final Set<AttachmentFileType> PHOTO_TYPES =
            Set.of(AttachmentFileType.JPEG, AttachmentFileType.PNG, AttachmentFileType.WEBP);

    public record PhotoContent(String contentType, byte[] content) {}

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenPort tokenPort;
    private final RefreshTokenApplicationService refreshTokenApplicationService;
    private final FileStoragePort fileStoragePort;

    public User getProfile(Long userId) {
        log.debug("Buscando perfil do usuário={}", userId);
        return findUser(userId);
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileCommand command) {
        log.debug("Atualizando perfil do usuário={}", userId);
        User user = findUser(userId);

        boolean emailChanged = !user.email().equalsIgnoreCase(command.email());
        if (emailChanged) {
            // O e-mail é o login: sem a senha, quem pegasse uma sessão aberta tomaria a conta.
            checkCurrentPassword(
                    user,
                    command.currentPassword(),
                    "Informe sua senha atual para alterar o e-mail");
            if (userRepositoryPort.existsByEmail(command.email())) {
                throw new EmailAlreadyInUseException(
                        "Já existe uma conta cadastrada com este e-mail");
            }
        }

        String documentNumber = onlyDigits(command.documentNumber());
        if (!documentNumber.equals(user.documentNumber())
                && userRepositoryPort.existsByDocumentNumber(documentNumber)) {
            throw new DocumentAlreadyInUseException(
                    "Já existe uma conta cadastrada com este " + command.documentType());
        }

        return userRepositoryPort.save(
                user.withProfile(
                        command.name(),
                        command.email(),
                        command.documentType(),
                        documentNumber,
                        onlyDigits(command.phone()),
                        command.taxRegime(),
                        command.address().toDomain()));
    }

    /**
     * Define a foto de perfil. O formato (JPG, PNG ou WEBP) é reconhecido pelo conteúdo, nunca pelo
     * nome ou pelo tipo informado pelo navegador. Grava o arquivo novo antes de trocar o registro e
     * só então apaga o anterior, para nunca deixar o usuário com uma foto quebrada.
     */
    @Transactional
    public User updatePhoto(Long userId, byte[] content) {
        log.debug("Atualizando foto de perfil do usuário={}", userId);
        User user = findUser(userId);
        if (content == null || content.length == 0) {
            throw new AttachmentInvalidException("O arquivo enviado está vazio.");
        }
        if (content.length > MAX_PHOTO_SIZE_BYTES) {
            throw new AttachmentInvalidException("A foto é grande demais. O limite é de 2 MB.");
        }
        AttachmentFileType fileType =
                AttachmentFileType.detect(Arrays.copyOf(content, Math.min(content.length, 12)))
                        .filter(PHOTO_TYPES::contains)
                        .orElseThrow(
                                () ->
                                        new AttachmentInvalidException(
                                                "Formato não aceito. Envie uma imagem JPG, PNG ou"
                                                        + " WEBP."));

        String newKey = "profile-" + UUID.randomUUID() + "." + fileType.extension();
        fileStoragePort.store(newKey, content);
        String oldKey = user.photoKey();
        try {
            User saved = userRepositoryPort.save(user.withPhoto(newKey, fileType.contentType()));
            if (oldKey != null) {
                fileStoragePort.delete(oldKey);
            }
            return saved;
        } catch (RuntimeException e) {
            fileStoragePort.delete(newKey);
            throw e;
        }
    }

    @Transactional
    public User removePhoto(Long userId) {
        log.debug("Removendo foto de perfil do usuário={}", userId);
        User user = findUser(userId);
        if (!user.hasPhoto()) {
            return user;
        }
        String oldKey = user.photoKey();
        User saved = userRepositoryPort.save(user.withPhoto(null, null));
        fileStoragePort.delete(oldKey);
        return saved;
    }

    public PhotoContent getPhoto(Long userId) {
        log.debug("Buscando foto de perfil do usuário={}", userId);
        User user = findUser(userId);
        if (!user.hasPhoto()) {
            throw new ResourceNotFoundException("Você ainda não tem foto de perfil");
        }
        return new PhotoContent(user.photoContentType(), fileStoragePort.load(user.photoKey()));
    }

    /**
     * Troca a senha e encerra as outras sessões (ver User#withPasswordHash). Devolve um token novo
     * (e um refresh token novo) para a sessão de quem fez a troca continuar valendo.
     */
    @Transactional
    public AuthResult changePassword(Long userId, String currentPassword, String newPassword) {
        log.debug("Alterando senha do usuário={}", userId);
        User user = findUser(userId);
        checkCurrentPassword(user, currentPassword, "Senha atual incorreta");

        User saved =
                userRepositoryPort.save(
                        user.withPasswordHash(passwordHasherPort.hash(newPassword)));
        String token = tokenPort.generate(saved.id(), saved.email(), saved.sessionVersion());
        String refreshToken = refreshTokenApplicationService.startSession(saved);
        return new AuthResult(token, refreshToken, saved.id(), saved.name(), saved.email());
    }

    private void checkCurrentPassword(User user, String rawPassword, String blankMessage) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IncorrectCurrentPasswordException(blankMessage);
        }
        if (!passwordHasherPort.matches(rawPassword, user.passwordHash())) {
            throw new IncorrectCurrentPasswordException("Senha atual incorreta");
        }
    }

    private User findUser(Long userId) {
        return userRepositoryPort
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
