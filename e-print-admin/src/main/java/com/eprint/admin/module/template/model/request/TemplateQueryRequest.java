package com.eprint.admin.module.template.model.request;

import com.eprint.admin.common.model.page.PageRequest;

public class TemplateQueryRequest extends PageRequest {

    private String templateTypeId;
    private String templateCode;
    private Integer status;

    public String queryTemplateCode() {
        return templateCode == null || templateCode.isBlank() ? getSearch() : templateCode;
    }

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
