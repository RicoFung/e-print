package com.eprint.admin.module.minio.template.service;

import com.eprint.admin.repository.minio.dao.MinioTemplateDao;
import com.eprint.admin.repository.minio.dao.MinioTemplateTypeDao;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MinioTemplateServiceTest {

    @Mock
    private MinioTemplateDao templateDao;
    @Mock
    private MinioTemplateTypeDao templateTypeDao;
    @Mock
    private MinioClient minioClient;

    private MinioTemplateService service;

    @BeforeEach
    void setUp() {
        service = new MinioTemplateService(templateDao, templateTypeDao, minioClient,
                "template-bucket", "templates/print");
    }

    @Test
    void previewRendersHandlebarsIfBlocksAndBusinessNamedCodeAssets() {
        String template = """
                {{#if codes.receiptBarcode}}<img class="barcode" src="{{codes.receiptBarcode.dataUrl}}">{{/if}}
                {{#if codes.memberQr}}<img class="qr" src="{{codes.memberQr.dataUrl}}">{{/if}}
                {{#if codes.missing}}missing{{/if}}
                """;
        String sampleData = """
                {
                  "codes": {
                    "receiptBarcode": {"codeType": "barcode", "value": "RC202609160001"},
                    "memberQr": {"codeType": "qr", "value": "https://example.com/member/001"}
                  }
                }
                """;

        String html = service.renderTemplateContent(template, sampleData);

        assertTrue(html.contains("class=\"barcode\" src=\"data:image/svg+xml;base64,"));
        assertTrue(html.contains("class=\"qr\" src=\"data:image/svg+xml;base64,"));
        assertFalse(html.contains("{{#if"));
        assertFalse(html.contains("missing"));
    }
}
