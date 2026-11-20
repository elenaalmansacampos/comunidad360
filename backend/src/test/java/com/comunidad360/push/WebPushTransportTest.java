package com.comunidad360.push;

import com.comunidad360.config.AppProperties;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushTransportTest {

    @Test
    void isConfiguredWhenEnabledWithValidKeys() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                Utils.ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(new ECGenParameterSpec(Utils.CURVE));
        KeyPair keyPair = generator.generateKeyPair();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String publicKey = encoder.encodeToString(Utils.encode((ECPublicKey) keyPair.getPublic()));
        String privateKey = encoder.encodeToString(Utils.encode((ECPrivateKey) keyPair.getPrivate()));

        AppProperties properties = new AppProperties();
        properties.getPush().setEnabled(true);
        properties.getPush().setPublicKey(publicKey);
        properties.getPush().setPrivateKey(privateKey);
        properties.getPush().setSubject("mailto:test@example.com");

        assertThat(new WebPushTransport(properties).isConfigured()).isTrue();
    }

    @Test
    void isNotConfiguredWhenDisabledOrMissingKeys() {
        AppProperties disabled = new AppProperties();
        disabled.getPush().setEnabled(false);
        disabled.getPush().setPublicKey("x");
        disabled.getPush().setPrivateKey("y");
        assertThat(new WebPushTransport(disabled).isConfigured()).isFalse();

        AppProperties missingKeys = new AppProperties();
        missingKeys.getPush().setEnabled(true);
        assertThat(new WebPushTransport(missingKeys).isConfigured()).isFalse();
    }
}
