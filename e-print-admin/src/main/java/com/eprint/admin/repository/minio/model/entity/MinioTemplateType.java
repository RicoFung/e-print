package com.eprint.admin.repository.minio.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class MinioTemplateType implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private Integer status;
    private Integer sortNo;

}
