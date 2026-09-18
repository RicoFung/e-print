package com.eprint.server.module.qiniu.template.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class QiniuObjectStorage {

    private final S3Client s3Client;
    private final String bucket;

    @Autowired
    public QiniuObjectStorage(@Value("${qiniu.access-key:disabled}") String accessKey,
                              @Value("${qiniu.secret-key:disabled}") String secretKey,
                              @Value("${qiniu.bucket:pos-uat}") String bucket,
                              @Value("${qiniu.s3-endpoint:https://s3.cn-south-1.qiniucs.com}") String s3Endpoint,
                              @Value("${qiniu.s3-region:cn-south-1}") String s3Region) {
        this.s3Client = createS3Client(accessKey, secretKey, s3Endpoint, s3Region);
        this.bucket = bucket;
    }

    QiniuObjectStorage(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public String read(String bucketName, String objectName) {
        validateBucket(bucketName);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(normalizeObjectName(objectName))
                .build();
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asString(StandardCharsets.UTF_8);
    }

    @PreDestroy
    public void close() {
        s3Client.close();
    }

    private void validateBucket(String bucketName) {
        if (!bucket.equals(bucketName)) {
            throw new IllegalArgumentException("Qiniu bucket must be " + bucket);
        }
    }

    private static S3Client createS3Client(String accessKey,
                                           String secretKey,
                                           String endpoint,
                                           String region) {
        return S3Client.builder()
                .endpointOverride(endpointUri(endpoint))
                .region(software.amazon.awssdk.regions.Region.of(region.trim()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(15))
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .build())
                .build();
    }

    static URI endpointUri(String value) {
        String endpoint = value == null ? "" : value.trim();
        if (!endpoint.contains("://")) {
            endpoint = "https://" + endpoint;
        }
        URI uri = URI.create(endpoint);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getRawAuthority() == null || uri.getRawAuthority().isBlank()
                || (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Qiniu S3 endpoint is invalid");
        }
        return uri;
    }

    private static String normalizeObjectName(String objectName) {
        String result = objectName.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }
}
