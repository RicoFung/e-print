package com.eprint.admin.repository.model.param;

import com.eprint.admin.common.model.page.PageParam;

public class TemplateQueryParam extends PageParam {

    private String templateTypeId;
    private String templateCode;
    private Integer status;

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
