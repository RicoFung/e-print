package com.eprint.admin.module.minio.template.model.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MinioTemplateTypeEnableRequest {

    private String id;
    private List<String> ids;

}
