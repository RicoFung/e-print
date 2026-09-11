package com.eprint.admin.repository.model.result;

import com.eprint.admin.repository.model.entity.Template;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateResult extends Template {
    private static final long serialVersionUID = 1L;

    private String templateTypeCode;
    private String templateTypeName;

}
