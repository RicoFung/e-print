package com.eprint.server.repository.model.param;

import java.io.Serializable;

public class TemplateGetByCodeParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private String templateCode; // db_column: TEMPLATE_CODE
    private Integer status; // db_column: STATUS

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
