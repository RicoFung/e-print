package com.eprint.admin.module.minio.template.service;

import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.minio.template.model.MinioModelMapper;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateCreateRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateDisableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateEnableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateModifyRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplatePreviewRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateQueryRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateRemoveRequest;
import com.eprint.admin.repository.minio.dao.MinioTemplateDao;
import com.eprint.admin.repository.minio.dao.MinioTemplateTypeDao;
import com.eprint.admin.repository.minio.model.entity.MinioTemplate;
import com.eprint.admin.repository.minio.model.entity.MinioTemplateType;
import com.eprint.admin.repository.minio.model.param.MinioTemplateCreateParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateModifyParam;
import com.eprint.admin.repository.minio.model.param.MinioTemplateQueryParam;
import com.eprint.admin.repository.minio.model.result.MinioTemplateResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MinioTemplateService {

    private static final Integer STATUS_ENABLED = 1;
    private static final String SIMULATED_STACK_OBJECT_NAME = "__SIMULATE_STACK__";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private static final Pattern EACH_PATTERN = Pattern.compile("\\{\\{#each\\s+([A-Za-z0-9_.-]+)\\s*}}([\\s\\S]*?)\\{\\{/each}}");
    private static final Pattern IF_PATTERN = Pattern.compile("\\{\\{#if\\s+([A-Za-z0-9_.-]+)\\s*}}([\\s\\S]*?)\\{\\{/if}}");
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

    private final MinioTemplateDao templateDao;
    private final MinioTemplateTypeDao templateTypeDao;
    private final ObjectProvider<MinioClient> minioClientProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String defaultBucketName;
    private final String defaultObjectPrefix;

    public MinioTemplateService(MinioTemplateDao templateDao,
                           MinioTemplateTypeDao templateTypeDao,
                           ObjectProvider<MinioClient> minioClientProvider,
                           @Value("${app.template.default-bucket:e-print}") String defaultBucketName,
                           @Value("${app.template.default-object-prefix:templates/print}") String defaultObjectPrefix) {
        this.templateDao = templateDao;
        this.templateTypeDao = templateTypeDao;
        this.minioClientProvider = minioClientProvider;
        this.defaultBucketName = defaultBucketName;
        this.defaultObjectPrefix = trimSlashes(defaultObjectPrefix);
    }

    public MinioTemplateCreateRequest createRequest() {
        MinioTemplateCreateRequest request = new MinioTemplateCreateRequest();
        List<MinioTemplateType> templateTypes = templateTypeDao.queryEnabled();
        if (!templateTypes.isEmpty()) {
            request.setTemplateTypeId(templateTypes.get(0).getId());
        }
        request.setBucketName(defaultBucketName);
        request.setContent(defaultTemplateContent());
        return request;
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(MinioTemplateCreateRequest request) {
        MinioTemplateCreateParam param = MinioModelMapper.INSTANCE.map(request);
        prepare(param, true);
        if (templateDao.getByTemplateTypeIdAndCode(param.getTemplateTypeId(), param.getTemplateCode()) != null) {
            throw new IllegalArgumentException("Template code already exists");
        }
        putObject(param.getBucketName(), param.getObjectName(), request.getContent());
        templateDao.create(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int remove(MinioTemplateRemoveRequest request) {
        var param = MinioModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateDao.remove(param);
    }

    public MinioTemplateModifyRequest getModifyRequest(MinioTemplateModifyRequest request) {
        MinioTemplate template = get(request.getId());
        MinioTemplateModifyRequest result = MinioModelMapper.INSTANCE.map(template);
        result.setContent(readObject(template.getBucketName(), template.getObjectName()));
        return result;
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(MinioTemplateModifyRequest request) {
        MinioTemplateModifyParam param = MinioModelMapper.INSTANCE.map(request);
        if (SIMULATED_STACK_OBJECT_NAME.equals(param.getObjectName())) {
            simulateNullPointerException();
        }
        MinioTemplate existing = get(param.getId());
        prepare(param, STATUS_ENABLED.equals(param.getStatus()));
        MinioTemplate sameCode = templateDao.getByTemplateTypeIdAndCode(param.getTemplateTypeId(), param.getTemplateCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template code already exists");
        }
        putObject(param.getBucketName(), param.getObjectName(), request.getContent());
        templateDao.modify(param);
    }

    private void simulateNullPointerException() {
        Object value = null;
        value.toString();
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int disable(MinioTemplateDisableRequest request) {
        var param = MinioModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateDao.disable(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int enable(MinioTemplateEnableRequest request) {
        var param = MinioModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateDao.enable(param);
    }

    public PageResult<MinioTemplateResult> query(MinioTemplateQueryRequest request) {
        MinioTemplateQueryParam param = MinioModelMapper.INSTANCE.map(request);
        return new PageResult<>(templateDao.query(param), param.getPage(), param.getPageSize(), templateDao.count(param));
    }

    public MinioTemplate get(String id) {
        MinioTemplate template = templateDao.get(id);
        if (template == null) {
            throw new IllegalArgumentException("Template not found");
        }
        return template;
    }

    public String getPreviewContent(String id) {
        MinioTemplate template = get(id);
        return readObject(template.getBucketName(), template.getObjectName());
    }

    public String renderPreviewContent(String id, String sampleData) {
        return renderTemplateContent(getPreviewContent(id), sampleData);
    }

    public String renderTemplateContent(String templateContent, String sampleData) {
        try {
            JsonNode root = objectMapper.readTree(StringUtils.hasText(sampleData) ? sampleData : "{}");
            resolveCodeAssets(root, "data");
            return renderPlaceholders(templateContent == null ? "" : templateContent, root);
        } catch (JsonProcessingException e) {
            log.warn("Render template preview failed", e);
            throw new IllegalArgumentException("Sample data must be valid JSON");
        }
    }

    public String renderPreview(MinioTemplatePreviewRequest request) {
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
                  "memberName": "张三",
                  "points": 1280,
                  "footerText": "谢谢惠顾，欢迎再次光临",
                  "productName": "MacBook Pro 14",
                  "sku": "MBP-14-001",
                  "price": "12999.00",
                  "quantity": 1,
                  "shopName": "E-Print Store",
                  "orderNo": "SO202606050001",
                  "codes": {
                    "receiptBarcode": {
                      "codeType": "barcode",
                      "value": "RC202606080001"
                    },
                    "memberQr": {
                      "codeType": "qr",
                      "value": "https://example.com/member/001"
                    },
                    "electronicReceiptQr": {
                      "codeType": "qr",
                      "value": "https://example.com/order/RC202606080001"
                    }
                  }
                }
                """;
    }

    private void prepare(MinioTemplate param, boolean requireEnabledType) {
        MinioTemplateType templateType = getTemplateType(param.getTemplateTypeId(), requireEnabledType);
        if (!StringUtils.hasText(param.getObjectName())) {
            param.setObjectName(defaultObjectName(templateType.getCode(), param.getTemplateCode()));
        } else {
            param.setObjectName(trimLeadingSlash(param.getObjectName().trim()));
        }
    }

    private MinioTemplateType getTemplateType(String id, boolean requireEnabled) {
        MinioTemplateType templateType = templateTypeDao.get(id);
        if (templateType == null) {
            throw new IllegalArgumentException("Template type not found");
        }
        if (requireEnabled && !STATUS_ENABLED.equals(templateType.getStatus())) {
            throw new IllegalArgumentException("Template type is disabled");
        }
        return templateType;
    }

    private String defaultObjectName(String templateType, String templateCode) {
        return defaultObjectPrefix + "/" + templateType + "/" + templateCode + ".html";
    }

    private void putObject(String bucketName, String objectName, String content) {
        try {
            ensureBucket(bucketName);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                minioClient().putObject(PutObjectArgs.builder()
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
        try (InputStream inputStream = minioClient().getObject(GetObjectArgs.builder()
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
        MinioClient minioClient = minioClient();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    private MinioClient minioClient() {
        return minioClientProvider.getObject();
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
        content = renderIfBlocks(content, root);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String value = resolveValue(root, matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String renderIfBlocks(String templateContent, JsonNode root) {
        Matcher matcher = IF_PATTERN.matcher(templateContent);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            JsonNode condition = resolveNode(root, matcher.group(1));
            String replacement = isTruthy(condition) ? renderPlaceholders(matcher.group(2), root) : "";
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private boolean isTruthy(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isNumber()) {
            return value.doubleValue() != 0;
        }
        if (value.isTextual()) {
            return StringUtils.hasText(value.textValue());
        }
        return !value.isArray() || !value.isEmpty();
    }

    private void resolveCodeAssets(JsonNode node, String path) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                resolveCodeAssets(node.get(i), path + "[" + i + "]");
            }
            return;
        }
        node.fields().forEachRemaining(entry -> resolveCodeAssets(entry.getValue(), path + "." + entry.getKey()));
        JsonNode codeTypeNode = node.get("codeType");
        if (codeTypeNode == null) {
            return;
        }
        String codeType = codeTypeNode.asText();
        if (!"barcode".equals(codeType) && !"qr".equals(codeType)) {
            throw new IllegalArgumentException("Unsupported codeType at " + path + ": " + codeType);
        }
        JsonNode valueNode = node.get("value");
        String value = valueNode == null ? null : valueNode.asText();
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Code value must be a non-empty string at " + path);
        }
        String dataUrl = "barcode".equals(codeType) ? barcodeDataUrl(value) : qrDataUrl(value);
        ((ObjectNode) node).put("dataUrl", dataUrl);
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

}
