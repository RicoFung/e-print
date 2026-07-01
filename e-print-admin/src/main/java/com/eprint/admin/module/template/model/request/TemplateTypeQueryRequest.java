package com.eprint.admin.module.template.model.request;

import com.eprint.admin.common.model.page.PageRequest;

public class TemplateTypeQueryRequest extends PageRequest {

    private String keyword;
    private Integer status;

    public String queryKeyword() {
        return keyword == null || keyword.isBlank() ? getSearch() : keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
