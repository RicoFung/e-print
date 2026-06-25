package com.eprint.admin.repository.model.result;

import com.eprint.admin.repository.model.entity.Template;

public class TemplateResult extends Template {
    private static final long serialVersionUID = 1L;

    private String templateType;
    private String templateTypeName;

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getTemplateTypeName() {
        return templateTypeName;
    }

    public void setTemplateTypeName(String templateTypeName) {
        this.templateTypeName = templateTypeName;
    }
}
