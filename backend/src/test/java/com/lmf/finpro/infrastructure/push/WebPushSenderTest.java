package com.lmf.finpro.infrastructure.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.finpro.domain.model.PushMessage;
import com.lmf.finpro.domain.model.PushSubscription;
import com.lmf.finpro.domain.port.out.PushSenderPort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

class WebPushSenderTest {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    @Test
    void aceitaUmParDeChavesVapidValidoEExpoAChavePublica() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        String publicKey = B64.encodeToString(uncompressedPoint((ECPublicKey) pair.getPublic()));
        String privateKey = B64.encodeToString(scalar((ECPrivateKey) pair.getPrivate()));

        WebPushSender sender =
                new WebPushSender(
                        publicKey,
                        privateKey,
                        "mailto:a@b.c",
                        new ObjectMapper(),
                        new SimpleMeterRegistry());

        assertThat(sender.publicKey()).contains(publicKey);
    }

    @Test
    void chavesInvalidasFalhamNaSubida() {
        assertThatThrownBy(
                        () ->
                                new WebPushSender(
                                        "nao-e-chave",
                                        "nem-esta",
                                        "mailto:a@b.c",
                                        new ObjectMapper(),
                                        new SimpleMeterRegistry()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void semConfiguracaoOPushFicaDesligado() {
        NoopPushSender sender = new NoopPushSender();

        assertThat(sender.publicKey()).isEmpty();
        assertThat(
                        sender.send(
                                new PushSubscription(1L, "https://x", "p", "a"),
                                new PushMessage("t", "b", "/")))
                .isEqualTo(PushSenderPort.Result.FAILED);
    }

    private static byte[] uncompressedPoint(ECPublicKey key) {
        byte[] point = new byte[65];
        point[0] = 4;
        System.arraycopy(fixed32(key.getW().getAffineX().toByteArray()), 0, point, 1, 32);
        System.arraycopy(fixed32(key.getW().getAffineY().toByteArray()), 0, point, 33, 32);
        return point;
    }

    private static byte[] scalar(ECPrivateKey key) {
        return fixed32(key.getS().toByteArray());
    }

    /** BigInteger.toByteArray pode trazer um byte de sinal ou omitir zeros à esquerda. */
    private static byte[] fixed32(byte[] value) {
        byte[] out = new byte[32];
        int length = Math.min(value.length, 32);
        System.arraycopy(value, value.length - length, out, 32 - length, length);
        return out;
    }
}
