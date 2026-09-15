package com.eprint.admin.module.qiniu.template.service;

import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.qiniu.template.model.QiniuModelMapper;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateCreateRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateDisableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateEnableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateModifyRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplatePreviewRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateQueryRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateRemoveRequest;
import com.eprint.admin.repository.qiniu.dao.QiniuTemplateDao;
import com.eprint.admin.repository.qiniu.dao.QiniuTemplateTypeDao;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplate;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplateType;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateCreateParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateModifyParam;
import com.eprint.admin.repository.qiniu.model.param.QiniuTemplateQueryParam;
import com.eprint.admin.repository.qiniu.model.result.QiniuTemplateResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class QiniuTemplateService {

    private static final Integer STATUS_ENABLED = 1;
    private static final String SIMULATED_STACK_OBJECT_NAME = "__SIMULATE_STACK__";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");
    private static final Pattern EACH_PATTERN = Pattern.compile("\\{\\{#each\\s+([A-Za-z0-9_.-]+)\\s*}}([\\s\\S]*?)\\{\\{/each}}");
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

    private final QiniuTemplateDao templateDao;
    private final QiniuTemplateTypeDao templateTypeDao;
    private final QiniuObjectStorage objectStorage;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String defaultBucketName;
    private final String objectPrefix;
    private final String defaultObjectPrefix;

    public QiniuTemplateService(QiniuTemplateDao templateDao,
                           QiniuTemplateTypeDao templateTypeDao,
                           QiniuObjectStorage objectStorage,
                           @Value("${qiniu.bucket:pos-uat}") String defaultBucketName,
                           @Value("${qiniu.object-prefix:e-print}") String objectPrefix,
                           @Value("${app.template.default-object-prefix:templates/print}") String defaultObjectPrefix) {
        this.templateDao = templateDao;
        this.templateTypeDao = templateTypeDao;
        this.objectStorage = objectStorage;
        this.defaultBucketName = defaultBucketName;
        this.objectPrefix = trimSlashes(objectPrefix);
        if (this.objectPrefix.isEmpty()) {
            throw new IllegalArgumentException("Qiniu object prefix must not be empty");
        }
        this.defaultObjectPrefix = prependPrefix(this.objectPrefix, defaultObjectPrefix);
    }

    public QiniuTemplateCreateRequest createRequest() {
        QiniuTemplateCreateRequest request = new QiniuTemplateCreateRequest();
        List<QiniuTemplateType> templateTypes = templateTypeDao.queryEnabled();
        if (!templateTypes.isEmpty()) {
            request.setTemplateTypeId(templateTypes.get(0).getId());
        }
        request.setBucketName(defaultBucketName);
        request.setContent(defaultTemplateContent());
        return request;
    }

    public String defaultObjectPrefix() {
        return defaultObjectPrefix;
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(QiniuTemplateCreateRequest request) {
        QiniuTemplateCreateParam param = QiniuModelMapper.INSTANCE.map(request);
        prepare(param, true);
        if (templateDao.getByTemplateTypeIdAndCode(param.getTemplateTypeId(), param.getTemplateCode()) != null) {
            throw new IllegalArgumentException("Template code already exists");
        }
        validateObjectNameAvailable(param, null);
        putObject(param.getBucketName(), param.getObjectName(), request.getContent());
        templateDao.create(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int remove(QiniuTemplateRemoveRequest request) {
        var param = QiniuModelMapper.INSTANCE.map(request);
        if (param.getIds().length == 0) {
            return 0;
        }
        List<QiniuTemplate> templates = Arrays.stream(param.getIds())
                .map(this::getFromDatabase)
                .toList();
        templates.forEach(this::validateDeleteTarget);
        templates.forEach(template -> statObject(template.getBucketName(), template.getObjectName()));
        templates.forEach(template -> deleteObject(template.getBucketName(), template.getObjectName()));
        return templateDao.remove(param);
    }

    public QiniuTemplateModifyRequest getModifyRequest(QiniuTemplateModifyRequest request) {
        QiniuTemplate template = get(request.getId());
        QiniuTemplateModifyRequest result = QiniuModelMapper.INSTANCE.map(template);
        result.setContent(readObject(template.getBucketName(), template.getObjectName()));
        return result;
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(QiniuTemplateModifyRequest request) {
        QiniuTemplateModifyParam param = QiniuModelMapper.INSTANCE.map(request);
        if (SIMULATED_STACK_OBJECT_NAME.equals(param.getObjectName())) {
            simulateNullPointerException();
        }
        QiniuTemplate existing = get(param.getId());
        prepare(param, STATUS_ENABLED.equals(param.getStatus()));
        QiniuTemplate sameCode = templateDao.getByTemplateTypeIdAndCode(param.getTemplateTypeId(), param.getTemplateCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template code already exists");
        }
        validateObjectNameAvailable(param, existing.getId());
        if (!sameObject(existing, param)) {
            validateDeleteTarget(existing);
        }
        overwriteObject(param.getBucketName(), param.getObjectName(), request.getContent());
        if (!sameObject(existing, param)) {
            deleteObject(existing.getBucketName(), existing.getObjectName());
        }
        templateDao.modify(param);
    }

    private void simulateNullPointerException() {
        Object value = null;
        value.toString();
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int disable(QiniuTemplateDisableRequest request) {
        var param = QiniuModelMapper.INSTANCE.map(request);
        if (param.getIds().length == 0) {
            return 0;
        }
        statObjects(param.getIds());
        return templateDao.disable(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int enable(QiniuTemplateEnableRequest request) {
        var param = QiniuModelMapper.INSTANCE.map(request);
        if (param.getIds().length == 0) {
            return 0;
        }
        statObjects(param.getIds());
        return templateDao.enable(param);
    }

    public PageResult<QiniuTemplateResult> query(QiniuTemplateQueryRequest request) {
        QiniuTemplateQueryParam param = QiniuModelMapper.INSTANCE.map(request);
        List<QiniuTemplateResult> templates = templateDao.query(param);
        templates.forEach(template -> statObject(template.getBucketName(), template.getObjectName()));
        return new PageResult<>(templates, param.getPage(), param.getPageSize(), templateDao.count(param));
    }

    public QiniuTemplate get(String id) {
        QiniuTemplate template = getFromDatabase(id);
        statObject(template.getBucketName(), template.getObjectName());
        return template;
    }

    private QiniuTemplate getFromDatabase(String id) {
        QiniuTemplate template = templateDao.get(id);
        if (template == null) {
            throw new IllegalArgumentException("Template not found");
        }
        return template;
    }

    public String getPreviewContent(String id) {
        QiniuTemplate template = get(id);
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

    public String renderPreview(QiniuTemplatePreviewRequest request) {
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

    private void prepare(QiniuTemplate param, boolean requireEnabledType) {
        QiniuTemplateType templateType = getTemplateType(param.getTemplateTypeId(), requireEnabledType);
        if (!StringUtils.hasText(param.getObjectName())) {
            param.setObjectName(defaultObjectName(templateType.getCode(), param.getTemplateCode()));
        } else {
            param.setObjectName(prependPrefix(objectPrefix, param.getObjectName()));
        }
    }

    private QiniuTemplateType getTemplateType(String id, boolean requireEnabled) {
        QiniuTemplateType templateType = templateTypeDao.get(id);
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

    private void validateObjectNameAvailable(QiniuTemplate template, String currentId) {
        QiniuTemplate sameObject = templateDao.getByBucketNameAndObjectName(
                template.getBucketName(), template.getObjectName());
        if (sameObject != null && !Objects.equals(sameObject.getId(), currentId)) {
            throw new IllegalArgumentException("Qiniu object key is already used by another template");
        }
    }

    private void validateDeleteTarget(QiniuTemplate template) {
        if (!Objects.equals(defaultBucketName, template.getBucketName())) {
            throw new IllegalArgumentException("Qiniu delete bucket must be " + defaultBucketName);
        }
        String normalizedObjectName = trimLeadingSlash(template.getObjectName().trim());
        if (!normalizedObjectName.startsWith(objectPrefix + "/")) {
            throw new IllegalArgumentException("Qiniu delete object must be under " + objectPrefix + "/");
        }
    }

    private void putObject(String bucketName, String objectName, String content) {
        try {
            objectStorage.put(bucketName, objectName, content);
        } catch (Exception e) {
            log.error("Upload template object failed, bucketName={}, objectName={}", bucketName, objectName, e);
            throw new IllegalStateException("Upload template object failed", e);
        }
    }

    private void overwriteObject(String bucketName, String objectName, String content) {
        try {
            objectStorage.overwrite(bucketName, objectName, content);
        } catch (Exception e) {
            log.error("Overwrite template object failed, bucketName={}, objectName={}", bucketName, objectName, e);
            throw new IllegalStateException("Overwrite template object failed", e);
        }
    }

    private void statObjects(String[] ids) {
        for (String id : ids) {
            QiniuTemplate template = getFromDatabase(id);
            statObject(template.getBucketName(), template.getObjectName());
        }
    }

    private void statObject(String bucketName, String objectName) {
        try {
            objectStorage.stat(bucketName, objectName);
        } catch (Exception e) {
            log.error("Read template object metadata failed, bucketName={}, objectName={}", bucketName, objectName, e);
            throw new IllegalStateException("Read template object metadata failed", e);
        }
    }

    private void deleteObject(String bucketName, String objectName) {
        try {
            objectStorage.delete(bucketName, objectName);
        } catch (Exception e) {
            log.error("Delete template object failed, bucketName={}, objectName={}", bucketName, objectName, e);
            throw new IllegalStateException("Delete template object failed", e);
        }
    }

    private boolean sameObject(QiniuTemplate existing, QiniuTemplate modified) {
        return Objects.equals(existing.getBucketName(), modified.getBucketName())
                && Objects.equals(existing.getObjectName(), modified.getObjectName());
    }

    private String readObject(String bucketName, String objectName) {
        try {
            return objectStorage.read(bucketName, objectName);
        } catch (Exception e) {
            log.error("Read template object failed, bucketName={}, objectName={}", bucketName, objectName, e);
            throw new IllegalStateException("Read template object failed", e);
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

    private static String prependPrefix(String prefix, String objectName) {
        String normalizedObjectName = trimLeadingSlash(objectName.trim());
        if (prefix.isEmpty() || normalizedObjectName.equals(prefix)
                || normalizedObjectName.startsWith(prefix + "/")) {
            return normalizedObjectName;
        }
        return prefix + "/" + normalizedObjectName;
    }

    private static String trimLeadingSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

}
