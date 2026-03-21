package com.example.springbootjavarefresh.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class S3CompatibleObjectStorageService implements ObjectStorageService {

    private final String bucket;
    private final Duration signedUrlTtl;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3CompatibleObjectStorageService(
            @Value("${app.fss.endpoint}") String endpoint,
            @Value("${app.fss.public-endpoint}") String publicEndpoint,
            @Value("${app.fss.region}") String region,
            @Value("${app.fss.access-key}") String accessKey,
            @Value("${app.fss.secret-key}") String secretKey,
            @Value("${app.fss.bucket}") String bucket,
            @Value("${app.fss.signed-url-ttl-minutes}") long signedUrlTtlMinutes) {
        this.bucket = bucket;
        this.signedUrlTtl = Duration.ofMinutes(signedUrlTtlMinutes);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3Configuration s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Configuration)
                .build();
        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Configuration)
                .build();
    }

    @PostConstruct
    void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ignored) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (Exception exception) {
            // MinIO returns generic exceptions during the first start window; a later upload will retry naturally.
        }
    }

    @Override
    public void upload(String objectKey, byte[] payload, String contentType) {
        ensureBucket();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(payload)
        );
    }

    @Override
    public SignedObjectUrl signGetUrl(String objectKey) {
        PresignedGetObjectRequest request = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(signedUrlTtl)
                        .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
                        .build()
        );
        return new SignedObjectUrl(
                request.url().toString(),
                LocalDateTime.ofInstant(request.expiration(), ZoneOffset.UTC)
        );
    }
}
