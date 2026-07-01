package com.eprint.admin.repository.model.param;

import com.eprint.admin.common.model.page.PageParam;

public class TemplateTypeQueryParam extends PageParam {

    private String keyword;
    private Integer status;

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
