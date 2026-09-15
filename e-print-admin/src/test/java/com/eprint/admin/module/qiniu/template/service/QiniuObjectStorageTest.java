package com.eprint.admin.module.qiniu.template.service;

import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QiniuObjectStorageTest {

    @Test
    void springSelectsConfiguredConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(QiniuObjectStorage.class);
            context.refresh();

            assertNotNull(context.getBean(QiniuObjectStorage.class));
        }
    }

    @Test
    void readsCompleteObjectKeyThroughS3Api() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket("pos-uat")
                .key("e-print/templates/print/sales_receipt/01.html")
                .build();
        when(s3Client.getObjectAsBytes(request)).thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(), "<html/>".getBytes(StandardCharsets.UTF_8)));
        QiniuObjectStorage storage = storage(s3Client);

        String content = storage.read(
                "pos-uat", "/e-print/templates/print/sales_receipt/01.html");

        assertEquals("<html/>", content);
        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(requestCaptor.capture());
        assertEquals("pos-uat", requestCaptor.getValue().bucket());
        assertEquals("e-print/templates/print/sales_receipt/01.html", requestCaptor.getValue().key());
    }

    @Test
    void rejectsInvalidS3Endpoint() {
        assertThrows(IllegalArgumentException.class, () -> new QiniuObjectStorage(
                "access-key", "secret-key", "z2", "pos-uat", "e-print",
                "https://example.com/not-an-endpoint", "cn-south-1", "pos-uat"));
    }

    @Test
    void rejectsDeleteOutsideConfiguredObjectPrefix() throws Exception {
        BucketManager bucketManager = mock(BucketManager.class);
        QiniuObjectStorage storage = storage(mock(S3Client.class), bucketManager);

        assertThrows(IllegalArgumentException.class,
                () -> storage.delete("pos-uat", "templates/print/sales_receipt/01.html"));

        verify(bucketManager, Mockito.never()).delete(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void deletesExactCompleteObjectKey() throws Exception {
        BucketManager bucketManager = mock(BucketManager.class);
        QiniuObjectStorage storage = storage(mock(S3Client.class), bucketManager);

        storage.delete("pos-uat", "/e-print/templates/print/sales_receipt/01.html");

        verify(bucketManager).delete("pos-uat", "e-print/templates/print/sales_receipt/01.html");
    }

    private QiniuObjectStorage storage(S3Client s3Client) {
        return storage(s3Client, mock(BucketManager.class));
    }

    private QiniuObjectStorage storage(S3Client s3Client, BucketManager bucketManager) {
        return new QiniuObjectStorage(
                mock(UploadManager.class),
                bucketManager,
                Auth.create("access-key", "secret-key"),
                s3Client,
                "pos-uat",
                "e-print",
                "pos-uat");
    }
}
