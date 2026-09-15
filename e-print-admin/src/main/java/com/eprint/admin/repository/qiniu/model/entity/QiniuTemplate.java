package com.eprint.admin.repository.qiniu.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class QiniuTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String templateTypeId;
    private String templateCode;
    private String bucketName;
    private String objectName;
    private Integer status;

}
