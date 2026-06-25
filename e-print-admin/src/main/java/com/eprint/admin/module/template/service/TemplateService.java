package com.eprint.admin.module.template.service;

import com.eprint.admin.module.template.model.PageResult;
import com.eprint.admin.module.template.model.request.TemplateCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateModifyRequest;
import com.eprint.admin.module.template.model.request.TemplatePreviewRequest;
import com.eprint.admin.module.template.model.request.TemplateQueryRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eprint.admin.repository.dao.TemplateDao;
import com.eprint.admin.repository.model.entity.Template;
import com.eprint.admin.repository.model.entity.TemplateType;
import com.eprint.admin.repository.model.param.TemplateQueryParam;
import com.eprint.admin.repository.model.result.TemplateResult;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TemplateService {

    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private static final Pattern EACH_PATTERN = Pattern.compile("\\{\\{#each\\s+([A-Za-z0-9_.-]+)\\s*}}([\\s\\S]*?)\\{\\{/each}}");
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "T.ID",
            "templateType", "TT.CODE",
            "templateCode", "T.CODE",
            "bucketName", "T.BUCKET_NAME",
            "objectName", "T.OBJECT_NAME",
            "status", "T.STATUS"
    );
    private static final String[] CODE128_PATTERNS = {
            "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
            "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
            "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
            "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
            "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
            "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
            "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
            "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
            "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
            "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
            "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
    };

    private final TemplateDao templateDao;
    private final TemplateTypeService templateTypeService;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String defaultBucketName;
    private final String defaultObjectPrefix;

    public TemplateService(TemplateDao templateDao,
                           TemplateTypeService templateTypeService,
                           MinioClient minioClient,
                           @Value("${app.template.default-bucket:e-print}") String defaultBucketName,
                           @Value("${app.template.default-object-prefix:templates/print}") String defaultObjectPrefix) {
        this.templateDao = templateDao;
        this.templateTypeService = templateTypeService;
        this.minioClient = minioClient;
        this.defaultBucketName = defaultBucketName;
        this.defaultObjectPrefix = trimSlashes(defaultObjectPrefix);
    }

    public PageResult<TemplateResult> query(TemplateQueryRequest request) {
        int normalizedPageSize = normalizePageSize(request.limit());
        int normalizedPage = request.page();
        TemplateQueryParam param = new TemplateQueryParam();
        param.setTemplateTypeId(StringUtils.hasText(request.getTemplateTypeId()) ? request.getTemplateTypeId().trim() : null);
        param.setTemplateCode(StringUtils.hasText(request.queryTemplateCode()) ? request.queryTemplateCode().trim() : null);
        param.setStatus(request.getStatus());
        param.setOrderBy(toOrderBy(request.getSort()));

        int total = templateDao.count(param);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedPageSize);
        if (totalPages > 0 && normalizedPage > totalPages) {
            normalizedPage = totalPages;
        }

        param.setPage(normalizedPage);
        param.setPageSize(normalizedPageSize);
        List<TemplateResult> records = templateDao.query(param);
        return new PageResult<>(records, normalizedPage, normalizedPageSize, total);
    }

    private String toOrderBy(String sort) {
        List<String> clauses = new ArrayList<>();
        LinkedHashSet<String> sortedFields = new LinkedHashSet<>();

        if (StringUtils.hasText(sort)) {
            for (String token : sort.split(",")) {
                if (clauses.size() >= 5) {
                    break;
                }
                String normalizedToken = token == null ? "" : token.trim();
                int separatorIndex = normalizedToken.lastIndexOf('.');
                if (separatorIndex <= 0 || separatorIndex >= normalizedToken.length() - 1) {
                    continue;
                }

                String field = normalizedToken.substring(0, separatorIndex);
                String direction = normalizedToken.substring(separatorIndex + 1).toUpperCase(Locale.ROOT);
                String column = SORT_COLUMNS.get(field);
                if (column == null || (!"ASC".equals(direction) && !"DESC".equals(direction)) || !sortedFields.add(field)) {
                    continue;
                }
                clauses.add(column + " " + direction);
            }
        }

        if (!sortedFields.contains("id")) {
            clauses.add("T.ID DESC");
        }

        StringJoiner orderBy = new StringJoiner(", ");
        clauses.forEach(orderBy::add);
        return orderBy.toString();
    }

    public TemplateCreateRequest createRequest() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        List<TemplateType> templateTypes = templateTypeService.queryEnabled();
        if (!templateTypes.isEmpty()) {
            request.setTemplateTypeId(templateTypes.get(0).getId());
        }
        request.setBucketName(defaultBucketName);
        request.setStatus(STATUS_ENABLED);
        request.setContent(defaultTemplateContent());
        return request;
    }

    public TemplateModifyRequest getModifyRequest(TemplateModifyRequest request) {
        String id = request.getId();
        Template template = getRequiredTemplate(id);
        TemplateModifyRequest result = toModifyRequest(template);
        result.setContent(readObject(template.getBucketName(), template.getObjectName()));
        return result;
    }

    public String getPreviewContent(String id) {
        Template template = getRequiredTemplate(id);
        return readObject(template.getBucketName(), template.getObjectName());
    }

    public String renderPreviewContent(String id, String sampleData) {
        return renderTemplateContent(getPreviewContent(id), sampleData);
    }

    public String renderTemplateContent(String templateContent, String sampleData) {
        try {
            JsonNode root = objectMapper.readTree(StringUtils.hasText(sampleData) ? sampleData : "{}");
            return renderPlaceholders(templateContent == null ? "" : templateContent, root);
        } catch (Exception e) {
            log.warn("Render template preview failed", e);
            throw new IllegalArgumentException("Sample data must be valid JSON");
        }
    }

    public String renderPreview(TemplatePreviewRequest request) {
        if (StringUtils.hasText(request.getId())) {
            return renderPreviewContent(request.getId(), request.getSampleData());
        }
        return renderTemplateContent(request.getContent(), request.getSampleData());
    }

    public String defaultSampleData() {
        return """
                {
                  "storeName": "E-Print Store",
                  "storeAddress": "上海市徐汇区示例路 100 号",
                  "storePhone": "400-100-2000",
                  "receiptNo": "RC202606080001",
                  "cashier": "A001",
                  "printTime": "2026-06-08 15:30:00",
                  "items": [
                    {
                      "name": "MacBook Pro 14",
                      "qty": 1,
                      "price": "12999.00",
                      "amount": "12999.00"
                    },
                    {
                      "name": "USB-C 充电线",
                      "qty": 2,
                      "price": "59.00",
                      "amount": "118.00"
                    }
                  ],
                  "subtotal": "13117.00",
                  "discount": "-100.00",
                  "total": "13017.00",
                  "paymentMethod": "微信支付",
                  "footerText": "谢谢惠顾，欢迎再次光临",
                  "productName": "MacBook Pro 14",
                  "sku": "MBP-14-001",
                  "price": "12999.00",
                  "quantity": 1,
                  "shopName": "E-Print Store",
                  "orderNo": "SO202606050001",
                  "qr": {
                    "qrText": "https://example.com/order/RC202606080001"
                  },
                  "barcode": {
                    "barcodeText": "RC202606080001"
                  }
                }
                """;
    }

    public Template getRequiredTemplate(String id) {
        Template template = templateDao.get(id);
        if (template == null) {
            throw new IllegalArgumentException("Template not found");
        }
        return template;
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(TemplateCreateRequest request) {
        TemplateType templateType = normalize(request, true);
        if (templateDao.getByTemplateTypeIdAndCode(request.getTemplateTypeId(), request.getTemplateCode()) != null) {
            throw new IllegalArgumentException("Template code already exists");
        }
        putObject(request.getBucketName(), request.getObjectName(), request.getContent());

        Template template = new Template();
        template.setTemplateTypeId(templateType.getId());
        template.setTemplateCode(request.getTemplateCode());
        template.setBucketName(request.getBucketName());
        template.setObjectName(request.getObjectName());
        template.setStatus(request.getStatus());
        templateDao.create(template);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(TemplateModifyRequest request) {
        Template existing = getRequiredTemplate(request.getId());
        TemplateType templateType = normalize(request, STATUS_ENABLED.equals(request.getStatus()));
        Template sameCode = templateDao.getByTemplateTypeIdAndCode(request.getTemplateTypeId(), request.getTemplateCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template code already exists");
        }

        putObject(request.getBucketName(), request.getObjectName(), request.getContent());

        existing.setTemplateTypeId(templateType.getId());
        existing.setTemplateCode(request.getTemplateCode());
        existing.setBucketName(request.getBucketName());
        existing.setObjectName(request.getObjectName());
        existing.setStatus(request.getStatus());
        templateDao.modify(existing);
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
    public void remove(String id) {
        getRequiredTemplate(id);
        templateDao.remove(new String[]{id});
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int remove(List<String> ids) {
        List<String> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }
        return templateDao.remove(normalizedIds.toArray(String[]::new));
    }

    private int updateStatus(List<String> ids, Integer status) {
        List<String> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return 0;
        }
        return templateDao.updateStatusByIds(normalizedIds.toArray(String[]::new), status);
    }

    private List<String> normalizeIds(List<String> ids) {
        return ids == null ? List.of() : ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private TemplateModifyRequest toModifyRequest(Template template) {
        TemplateModifyRequest request = new TemplateModifyRequest();
        request.setId(template.getId());
        request.setTemplateTypeId(template.getTemplateTypeId());
        request.setTemplateCode(template.getTemplateCode());
        request.setBucketName(template.getBucketName());
        request.setObjectName(template.getObjectName());
        request.setStatus(template.getStatus());
        return request;
    }

    private TemplateType normalize(TemplateCreateRequest request, boolean requireEnabledType) {
        request.setTemplateTypeId(request.getTemplateTypeId().trim());
        TemplateType templateType = requireEnabledType
                ? templateTypeService.getEnabledRequired(request.getTemplateTypeId())
                : templateTypeService.getRequired(request.getTemplateTypeId());
        request.setTemplateCode(request.getTemplateCode().trim());
        request.setBucketName(request.getBucketName().trim());
        if (!StringUtils.hasText(request.getObjectName())) {
            request.setObjectName(defaultObjectName(templateType.getCode(), request.getTemplateCode()));
        } else {
            request.setObjectName(trimLeadingSlash(request.getObjectName().trim()));
        }
        if (request.getStatus() == null) {
            request.setStatus(STATUS_ENABLED);
        }
        if (!STATUS_ENABLED.equals(request.getStatus()) && !STATUS_DISABLED.equals(request.getStatus())) {
            throw new IllegalArgumentException("Invalid template status");
        }
        return templateType;
    }

    private TemplateType normalize(TemplateModifyRequest request, boolean requireEnabledType) {
        request.setTemplateTypeId(request.getTemplateTypeId().trim());
        TemplateType templateType = requireEnabledType
                ? templateTypeService.getEnabledRequired(request.getTemplateTypeId())
                : templateTypeService.getRequired(request.getTemplateTypeId());
        request.setTemplateCode(request.getTemplateCode().trim());
        request.setBucketName(request.getBucketName().trim());
        if (!StringUtils.hasText(request.getObjectName())) {
            request.setObjectName(defaultObjectName(templateType.getCode(), request.getTemplateCode()));
        } else {
            request.setObjectName(trimLeadingSlash(request.getObjectName().trim()));
        }
        if (request.getStatus() == null) {
            request.setStatus(STATUS_ENABLED);
        }
        if (!STATUS_ENABLED.equals(request.getStatus()) && !STATUS_DISABLED.equals(request.getStatus())) {
            throw new IllegalArgumentException("Invalid template status");
        }
        return templateType;
    }

    public String defaultObjectName(String templateType, String templateCode) {
        return defaultObjectPrefix + "/" + templateType + "/" + templateCode + ".html";
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
        String content = renderEachBlocks(templateContent, root);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String value = resolveValue(root, matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String renderEachBlocks(String templateContent, JsonNode root) {
        Matcher matcher = EACH_PATTERN.matcher(templateContent);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            JsonNode items = resolveNode(root, matcher.group(1));
            String block = matcher.group(2);
            String replacement = "";
            if (items != null && items.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode item : items) {
                    builder.append(renderPlaceholders(block, item));
                }
                replacement = builder.toString();
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String resolveValue(JsonNode root, String path) {
        JsonNode current = resolveNode(root, path);
        if (current == null || current.isMissingNode() || current.isNull()) {
            return "";
        }
        String value = current.isValueNode() ? current.asText() : current.toString();
        if (path.startsWith("qr.") && StringUtils.hasText(value)) {
            return qrDataUrl(value);
        }
        if (path.startsWith("barcode.") && StringUtils.hasText(value)) {
            return barcodeDataUrl(value);
        }
        return value;
    }

    private JsonNode resolveNode(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }
            current = current.path(segment);
        }
        return current;
    }

    private String qrDataUrl(String value) {
        int size = 29;
        int cell = 6;
        int quiet = 4;
        int viewBox = (size + quiet * 2) * cell;
        StringBuilder rects = new StringBuilder();
        boolean[][] matrix = previewQrMatrix(value, size);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (matrix[y][x]) {
                    rects.append("<rect x=\"")
                            .append((x + quiet) * cell)
                            .append("\" y=\"")
                            .append((y + quiet) * cell)
                            .append("\" width=\"")
                            .append(cell)
                            .append("\" height=\"")
                            .append(cell)
                            .append("\"/>");
                }
            }
        }
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + viewBox + " " + viewBox + "\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<g fill=\"#111\">" + rects + "</g>"
                + "</svg>";
        return svgDataUrl(svg);
    }

    private boolean[][] previewQrMatrix(String value, int size) {
        boolean[][] matrix = new boolean[size][size];
        drawFinder(matrix, 0, 0);
        drawFinder(matrix, size - 7, 0);
        drawFinder(matrix, 0, size - 7);
        byte[] digest = sha256(value);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (isFinderArea(x, y, size)) {
                    continue;
                }
                int index = Math.floorMod((x * 31 + y * 17 + digest[(x + y) % digest.length]), digest.length);
                int bit = (digest[index] >> ((x + y) % 8)) & 1;
                matrix[y][x] = bit == 1 || ((x * 3 + y * 5 + digest[index]) & 7) == 0;
            }
        }
        return matrix;
    }

    private void drawFinder(boolean[][] matrix, int left, int top) {
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                boolean outer = x == 0 || x == 6 || y == 0 || y == 6;
                boolean inner = x >= 2 && x <= 4 && y >= 2 && y <= 4;
                matrix[top + y][left + x] = outer || inner;
            }
        }
    }

    private boolean isFinderArea(int x, int y, int size) {
        return (x < 8 && y < 8) || (x >= size - 8 && y < 8) || (x < 8 && y >= size - 8);
    }

    private String barcodeDataUrl(String value) {
        String text = value.length() > 80 ? value.substring(0, 80) : value;
        List<Integer> codes = new java.util.ArrayList<>();
        codes.add(104);
        int checksum = 104;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int code = ch >= 32 && ch <= 127 ? ch - 32 : 0;
            codes.add(code);
            checksum += code * (i + 1);
        }
        codes.add(checksum % 103);
        codes.add(106);

        int module = 2;
        int height = 72;
        int quiet = 18;
        int x = quiet;
        StringBuilder bars = new StringBuilder();
        for (Integer code : codes) {
            String pattern = CODE128_PATTERNS[code];
            for (int i = 0; i < pattern.length(); i++) {
                int width = Character.digit(pattern.charAt(i), 10) * module;
                if (i % 2 == 0) {
                    bars.append("<rect x=\"")
                            .append(x)
                            .append("\" y=\"0\" width=\"")
                            .append(width)
                            .append("\" height=\"")
                            .append(height)
                            .append("\"/>");
                }
                x += width;
            }
        }
        int width = x + quiet;
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + width + " " + height + "\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<g fill=\"#111\">" + bars + "</g>"
                + "</svg>";
        return svgDataUrl(svg);
    }

    private String svgDataUrl(String svg) {
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
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
