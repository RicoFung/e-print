package com.eprint.server.module.qiniu.template.service;

import com.eprint.server.repository.qiniu.template.dao.QiniuTemplateDao;
import com.eprint.server.repository.qiniu.template.model.param.QiniuTemplateGetByCodeParam;
import com.eprint.server.repository.qiniu.template.model.result.QiniuTemplateResult;
import com.niko.boot.model.result.NikoResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class QiniuTemplateService {

    private static final Integer STATUS_ENABLED = 1;
    private static final String DEFAULT_TEMPLATE_CODE = "01";

    @Autowired
    private QiniuTemplateDao dao;

    @Autowired
    private QiniuObjectStorage objectStorage;

    public NikoResult getByTemplateCode(String templateType, String templateCode) {
        String templateContent;
        QiniuTemplateResult template;
        try {
            template = resolveTemplate(templateType, templateCode);
            templateContent = readTemplateContent(template);
        } catch (IllegalArgumentException e) {
            log.info("Print template not found, templateType={}, templateCode={}", templateType, templateCode);
            return NikoResult.error(e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Read print template object failed, templateType={}, templateCode={}", templateType, templateCode, e);
            return NikoResult.error(e.getMessage());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateType", template.getTemplateType());
        data.put("templateCode", template.getTemplateCode());
        data.put("content", templateContent);
        return NikoResult.data(data);
    }

    public String getTemplateContentByCode(String templateType, String templateCode) {
        return readTemplateContent(resolveTemplate(templateType, templateCode));
    }

    public QiniuTemplateResult resolveTemplate(String templateType, String templateCode) {
        QiniuTemplateGetByCodeParam param = new QiniuTemplateGetByCodeParam();
        param.setTemplateType(templateType);
        param.setTemplateCode(templateCode);
        param.setStatus(STATUS_ENABLED);

        QiniuTemplateResult result = dao.getByTemplateCode(param);
        if (result == null && !DEFAULT_TEMPLATE_CODE.equals(templateCode)) {
            param.setTemplateCode(DEFAULT_TEMPLATE_CODE);
            result = dao.getByTemplateCode(param);
        }
        if (result == null) {
            throw new IllegalArgumentException("Template not found");
        }
        return result;
    }

    private String readTemplateContent(QiniuTemplateResult result) {
        try {
            return getObjectContent(result.getBucketName(), result.getObjectName());
        } catch (Exception e) {
            log.error("Read print template object failed, templateType={}, templateCode={}, bucketName={}, objectName={}",
                    result.getTemplateType(), result.getTemplateCode(), result.getBucketName(), result.getObjectName(), e);
            throw new IllegalStateException("Template object not found", e);
        }
    }

    private String getObjectContent(String bucketName, String objectName) throws Exception {
        return objectStorage.read(bucketName, objectName);
    }
}
