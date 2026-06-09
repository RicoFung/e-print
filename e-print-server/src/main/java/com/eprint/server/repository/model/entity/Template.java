package com.eprint.server.repository.model.entity;

import java.io.Serializable;

/**
 * Template entity.
 * db_table: E_PRINT_TEMPLATE
 */
public class Template implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // db_column: ID
    private String templateType; // db_column: TEMPLATE_TYPE
    private String templateCode; // db_column: TEMPLATE_CODE
    private String bucketName; // db_column: BUCKET_NAME
    private String objectName; // db_column: OBJECT_NAME
    private Integer status; // db_column: STATUS

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
