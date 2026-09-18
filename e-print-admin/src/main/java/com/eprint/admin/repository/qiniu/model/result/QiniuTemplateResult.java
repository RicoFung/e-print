package com.eprint.admin.repository.qiniu.model.result;

import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QiniuTemplateResult extends QiniuTemplate {
    private static final long serialVersionUID = 1L;

    private String templateTypeCode;
    private String templateTypeName;

}
