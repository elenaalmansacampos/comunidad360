package com.comunidad360.push;

import com.comunidad360.config.AppProperties;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Security;

@Component
public class WebPushTransport implements PushTransport {

    private static final Logger log = LoggerFactory.getLogger(WebPushTransport.class);

    private final PushService pushService;
    private final boolean configured;

    public WebPushTransport(AppProperties properties) {
        AppProperties.Push push = properties.getPush();
        boolean hasKeys = push.getPublicKey() != null && !push.getPublicKey().isBlank()
                && push.getPrivateKey() != null && !push.getPrivateKey().isBlank();
        if (!push.isEnabled()) {
            this.pushService = null;
            this.configured = false;
            return;
        }
        if (!hasKeys) {
            log.warn("Web Push está activado pero faltan APP_PUSH_PUBLIC_KEY/APP_PUSH_PRIVATE_KEY; se desactiva");
            this.pushService = null;
            this.configured = false;
            return;
        }
        PushService service = null;
        boolean ok = false;
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            service = new PushService(push.getPublicKey().trim(), push.getPrivateKey().trim(), push.getSubject());
            ok = true;
            log.info("Web Push configurado");
        } catch (Exception exception) {
            log.error("No se pudo inicializar Web Push: {}", exception.toString());
        }
        this.pushService = service;
        this.configured = ok;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public PushResult send(String endpoint, String p256dh, String auth, byte[] payload) {
        if (!configured || pushService == null) {
            return PushResult.failure("PUSH_DISABLED");
        }
        try {
            Notification notification = new Notification(endpoint, p256dh, auth, payload);
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                return PushResult.success();
            }
            if (status == 404 || status == 410) {
                return PushResult.goneResult();
            }
            return PushResult.failure("PUSH_HTTP_" + status);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return PushResult.failure("PUSH_INTERRUPTED");
        } catch (Exception exception) {
            log.warn("Fallo enviando push: {}", exception.toString());
            return PushResult.failure("PUSH_SEND_FAILED");
        }
    }
}
