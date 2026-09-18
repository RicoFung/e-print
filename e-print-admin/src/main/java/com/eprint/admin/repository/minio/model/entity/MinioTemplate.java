package com.eprint.admin.repository.minio.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class MinioTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String templateTypeId;
    private String templateCode;
    private String bucketName;
    private String objectName;
    private Integer status;

}
