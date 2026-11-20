package com.comunidad360.push;

import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public final class VapidKeys {

    private VapidKeys() {
    }

    public static void main(String[] args) throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                Utils.ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        generator.initialize(new ECGenParameterSpec(Utils.CURVE), new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        byte[] publicBytes = Utils.encode((ECPublicKey) keyPair.getPublic());
        byte[] privateBytes = normalize(Utils.encode((ECPrivateKey) keyPair.getPrivate()));
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        System.out.println("APP_PUSH_ENABLED=true");
        System.out.println("APP_PUSH_PUBLIC_KEY=" + encoder.encodeToString(publicBytes));
        System.out.println("APP_PUSH_PRIVATE_KEY=" + encoder.encodeToString(privateBytes));
        System.out.println("APP_PUSH_SUBJECT=mailto:soporte@example.com");
    }

    private static byte[] normalize(byte[] value) {
        if (value.length == 32) {
            return value;
        }
        byte[] result = new byte[32];
        if (value.length > 32) {
            System.arraycopy(value, value.length - 32, result, 0, 32);
        } else {
            System.arraycopy(value, 0, result, 32 - value.length, value.length);
        }
        return result;
    }
}
