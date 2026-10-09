package com.lmf.finpro.infrastructure.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Criptografia dos anexos em repouso: AES-256-GCM. Formato do arquivo: {@code "FPE1"} (4 bytes) +
 * IV aleatório de 12 bytes + texto cifrado com a tag de autenticação. O nome do arquivo entra como
 * dado autenticado (AAD): trocar um arquivo cifrado por outro no disco faz a leitura falhar.
 *
 * <p>Os formatos aceitos nos anexos (PDF, JPG, PNG, WEBP) nunca começam com {@code FPE1}, então um
 * arquivo antigo, sem criptografia, é reconhecido pela ausência do prefixo.
 */
final class AttachmentCipher {

    private static final byte[] MAGIC = "FPE1".getBytes(StandardCharsets.US_ASCII);
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    AttachmentCipher(String base64Key) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "FINPRO_ATTACHMENTS_ENCRYPTION_KEY precisa estar em Base64 (32 bytes).", e);
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException(
                    "FINPRO_ATTACHMENTS_ENCRYPTION_KEY precisa ter 32 bytes (AES-256); gere com:"
                            + " openssl rand -base64 32");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    static boolean isEncrypted(byte[] stored) {
        return stored.length > MAGIC.length
                && Arrays.equals(Arrays.copyOf(stored, MAGIC.length), MAGIC);
    }

    byte[] encrypt(String fileKey, byte[] plain) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(fileKey.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plain);
            return ByteBuffer.allocate(MAGIC.length + IV_BYTES + encrypted.length)
                    .put(MAGIC)
                    .put(iv)
                    .put(encrypted)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar o anexo", e);
        }
    }

    byte[] decrypt(String fileKey, byte[] stored) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_BITS, stored, MAGIC.length, IV_BYTES));
            cipher.updateAAD(fileKey.getBytes(StandardCharsets.UTF_8));
            int offset = MAGIC.length + IV_BYTES;
            return cipher.doFinal(stored, offset, stored.length - offset);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Falha ao decifrar o anexo (chave errada ou arquivo adulterado)", e);
        }
    }
}
