package com.comunidad360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String defaultLanguage = "es";
    private List<String> supportedLanguages = new ArrayList<>(List.of("es", "ca"));
    private Cors cors = new Cors();
    private Security security = new Security();
    private Seed seed = new Seed();
    private Structure structure = new Structure();
    private Bank bank = new Bank();
    private Mail mail = new Mail();
    private RateLimit rateLimit = new RateLimit();
    private Storage storage = new Storage();

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    public void setSupportedLanguages(List<String> supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Seed getSeed() {
        return seed;
    }

    public void setSeed(Seed seed) {
        this.seed = seed;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public Bank getBank() {
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Security {
        private String jwtSecret;
        private long accessTokenTtlSeconds = 900;
        private long refreshTokenTtlDays = 30;
        private boolean cookieSecure = false;
        private String cookieSameSite = "Lax";
        private boolean exposeDevTokens = false;

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public long getAccessTokenTtlSeconds() {
            return accessTokenTtlSeconds;
        }

        public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
            this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        }

        public long getRefreshTokenTtlDays() {
            return refreshTokenTtlDays;
        }

        public void setRefreshTokenTtlDays(long refreshTokenTtlDays) {
            this.refreshTokenTtlDays = refreshTokenTtlDays;
        }

        public boolean isCookieSecure() {
            return cookieSecure;
        }

        public void setCookieSecure(boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }

        public String getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(String cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }

        public boolean isExposeDevTokens() {
            return exposeDevTokens;
        }

        public void setExposeDevTokens(boolean exposeDevTokens) {
            this.exposeDevTokens = exposeDevTokens;
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int authPerMinute = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getAuthPerMinute() {
            return authPerMinute;
        }

        public void setAuthPerMinute(int authPerMinute) {
            this.authPerMinute = authPerMinute;
        }
    }

    public static class Mail {
        private boolean enabled = false;
        private String from = "no-reply@comunidad360.local";
        private String fromName = "Comunidad 360";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }
    }

    public static class Storage {
        private String type = "local";
        private String localPath = "./data/uploads";
        private long maxIncidentPhotoBytes = 5_242_880;
        private long maxDocumentBytes = 26_214_400;
        private S3 s3 = new S3();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocalPath() {
            return localPath;
        }

        public void setLocalPath(String localPath) {
            this.localPath = localPath;
        }

        public long getMaxIncidentPhotoBytes() {
            return maxIncidentPhotoBytes;
        }

        public void setMaxIncidentPhotoBytes(long maxIncidentPhotoBytes) {
            this.maxIncidentPhotoBytes = maxIncidentPhotoBytes;
        }

        public long getMaxDocumentBytes() {
            return maxDocumentBytes;
        }

        public void setMaxDocumentBytes(long maxDocumentBytes) {
            this.maxDocumentBytes = maxDocumentBytes;
        }

        public S3 getS3() {
            return s3;
        }

        public void setS3(S3 s3) {
            this.s3 = s3;
        }
    }

    public static class S3 {
        private String endpoint = "http://localhost:9090";
        private String publicEndpoint;
        private String region = "us-east-1";
        private String bucket = "comunidad360-documents";
        private String accessKey = "test";
        private String secretKey = "test";
        private boolean pathStyle = true;
        private long signedUrlTtlSeconds = 300;
        private boolean createBucket = false;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getPublicEndpoint() {
            return publicEndpoint;
        }

        public void setPublicEndpoint(String publicEndpoint) {
            this.publicEndpoint = publicEndpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isPathStyle() {
            return pathStyle;
        }

        public void setPathStyle(boolean pathStyle) {
            this.pathStyle = pathStyle;
        }

        public long getSignedUrlTtlSeconds() {
            return signedUrlTtlSeconds;
        }

        public void setSignedUrlTtlSeconds(long signedUrlTtlSeconds) {
            this.signedUrlTtlSeconds = signedUrlTtlSeconds;
        }

        public boolean isCreateBucket() {
            return createBucket;
        }

        public void setCreateBucket(boolean createBucket) {
            this.createBucket = createBucket;
        }
    }

    public static class Bank {
        private int importMaxRows = 10000;

        public int getImportMaxRows() {
            return importMaxRows;
        }

        public void setImportMaxRows(int importMaxRows) {
            this.importMaxRows = importMaxRows;
        }
    }

    public static class Structure {
        private java.math.BigDecimal coefficientTolerance = new java.math.BigDecimal("0.0001");

        public java.math.BigDecimal getCoefficientTolerance() {
            return coefficientTolerance;
        }

        public void setCoefficientTolerance(java.math.BigDecimal coefficientTolerance) {
            this.coefficientTolerance = coefficientTolerance;
        }
    }

    public static class Seed {
        private boolean enabled = false;
        private String adminEmail = "admin@comunidad360.local";
        private String adminPassword = "Admin1234!";
        private String communityName = "Comunidad Demo";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAdminEmail() {
            return adminEmail;
        }

        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public String getCommunityName() {
            return communityName;
        }

        public void setCommunityName(String communityName) {
            this.communityName = communityName;
        }
    }
}
