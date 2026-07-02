package com.eprint.admin.module.template.model.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TemplateEnableRequest {

    private String id;
    private List<String> ids;

}
