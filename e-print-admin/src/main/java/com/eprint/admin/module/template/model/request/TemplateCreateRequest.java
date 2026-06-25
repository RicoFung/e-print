package com.eprint.admin.module.template.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TemplateCreateRequest {

    @NotBlank(message = "Template type is required")
    private String templateTypeId;

    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Template code must be at most 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Template code can only contain letters, numbers, underscore, and hyphen")
    private String templateCode;

    @NotBlank(message = "Bucket name is required")
    @Size(max = 100, message = "Bucket name must be at most 100 characters")
    private String bucketName;

    @Size(max = 500, message = "Object name must be at most 500 characters")
    private String objectName;

    @NotNull(message = "Status is required")
    private Integer status = 1;

    @NotBlank(message = "Template content is required")
    private String content;

    public String getTemplateTypeId() {
        return templateTypeId;
    }

    public void setTemplateTypeId(String templateTypeId) {
        this.templateTypeId = templateTypeId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
