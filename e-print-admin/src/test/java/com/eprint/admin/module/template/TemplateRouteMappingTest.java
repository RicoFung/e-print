package com.eprint.admin.module.template;

import com.eprint.admin.module.minio.template.controller.MinioTemplateController;
import com.eprint.admin.module.minio.template.controller.MinioTemplateTypeController;
import com.eprint.admin.module.qiniu.template.controller.QiniuTemplateController;
import com.eprint.admin.module.qiniu.template.controller.QiniuTemplateTypeController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateRouteMappingTest {

    @Test
    void providerControllersUseSeparatedRoutes() {
        assertRoute(MinioTemplateController.class, "/minio/templates");
        assertRoute(MinioTemplateTypeController.class, "/minio/template-types");
        assertRoute(QiniuTemplateController.class, "/qiniu/templates");
        assertRoute(QiniuTemplateTypeController.class, "/qiniu/template-types");
    }

    @Test
    void navigationGroupsQiniuBeforeMinio() throws IOException {
        String layout = resource("templates/layout.html");
        String normalizedLayout = layout.toLowerCase(Locale.ROOT);
        int qiniuGroup = normalizedLayout.indexOf("<p>qiniu ");
        int minioGroup = normalizedLayout.indexOf("<p>minio ");

        assertTrue(qiniuGroup >= 0);
        assertTrue(minioGroup > qiniuGroup);
        assertTrue(layout.contains("@{/qiniu/template-types}"));
        assertTrue(layout.contains("@{/qiniu/templates}"));
        assertTrue(layout.contains("@{/minio/template-types}"));
        assertTrue(layout.contains("@{/minio/templates}"));
    }

    @Test
    void navigationUsesUnifiedStorageIcons() throws IOException {
        String layout = resource("templates/layout.html");

        assertTrue(layout.contains("id=\"admin-icon-storage\""));
        assertTrue(layout.contains("<svg><use href=\"#admin-icon-storage\"></use></svg>"));
        assertFalse(layout.contains("@{/img/providers/qiniu.png}"));
        assertFalse(layout.contains("@{/img/providers/minio.png}"));
    }

    @Test
    void navigationProvidesAccessibleInteractiveProviderGroups() throws IOException {
        String layout = resource("templates/layout.html");
        String navigationScript = resource("static/js/admin-theme.js");

        assertTrue(layout.contains("data-accordion=\"true\""));
        assertTrue(layout.contains("data-provider-toggle"));
        assertTrue(layout.contains("aria-controls='qiniu-navigation'"));
        assertTrue(layout.contains("aria-controls='minio-navigation'"));
        assertTrue(layout.contains("aria-current=${"));
        assertTrue(navigationScript.contains("initializeNavigation()"));
        assertTrue(navigationScript.contains("toggle.setAttribute('aria-expanded'"));
    }

    @Test
    void qiniuHasIndependentMaintenanceViews() throws IOException {
        assertQiniuView("template-type/query.html");
        assertQiniuView("template-type/create.html");
        assertQiniuView("template-type/modify.html");
        assertQiniuView("template/query.html");
        assertQiniuView("template/create.html");
        assertQiniuView("template/modify.html");
        assertQiniuView("template/preview.html");
        assertQiniuView("template/preview-modal.html");
    }

    @Test
    void providerTemplateStorageFieldsAreReadonlyAndObjectNameIsAutoManaged() throws IOException {
        assertTemplateStorageFields("templates/qiniu/template/create.html");
        assertTemplateStorageFields("templates/qiniu/template/modify.html");
        assertTemplateStorageFields("templates/minio/template/create.html");
        assertTemplateStorageFields("templates/minio/template/modify.html");

        String script = resource("static/js/template-form.js");
        assertTrue(script.contains("objectName.hasAttribute('data-object-name-auto')"));
        assertTrue(script.contains("templateCode.addEventListener('input', syncObjectName)"));
        assertTrue(script.contains("templateType.addEventListener('change', syncObjectName)"));
    }

    @Test
    void providerListsEnableResizableColumns() throws IOException {
        assertResizableList("templates/minio/template/query.html");
        assertResizableList("templates/minio/template-type/query.html");
        assertResizableList("templates/qiniu/template/query.html");
        assertResizableList("templates/qiniu/template-type/query.html");
    }

    private void assertRoute(Class<?> controllerType, String route) {
        RequestMapping mapping = controllerType.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{route}, mapping.value());
    }

    private void assertQiniuView(String view) throws IOException {
        String content = resource("templates/qiniu/" + view);
        assertTrue(content.contains("data-provider-base-path=\"/qiniu\"")
                || view.equals("template/preview-modal.html"));
    }

    private void assertResizableList(String view) throws IOException {
        String content = resource(view);
        int dependency = content.indexOf("jquery.resizableColumns.min.js");
        int bootstrapTable = content.indexOf("bootstrap-table.min.js");
        int extension = content.indexOf("bootstrap-table-resizable.min.js");

        assertTrue(content.contains("data-resizable=\"true\""));
        assertTrue(dependency >= 0 && dependency < bootstrapTable);
        assertTrue(extension > bootstrapTable);
    }

    private void assertTemplateStorageFields(String view) throws IOException {
        String content = resource(view);

        assertTrue(content.contains("id=\"bucketName\" th:field=\"*{bucketName}\" readonly aria-readonly=\"true\""));
        assertTrue(content.contains("id=\"objectName\" th:field=\"*{objectName}\" readonly aria-readonly=\"true\" data-object-name-auto"));
    }

    private String resource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
