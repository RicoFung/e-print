package com.eprint.admin.repository.model.param;

import com.eprint.admin.common.model.page.PageParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateQueryParam extends PageParam {

    private String templateTypeId;
    private String templateCode;
    private Integer status;

}
