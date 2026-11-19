package com.comunidad360.storage;

import com.comunidad360.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final AppProperties.S3 s3;

    public S3StorageService(S3Client s3Client, S3Presigner presigner, AppProperties properties) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.s3 = properties.getStorage().getS3();
    }

    @Override
    public String store(byte[] content, String extension, String contentType) {
        ensureBucket();
        String key = UUID.randomUUID() + "." + extension;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(s3.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content)
        );
        return key;
    }

    @Override
    public byte[] read(String key) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(s3.getBucket())
                        .key(key)
                        .build())
                .asByteArray();
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3.getBucket())
                .key(key)
                .build());
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        Duration duration = ttl == null || ttl.isNegative() || ttl.isZero()
                ? Duration.ofSeconds(s3.getSignedUrlTtlSeconds())
                : ttl;
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(s3.getBucket())
                        .key(key)
                        .build())
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    @Override
    public boolean supportsPresignedUrls() {
        return true;
    }

    private void ensureBucket() {
        if (!s3.isCreateBucket()) {
            return;
        }
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(s3.getBucket()).build());
        } catch (NoSuchBucketException exception) {
            createBucket();
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                createBucket();
            } else {
                throw exception;
            }
        }
    }

    private void createBucket() {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(s3.getBucket()).build());
    }
}
