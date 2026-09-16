package com.eprint.server.repository.minio.template.model.param;

import java.io.Serializable;

public class MinioTemplateGetByCodeParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private String templateType; // db_column: E_PRINT_MINIO_TEMPLATE_TYPE.CODE
    private String templateCode; // db_column: E_PRINT_MINIO_TEMPLATE.CODE
    private Integer status; // db_column: STATUS

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
