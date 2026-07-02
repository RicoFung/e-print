package com.eprint.admin.module.template.model.request;

import com.eprint.admin.common.model.page.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateQueryRequest extends PageRequest {

    private String templateTypeId;
    private String templateCode;
    private Integer status;

    public String queryTemplateCode() {
        return templateCode == null || templateCode.isBlank() ? getSearch() : templateCode;
    }

}
