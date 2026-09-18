package com.eprint.admin.module.qiniu.template.service;

import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateCreateRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateDisableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateModifyRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateRemoveRequest;
import com.eprint.admin.repository.qiniu.dao.QiniuTemplateDao;
import com.eprint.admin.repository.qiniu.dao.QiniuTemplateTypeDao;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplate;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplateType;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateRemoveParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QiniuTemplateServiceTest {

    @Mock
    private QiniuTemplateDao templateDao;
    @Mock
    private QiniuTemplateTypeDao templateTypeDao;
    @Mock
    private QiniuObjectStorage objectStorage;

    private QiniuTemplateService service;

    @BeforeEach
    void setUp() {
        service = new QiniuTemplateService(templateDao, templateTypeDao, objectStorage,
                "template-bucket", "e-print", "templates/print");
    }

    @Test
    void createDoesNotWriteDatabaseWhenQiniuUploadFails() throws Exception {
        QiniuTemplateCreateRequest request = createRequest();
        when(templateTypeDao.get("type-1")).thenReturn(enabledType());
        doThrow(new IllegalStateException("qiniu unavailable"))
                .when(objectStorage).put("template-bucket", "e-print/templates/print/sales/T001.html", "<html/>");

        assertThrows(IllegalStateException.class, () -> service.create(request));

        verify(templateDao, never()).create(any());
    }

    @Test
    void removeDoesNotWriteDatabaseWhenQiniuDeleteFails() throws Exception {
        QiniuTemplateRemoveRequest request = new QiniuTemplateRemoveRequest();
        request.setIds(List.of("1"));
        when(templateDao.get("1")).thenReturn(template("1", "e-print/old.html"));
        doThrow(new IllegalStateException("qiniu unavailable"))
                .when(objectStorage).delete("template-bucket", "e-print/old.html");

        assertThrows(IllegalStateException.class, () -> service.remove(request));

        verify(templateDao, never()).remove(any(QiniuTemplateRemoveParam.class));
    }

    @Test
    void modifyDoesNotWriteDatabaseWhenQiniuUploadFails() throws Exception {
        QiniuTemplateModifyRequest request = modifyRequest("e-print/old.html");
        when(templateDao.get("1")).thenReturn(template("1", "e-print/old.html"));
        when(templateTypeDao.get("type-1")).thenReturn(enabledType());
        doThrow(new IllegalStateException("qiniu unavailable"))
                .when(objectStorage).overwrite("template-bucket", "e-print/old.html", "<html/>");

        assertThrows(IllegalStateException.class, () -> service.modify(request));

        verify(templateDao, never()).modify(any());
    }

    @Test
    void modifyDeletesPreviousObjectBeforeWritingDatabaseWhenObjectNameChanges() throws Exception {
        QiniuTemplateModifyRequest request = modifyRequest("e-print/new.html");
        when(templateDao.get("1")).thenReturn(template("1", "e-print/old.html"));
        when(templateTypeDao.get("type-1")).thenReturn(enabledType());

        service.modify(request);

        verify(objectStorage).overwrite("template-bucket", "e-print/new.html", "<html/>");
        verify(objectStorage).delete("template-bucket", "e-print/old.html");
        verify(templateDao).modify(any());
    }

    @Test
    void modifyDoesNotWriteDatabaseWhenDeletingPreviousQiniuObjectFails() throws Exception {
        QiniuTemplateModifyRequest request = modifyRequest("e-print/new.html");
        when(templateDao.get("1")).thenReturn(template("1", "e-print/old.html"));
        when(templateTypeDao.get("type-1")).thenReturn(enabledType());
        doThrow(new IllegalStateException("qiniu unavailable"))
                .when(objectStorage).delete("template-bucket", "e-print/old.html");

        assertThrows(IllegalStateException.class, () -> service.modify(request));

        verify(templateDao, never()).modify(any());
    }

    @Test
    void disableDoesNotWriteDatabaseWhenQiniuStatFails() throws Exception {
        QiniuTemplateDisableRequest request = new QiniuTemplateDisableRequest();
        request.setIds(List.of("1"));
        when(templateDao.get("1")).thenReturn(template("1", "stored.html"));
        doThrow(new IllegalStateException("qiniu unavailable"))
                .when(objectStorage).stat("template-bucket", "stored.html");

        assertThrows(IllegalStateException.class, () -> service.disable(request));

        verify(templateDao, never()).disable(any());
    }

    @Test
    void createStoresCompleteQiniuObjectKey() throws Exception {
        QiniuTemplateCreateRequest request = createRequest();
        when(templateTypeDao.get("type-1")).thenReturn(enabledType());

        service.create(request);

        verify(objectStorage).put("template-bucket", "e-print/templates/print/sales/T001.html", "<html/>");
        verify(templateDao).create(org.mockito.ArgumentMatchers.argThat(param ->
                "e-print/templates/print/sales/T001.html".equals(param.getObjectName())));
    }

    @Test
    void removeRejectsObjectOutsideConfiguredPrefixBeforeCallingQiniu() throws Exception {
        QiniuTemplateRemoveRequest request = new QiniuTemplateRemoveRequest();
        request.setIds(List.of("1"));
        when(templateDao.get("1")).thenReturn(template("1", "other-app/template.html"));

        assertThrows(IllegalArgumentException.class, () -> service.remove(request));

        verify(objectStorage, never()).stat(any(), any());
        verify(objectStorage, never()).delete(any(), any());
        verify(templateDao, never()).remove(any(QiniuTemplateRemoveParam.class));
    }

    @Test
    void batchRemoveStatsEveryObjectBeforeDeletingAnyObject() throws Exception {
        QiniuTemplateRemoveRequest request = new QiniuTemplateRemoveRequest();
        request.setIds(List.of("1", "2"));
        when(templateDao.get("1")).thenReturn(template("1", "e-print/one.html"));
        when(templateDao.get("2")).thenReturn(template("2", "e-print/two.html"));

        service.remove(request);

        InOrder order = inOrder(objectStorage, templateDao);
        order.verify(objectStorage).stat("template-bucket", "e-print/one.html");
        order.verify(objectStorage).stat("template-bucket", "e-print/two.html");
        order.verify(objectStorage).delete("template-bucket", "e-print/one.html");
        order.verify(objectStorage).delete("template-bucket", "e-print/two.html");
        order.verify(templateDao).remove(any(QiniuTemplateRemoveParam.class));
    }

    @Test
    void createRejectsObjectKeyUsedByAnotherTemplate() throws Exception {
        QiniuTemplateCreateRequest request = createRequest();
        when(templateTypeDao.get("type-1")).thenReturn(enabledType());
        when(templateDao.getByBucketNameAndObjectName(
                "template-bucket", "e-print/templates/print/sales/T001.html"))
                .thenReturn(template("2", "e-print/templates/print/sales/T001.html"));

        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        verify(objectStorage, never()).put(any(), any(), any());
        verify(templateDao, never()).create(any());
    }

    @Test
    void modifyRejectsObjectKeyUsedByAnotherTemplateBeforeOverwrite() throws Exception {
        QiniuTemplateModifyRequest request = modifyRequest("e-print/new.html");
        when(templateDao.get("1")).thenReturn(template("1", "e-print/old.html"));
        when(templateTypeDao.get("type-1")).thenReturn(enabledType());
        when(templateDao.getByBucketNameAndObjectName("template-bucket", "e-print/new.html"))
                .thenReturn(template("2", "e-print/new.html"));

        assertThrows(IllegalArgumentException.class, () -> service.modify(request));

        verify(objectStorage, never()).overwrite(any(), any(), any());
        verify(objectStorage, never()).delete(any(), any());
        verify(templateDao, never()).modify(any());
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

    private QiniuTemplateCreateRequest createRequest() {
        QiniuTemplateCreateRequest request = new QiniuTemplateCreateRequest();
        request.setTemplateTypeId("type-1");
        request.setTemplateCode("T001");
        request.setBucketName("template-bucket");
        request.setStatus(1);
        request.setContent("<html/>");
        return request;
    }

    private QiniuTemplateModifyRequest modifyRequest(String objectName) {
        QiniuTemplateModifyRequest request = new QiniuTemplateModifyRequest();
        request.setId("1");
        request.setTemplateTypeId("type-1");
        request.setTemplateCode("T001");
        request.setBucketName("template-bucket");
        request.setObjectName(objectName);
        request.setStatus(1);
        request.setContent("<html/>");
        return request;
    }

    private QiniuTemplate template(String id, String objectName) {
        QiniuTemplate template = new QiniuTemplate();
        template.setId(id);
        template.setTemplateTypeId("type-1");
        template.setTemplateCode("T001");
        template.setBucketName("template-bucket");
        template.setObjectName(objectName);
        template.setStatus(1);
        return template;
    }

    private QiniuTemplateType enabledType() {
        QiniuTemplateType type = new QiniuTemplateType();
        type.setId("type-1");
        type.setCode("sales");
        type.setStatus(1);
        return type;
    }
}
