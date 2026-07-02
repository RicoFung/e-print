package com.eprint.admin.repository.model.param;

import com.eprint.admin.common.model.page.PageParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateTypeQueryParam extends PageParam {

    private String keyword;
    private Integer status;

}
