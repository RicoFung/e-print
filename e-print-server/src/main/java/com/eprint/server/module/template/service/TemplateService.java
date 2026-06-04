package com.eprint.server.module.template.service;

import com.eprint.server.repository.dao.TemplateDao;
import com.eprint.server.repository.model.param.TemplateGetByCodeParam;
import com.eprint.server.repository.model.result.TemplateResult;
import com.niko.boot.model.result.NikoResult;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service(value = "TemplateService")
public class TemplateService {

    private static final Integer STATUS_ENABLED = 1;

    @Autowired
    private TemplateDao dao;

    @Autowired
    private MinioClient minioClient;

    public NikoResult getByTemplateCode(String templateCode) {
        String templateContent;
        try {
            templateContent = getTemplateContentByCode(templateCode);
        } catch (IllegalArgumentException e) {
            log.info("Print template not found, templateCode={}", templateCode);
            return NikoResult.error(e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Read print template object failed, templateCode={}", templateCode, e);
            return NikoResult.error(e.getMessage());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateCode", templateCode);
        data.put("content", templateContent);
        return NikoResult.data(data);
    }

    public String getTemplateContentByCode(String templateCode) {
        TemplateGetByCodeParam param = new TemplateGetByCodeParam();
        param.setTemplateCode(templateCode);
        param.setStatus(STATUS_ENABLED);

        TemplateResult result = dao.getByTemplateCode(param);
        if (result == null) {
            throw new IllegalArgumentException("Template not found");
        }

        try {
            return getObjectContent(result.getBucketName(), result.getObjectName());
        } catch (Exception e) {
            log.error("Read print template object failed, templateCode={}, bucketName={}, objectName={}",
                    templateCode, result.getBucketName(), result.getObjectName(), e);
            throw new IllegalStateException("Template object not found", e);
        }
    }

    private String getObjectContent(String bucketName, String objectName) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build();
        try (InputStream inputStream = minioClient.getObject(args)) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
