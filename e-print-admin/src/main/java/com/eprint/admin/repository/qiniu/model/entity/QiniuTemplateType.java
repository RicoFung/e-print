package com.eprint.admin.repository.qiniu.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class QiniuTemplateType implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private Integer status;
    private Integer sortNo;

}
