package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AttachmentFileTypeTest {

    @Test
    void detectsAcceptedFormatsByTheirFirstBytes() {
        assertThat(AttachmentFileType.detect("%PDF-1.7".getBytes(StandardCharsets.US_ASCII)))
                .contains(AttachmentFileType.PDF);
        assertThat(AttachmentFileType.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0)))
                .contains(AttachmentFileType.JPEG);
        assertThat(AttachmentFileType.detect(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
                .contains(AttachmentFileType.PNG);
        assertThat(
                        AttachmentFileType.detect(
                                "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII)))
                .contains(AttachmentFileType.WEBP);
    }

    @Test
    void rejectsOtherFormatsEvenWithAcceptedExtension() {
        // "MZ" = executável do Windows; ZIP; texto; e um RIFF que não é WEBP (WAV).
        assertThat(AttachmentFileType.detect(bytes(0x4D, 0x5A, 0x90, 0x00))).isEmpty();
        assertThat(AttachmentFileType.detect(bytes(0x50, 0x4B, 0x03, 0x04))).isEmpty();
        assertThat(AttachmentFileType.detect("hello".getBytes(StandardCharsets.US_ASCII)))
                .isEmpty();
        assertThat(
                        AttachmentFileType.detect(
                                "RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII)))
                .isEmpty();
        assertThat(AttachmentFileType.detect(new byte[0])).isEmpty();
    }

    @Test
    void findsTypeByContentType() {
        assertThat(AttachmentFileType.fromContentType("image/png"))
                .contains(AttachmentFileType.PNG);
        assertThat(AttachmentFileType.fromContentType("text/html")).isEmpty();
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }
}
