package com.eprint.admin.module.qiniu.template.service;

import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
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

    private final UploadManager uploadManager;
    private final BucketManager bucketManager;
    private final Auth auth;
    private final S3Client s3Client;
    private final String bucket;
    private final String objectPrefix;
    private final String s3Bucket;

    @Autowired
    public QiniuObjectStorage(@Value("${qiniu.access-key:disabled}") String accessKey,
                              @Value("${qiniu.secret-key:disabled}") String secretKey,
                              @Value("${qiniu.region:z2}") String region,
                              @Value("${qiniu.bucket:pos-uat}") String bucket,
                              @Value("${qiniu.object-prefix:e-print}") String objectPrefix,
                              @Value("${qiniu.s3-endpoint:https://s3.cn-south-1.qiniucs.com}") String s3Endpoint,
                              @Value("${qiniu.s3-region:cn-south-1}") String s3Region,
                              @Value("${qiniu.s3-bucket:${qiniu.bucket:pos-uat}}") String s3Bucket) {
        this.auth = Auth.create(accessKey, secretKey);
        Configuration configuration = Configuration.create(resolveRegion(region));
        this.uploadManager = new UploadManager(configuration);
        this.bucketManager = new BucketManager(auth, configuration);
        this.s3Client = createS3Client(accessKey, secretKey, s3Endpoint, s3Region);
        this.bucket = bucket;
        this.objectPrefix = normalizeObjectPrefix(objectPrefix);
        this.s3Bucket = s3Bucket.trim();
    }

    QiniuObjectStorage(UploadManager uploadManager,
                       BucketManager bucketManager,
                       Auth auth,
                       S3Client s3Client,
                       String bucket,
                       String objectPrefix,
                       String s3Bucket) {
        this.uploadManager = uploadManager;
        this.bucketManager = bucketManager;
        this.auth = auth;
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.objectPrefix = normalizeObjectPrefix(objectPrefix);
        this.s3Bucket = s3Bucket;
    }

    public void put(String bucketName, String objectName, String content) throws Exception {
        validateBucket(bucketName);
        String normalizedObjectName = normalizeObjectName(objectName);
        upload(normalizedObjectName, content, auth.uploadToken(bucketName));
    }

    public void overwrite(String bucketName, String objectName, String content) throws Exception {
        validateBucket(bucketName);
        String normalizedObjectName = normalizeObjectName(objectName);
        upload(normalizedObjectName, content, auth.uploadToken(bucketName, normalizedObjectName));
    }

    private void upload(String objectName, String content, String uploadToken) throws Exception {
        Response response = uploadManager.put(content.getBytes(StandardCharsets.UTF_8), objectName, uploadToken);
        try {
            if (!response.isOK()) {
                throw new IllegalStateException("Qiniu upload failed: HTTP " + response.statusCode);
            }
            DefaultPutRet putRet = response.jsonToObject(DefaultPutRet.class);
            if (putRet == null || !objectName.equals(putRet.key)) {
                throw new IllegalStateException("Qiniu upload returned an unexpected object key");
            }
        } finally {
            response.close();
        }
    }

    public String read(String bucketName, String objectName) throws Exception {
        validateBucket(bucketName);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(normalizeObjectName(objectName))
                .build();
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asString(StandardCharsets.UTF_8);
    }

    public void stat(String bucketName, String objectName) throws Exception {
        validateBucket(bucketName);
        bucketManager.stat(bucketName, normalizeObjectName(objectName));
    }

    public void delete(String bucketName, String objectName) throws Exception {
        String normalizedObjectName = validateDeleteTarget(bucketName, objectName);
        bucketManager.delete(bucketName, normalizedObjectName);
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

    String validateDeleteTarget(String bucketName, String objectName) {
        validateBucket(bucketName);
        String normalizedObjectName = normalizeObjectName(objectName);
        if (!normalizedObjectName.startsWith(objectPrefix + "/")) {
            throw new IllegalArgumentException("Qiniu delete object must be under " + objectPrefix + "/");
        }
        return normalizedObjectName;
    }

    private static Region resolveRegion(String region) {
        return switch (region == null ? "z2" : region.trim().toLowerCase()) {
            case "z0", "cn-east-1" -> Region.region0();
            case "cn-east-2" -> Region.regionCnEast2();
            case "z1", "cn-north-1" -> Region.region1();
            case "z2", "cn-south-1" -> Region.region2();
            case "na0", "us-north-1" -> Region.regionNa0();
            case "as0", "ap-southeast-1" -> Region.regionAs0();
            case "", "auto" -> Region.autoRegion();
            default -> Region.createWithRegionId(region.trim());
        };
    }

    private static S3Client createS3Client(String accessKey,
                                           String secretKey,
                                           String endpoint,
                                           String region) {
        URI endpointUri = endpointUri(endpoint);
        return S3Client.builder()
                .endpointOverride(endpointUri)
                .region(software.amazon.awssdk.regions.Region.of(region.trim()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(15))
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .build())
                .build();
    }

    private static URI endpointUri(String value) {
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
        return trimLeadingSlashes(objectName.trim());
    }

    private static String normalizeObjectPrefix(String objectPrefix) {
        String normalizedObjectPrefix = trimLeadingSlashes(objectPrefix == null ? "" : objectPrefix.trim());
        while (normalizedObjectPrefix.endsWith("/")) {
            normalizedObjectPrefix = normalizedObjectPrefix.substring(0, normalizedObjectPrefix.length() - 1);
        }
        if (normalizedObjectPrefix.isEmpty()) {
            throw new IllegalArgumentException("Qiniu object prefix must not be empty");
        }
        return normalizedObjectPrefix;
    }

    private static String trimLeadingSlashes(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }
}
