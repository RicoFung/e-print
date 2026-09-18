package com.eprint.admin.repository.qiniu.model.param;

import com.eprint.admin.common.model.page.PageParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QiniuTemplateTypeQueryParam extends PageParam {

    private String keyword;
    private Integer status;

}
