package com.comunidad360.push;

public interface PushTransport {

    boolean isConfigured();

    PushResult send(String endpoint, String p256dh, String auth, byte[] payload);

    record PushResult(boolean sent, boolean gone, String errorCode) {

        public static PushResult success() {
            return new PushResult(true, false, null);
        }

        public static PushResult goneResult() {
            return new PushResult(false, true, "PUSH_GONE");
        }

        public static PushResult failure(String errorCode) {
            return new PushResult(false, false, errorCode);
        }
    }
}
