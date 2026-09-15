package com.eprint.admin.module.qiniu.template.model.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QiniuTemplateRemoveRequest {

    private String id;
    private List<String> ids;

}
