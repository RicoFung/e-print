package com.eprint.admin.module.template.service;

import com.eprint.admin.module.template.model.TemplateForm;
import com.eprint.admin.module.template.model.PageResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eprint.admin.repository.dao.TemplateDao;
import com.eprint.admin.repository.model.entity.Template;
import com.eprint.admin.repository.model.param.TemplateQueryParam;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TemplateAdminService {

    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private final TemplateDao templateDao;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String defaultBucketName;
    private final String defaultObjectPrefix;

    public TemplateAdminService(TemplateDao templateDao,
                                MinioClient minioClient,
                                @Value("${app.template.default-bucket:e-print}") String defaultBucketName,
                                @Value("${app.template.default-object-prefix:templates/print}") String defaultObjectPrefix) {
        this.templateDao = templateDao;
        this.minioClient = minioClient;
        this.defaultBucketName = defaultBucketName;
        this.defaultObjectPrefix = trimSlashes(defaultObjectPrefix);
    }

    public PageResult<Template> page(String templateCode, Integer status, Integer page, Integer pageSize) {
        int normalizedPageSize = normalizePageSize(pageSize);
        int normalizedPage = page == null || page < 1 ? 1 : page;
        TemplateQueryParam param = new TemplateQueryParam();
        param.setTemplateCode(StringUtils.hasText(templateCode) ? templateCode.trim() : null);
        param.setStatus(status);

        int total = templateDao.count(param);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedPageSize);
        if (totalPages > 0 && normalizedPage > totalPages) {
            normalizedPage = totalPages;
        }

        param.setRowStart((normalizedPage - 1) * normalizedPageSize);
        param.setRowEnd(normalizedPage * normalizedPageSize);
        List<Template> records = templateDao.list(param);
        return new PageResult<>(records, normalizedPage, normalizedPageSize, total);
    }

    public TemplateForm createForm() {
        TemplateForm form = new TemplateForm();
        form.setBucketName(defaultBucketName);
        form.setStatus(STATUS_ENABLED);
        form.setContent(defaultTemplateContent());
        return form;
    }

    public TemplateForm getForm(String id) {
        Template template = getRequiredTemplate(id);
        TemplateForm form = toForm(template);
        form.setContent(readObject(template.getBucketName(), template.getObjectName()));
        return form;
    }

    public String getPreviewContent(String id) {
        Template template = getRequiredTemplate(id);
        return readObject(template.getBucketName(), template.getObjectName());
    }

    public String renderPreviewContent(String id, String sampleData) {
        String templateContent = getPreviewContent(id);
        try {
            JsonNode root = objectMapper.readTree(StringUtils.hasText(sampleData) ? sampleData : "{}");
            return renderPlaceholders(templateContent, root);
        } catch (Exception e) {
            log.warn("Render template preview failed, templateId={}", id, e);
            throw new IllegalArgumentException("Sample data must be valid JSON");
        }
    }

    public String defaultSampleData() {
        return """
                {
                  "productName": "MacBook Pro 14",
                  "sku": "MBP-14-001",
                  "price": "12999.00",
                  "quantity": 1,
                  "shopName": "E-Print Store",
                  "orderNo": "SO202606050001",
                  "qr": {
                    "qrText": "https://example.com/product/MBP-14-001"
                  },
                  "barcode": {
                    "barcodeText": "MBP-14-001"
                  }
                }
                """;
    }

    public Template getRequiredTemplate(String id) {
        Template template = templateDao.getById(id);
        if (template == null) {
            throw new IllegalArgumentException("Template not found");
        }
        return template;
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(TemplateForm form) {
        normalize(form);
        if (templateDao.getByTemplateCode(form.getTemplateCode()) != null) {
            throw new IllegalArgumentException("Template code already exists");
        }
        putObject(form.getBucketName(), form.getObjectName(), form.getContent());

        Template template = new Template();
        template.setTemplateCode(form.getTemplateCode());
        template.setBucketName(form.getBucketName());
        template.setObjectName(form.getObjectName());
        template.setStatus(form.getStatus());
        templateDao.insert(template);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void update(String id, TemplateForm form) {
        Template existing = getRequiredTemplate(id);
        normalize(form);
        Template sameCode = templateDao.getByTemplateCode(form.getTemplateCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template code already exists");
        }

        putObject(form.getBucketName(), form.getObjectName(), form.getContent());

        existing.setTemplateCode(form.getTemplateCode());
        existing.setBucketName(form.getBucketName());
        existing.setObjectName(form.getObjectName());
        existing.setStatus(form.getStatus());
        templateDao.update(existing);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void disable(String id) {
        getRequiredTemplate(id);
        templateDao.disable(id);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void enable(String id) {
        getRequiredTemplate(id);
        templateDao.enable(id);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int disable(List<String> ids) {
        return updateStatus(ids, STATUS_DISABLED);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int enable(List<String> ids) {
        return updateStatus(ids, STATUS_ENABLED);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void delete(String id) {
        getRequiredTemplate(id);
        templateDao.deleteById(id);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int delete(List<String> ids) {
        List<String> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }
        return templateDao.deleteByIds(normalizedIds);
    }

    private int updateStatus(List<String> ids, Integer status) {
        List<String> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }
        return templateDao.updateStatusByIds(normalizedIds, status);
    }

    private List<String> normalizeIds(List<String> ids) {
        return ids == null ? List.of() : ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private TemplateForm toForm(Template template) {
        TemplateForm form = new TemplateForm();
        form.setId(template.getId());
        form.setTemplateCode(template.getTemplateCode());
        form.setBucketName(template.getBucketName());
        form.setObjectName(template.getObjectName());
        form.setStatus(template.getStatus());
        return form;
    }

    private void normalize(TemplateForm form) {
        form.setTemplateCode(form.getTemplateCode().trim());
        form.setBucketName(form.getBucketName().trim());
        if (!StringUtils.hasText(form.getObjectName())) {
            form.setObjectName(defaultObjectName(form.getTemplateCode()));
        } else {
            form.setObjectName(trimLeadingSlash(form.getObjectName().trim()));
        }
        if (form.getStatus() == null) {
            form.setStatus(STATUS_ENABLED);
        }
        if (!STATUS_ENABLED.equals(form.getStatus()) && !STATUS_DISABLED.equals(form.getStatus())) {
            throw new IllegalArgumentException("Invalid template status");
        }
    }

    public String defaultObjectName(String templateCode) {
        return defaultObjectPrefix + "/" + templateCode + ".html";
    }

    private void putObject(String bucketName, String objectName, String content) {
        try {
            ensureBucket(bucketName);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, bytes.length, -1)
                        .contentType("text/html; charset=UTF-8")
                        .build());
            }
        } catch (Exception e) {
            log.error("Upload template object failed, bucketName={}, objectName={}", bucketName, objectName, e);
            throw new IllegalStateException("Upload template object failed", e);
        }
    }

    private String readObject(String bucketName, String objectName) {
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build())) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Read template object failed, bucketName={}, objectName={}", bucketName, objectName, e);
            throw new IllegalStateException("Read template object failed", e);
        }
    }

    private void ensureBucket(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    private String defaultTemplateContent() {
        return """
                <!doctype html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { font-family: sans-serif; margin: 0; padding: 16px; }
                    .label { width: 60mm; min-height: 40mm; border: 1px solid #222; padding: 8px; }
                  </style>
                </head>
                <body>
                  <section class="label">
                    <h1>{{productName}}</h1>
                    <p>{{sku}}</p>
                  </section>
                </body>
                </html>
                """;
    }

    private String renderPlaceholders(String templateContent, JsonNode root) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateContent);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String value = resolveValue(root, matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String resolveValue(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return "";
            }
            current = current.path(segment);
        }
        if (current == null || current.isMissingNode() || current.isNull()) {
            return "";
        }
        if (current.isValueNode()) {
            return current.asText();
        }
        return current.toString();
    }

    private static String trimSlashes(String value) {
        String result = value == null ? "" : value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String trimLeadingSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 10;
        }
        if (pageSize < 5) {
            return 5;
        }
        return Math.min(pageSize, 100);
    }
}
