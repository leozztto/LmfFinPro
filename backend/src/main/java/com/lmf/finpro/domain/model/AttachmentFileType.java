package com.lmf.finpro.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos aceitos nos anexos. O formato é reconhecido pelos primeiros bytes do arquivo ("magic
 * number"), não pela extensão nem pelo Content-Type enviado — assim um executável renomeado para
 * .pdf é recusado, e o arquivo é sempre servido de volta com o tipo correto.
 */
public enum AttachmentFileType {
    PDF("application/pdf", "pdf"),
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String contentType;
    private final String extension;

    AttachmentFileType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<AttachmentFileType> fromContentType(String contentType) {
        return Arrays.stream(values())
                .filter(type -> type.contentType.equalsIgnoreCase(contentType))
                .findFirst();
    }

    /**
     * @param header os primeiros bytes do arquivo (12 bastam)
     */
    public static Optional<AttachmentFileType> detect(byte[] header) {
        if (startsWith(header, 0x25, 0x50, 0x44, 0x46)) { // %PDF
            return Optional.of(PDF);
        }
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        // RIFF....WEBP
        if (startsWith(header, 0x52, 0x49, 0x46, 0x46)
                && header.length >= 12
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int... signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
