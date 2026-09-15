package com.eprint.admin.module.minio.template.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MinioTemplatePreviewRequest {

    private String id;
    private String content;
    private String sampleData;

}
