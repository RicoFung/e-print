package com.eprint.admin.repository.minio.model.result;

import com.eprint.admin.repository.minio.model.entity.MinioTemplate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MinioTemplateResult extends MinioTemplate {
    private static final long serialVersionUID = 1L;

    private String templateTypeCode;
    private String templateTypeName;

}
