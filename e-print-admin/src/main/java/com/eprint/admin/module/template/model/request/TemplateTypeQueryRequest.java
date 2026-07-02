package com.eprint.admin.module.template.model.request;

import com.eprint.admin.common.model.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateTypeQueryRequest extends PageRequest {

    private String keyword;
    private Integer status;

    public String queryKeyword() {
        return keyword == null || keyword.isBlank() ? getSearch() : keyword;
    }

}
