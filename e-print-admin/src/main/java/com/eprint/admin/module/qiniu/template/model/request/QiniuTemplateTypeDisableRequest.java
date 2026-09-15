package com.eprint.admin.module.qiniu.template.model.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QiniuTemplateTypeDisableRequest {

    private String id;
    private List<String> ids;

}
