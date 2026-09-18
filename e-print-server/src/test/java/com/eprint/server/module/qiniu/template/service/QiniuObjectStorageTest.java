package com.eprint.server.module.qiniu.template.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QiniuObjectStorageTest {

    @Test
    void createsBeanWithConfiguredConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("qiniu-test", Map.of(
                    "qiniu.access-key", "access-key",
                    "qiniu.secret-key", "secret-key",
                    "qiniu.bucket", "template-bucket",
                    "qiniu.s3-endpoint", "https://s3.cn-south-1.qiniucs.com",
                    "qiniu.s3-region", "cn-south-1")));
            context.register(QiniuObjectStorage.class);
            context.refresh();

            assertNotNull(context.getBean(QiniuObjectStorage.class));
        }
    }

    @Test
    void readsTemplateThroughS3Client() {
        AtomicReference<GetObjectRequest> requestReference = new AtomicReference<>();
        S3Client s3Client = s3Client(requestReference, "<html>template</html>");
        QiniuObjectStorage storage = new QiniuObjectStorage(s3Client, "template-bucket");

        assertEquals("<html>template</html>", storage.read(
                "template-bucket", "/e-print/templates/print/sales_receipt/default.html"));
        assertEquals("template-bucket", requestReference.get().bucket());
        assertEquals("e-print/templates/print/sales_receipt/default.html", requestReference.get().key());
    }

    @Test
    void rejectsUnexpectedBucket() {
        QiniuObjectStorage storage = new QiniuObjectStorage(s3Client(new AtomicReference<>(), ""), "template-bucket");

        assertThrows(IllegalArgumentException.class,
                () -> storage.read("other-bucket", "e-print/template.html"));
    }

    @Test
    void rejectsS3EndpointWithPath() {
        assertThrows(IllegalArgumentException.class,
                () -> QiniuObjectStorage.endpointUri("https://s3.example.com/path"));
    }

    private S3Client s3Client(AtomicReference<GetObjectRequest> requestReference, String content) {
        return (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[]{S3Client.class},
                (proxy, method, args) -> {
                    if ("getObjectAsBytes".equals(method.getName())) {
                        requestReference.set((GetObjectRequest) args[0]);
                        return ResponseBytes.fromByteArray(
                                GetObjectResponse.builder().build(),
                                content.getBytes(StandardCharsets.UTF_8));
                    }
                    if ("serviceName".equals(method.getName())) {
                        return "s3";
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
