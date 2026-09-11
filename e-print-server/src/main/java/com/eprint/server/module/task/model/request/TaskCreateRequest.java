package com.eprint.server.module.task.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaskCreateRequest {

    @NotBlank(message = "客户端 ID 【clientId】不能为空")
    private String clientId;

    @NotBlank(message = "模板类型【templateType】不能为空")
    private String templateType;

    @NotBlank(message = "模板编码【templateCode】不能为空")
    private String templateCode;

    @Min(value = 1, message = "打印份数【copies】不能小于 1")
    private int copies = 1;

    private Map<String, Object> data = new LinkedHashMap<>();

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data == null ? new LinkedHashMap<>() : data;
    }
}
