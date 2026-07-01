package com.eprint.admin.module.template.service;

import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.template.model.ModelMapper;
import com.eprint.admin.module.template.model.request.TemplateCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateDisableRequest;
import com.eprint.admin.module.template.model.request.TemplateEnableRequest;
import com.eprint.admin.module.template.model.request.TemplateModifyRequest;
import com.eprint.admin.module.template.model.request.TemplatePreviewRequest;
import com.eprint.admin.module.template.model.request.TemplateQueryRequest;
import com.eprint.admin.module.template.model.request.TemplateRemoveRequest;
import com.eprint.admin.repository.dao.TemplateDao;
import com.eprint.admin.repository.dao.TemplateTypeDao;
import com.eprint.admin.repository.model.entity.Template;
import com.eprint.admin.repository.model.entity.TemplateType;
import com.eprint.admin.repository.model.param.TemplateCreateParam;
import com.eprint.admin.repository.model.param.TemplateModifyParam;
import com.eprint.admin.repository.model.param.TemplateQueryParam;
import com.eprint.admin.repository.model.result.TemplateResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TemplateService {

    private static final Integer STATUS_ENABLED = 1;
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

    private final TemplateDao templateDao;
    private final TemplateTypeDao templateTypeDao;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String defaultBucketName;
    private final String defaultObjectPrefix;

    public TemplateService(TemplateDao templateDao,
                           TemplateTypeDao templateTypeDao,
                           MinioClient minioClient,
                           @Value("${app.template.default-bucket:e-print}") String defaultBucketName,
                           @Value("${app.template.default-object-prefix:templates/print}") String defaultObjectPrefix) {
        this.templateDao = templateDao;
        this.templateTypeDao = templateTypeDao;
        this.minioClient = minioClient;
        this.defaultBucketName = defaultBucketName;
        this.defaultObjectPrefix = trimSlashes(defaultObjectPrefix);
    }

    public TemplateCreateRequest createRequest() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        List<TemplateType> templateTypes = templateTypeDao.queryEnabled();
        if (!templateTypes.isEmpty()) {
            request.setTemplateTypeId(templateTypes.get(0).getId());
        }
        request.setBucketName(defaultBucketName);
        request.setContent(defaultTemplateContent());
        return request;
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void create(TemplateCreateRequest request) {
        TemplateCreateParam param = ModelMapper.INSTANCE.map(request);
        prepare(param, true);
        if (templateDao.getByTemplateTypeIdAndCode(param.getTemplateTypeId(), param.getTemplateCode()) != null) {
            throw new IllegalArgumentException("Template code already exists");
        }
        putObject(param.getBucketName(), param.getObjectName(), request.getContent());
        templateDao.create(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int remove(TemplateRemoveRequest request) {
        var param = ModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateDao.remove(param);
    }

    public TemplateModifyRequest getModifyRequest(TemplateModifyRequest request) {
        Template template = get(request.getId());
        TemplateModifyRequest result = ModelMapper.INSTANCE.map(template);
        result.setContent(readObject(template.getBucketName(), template.getObjectName()));
        return result;
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public void modify(TemplateModifyRequest request) {
        TemplateModifyParam param = ModelMapper.INSTANCE.map(request);
        Template existing = get(param.getId());
        prepare(param, STATUS_ENABLED.equals(param.getStatus()));
        Template sameCode = templateDao.getByTemplateTypeIdAndCode(param.getTemplateTypeId(), param.getTemplateCode());
        if (sameCode != null && !sameCode.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("Template code already exists");
        }
        putObject(param.getBucketName(), param.getObjectName(), request.getContent());
        templateDao.modify(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int disable(TemplateDisableRequest request) {
        var param = ModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateDao.disable(param);
    }

    @Transactional(transactionManager = "transactionManagerMybatis", rollbackFor = Exception.class)
    public int enable(TemplateEnableRequest request) {
        var param = ModelMapper.INSTANCE.map(request);
        return param.getIds().length == 0 ? 0 : templateDao.enable(param);
    }

    public PageResult<TemplateResult> query(TemplateQueryRequest request) {
        TemplateQueryParam param = ModelMapper.INSTANCE.map(request);
        return new PageResult<>(templateDao.query(param), param.getPage(), param.getPageSize(), templateDao.count(param));
    }

    public Template get(String id) {
        Template template = templateDao.get(id);
        if (template == null) {
            throw new IllegalArgumentException("Template not found");
        }
        return template;
    }

    public String getPreviewContent(String id) {
        Template template = get(id);
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

    private void prepare(Template param, boolean requireEnabledType) {
        TemplateType templateType = getTemplateType(param.getTemplateTypeId(), requireEnabledType);
        if (!StringUtils.hasText(param.getObjectName())) {
            param.setObjectName(defaultObjectName(templateType.getCode(), param.getTemplateCode()));
        } else {
            param.setObjectName(trimLeadingSlash(param.getObjectName().trim()));
        }
    }

    private TemplateType getTemplateType(String id, boolean requireEnabled) {
        TemplateType templateType = templateTypeDao.get(id);
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

}
