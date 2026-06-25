package com.eprint.admin.module.template.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TemplateTypeCreateRequest {

    @NotBlank(message = "Type code is required")
    @Size(max = 64, message = "Type code must be at most 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Type code can only contain letters, numbers, underscore, and hyphen")
    private String code;

    @NotBlank(message = "Type name is required")
    @Size(max = 100, message = "Type name must be at most 100 characters")
    private String name;

    @NotNull(message = "Status is required")
    private Integer status = 1;

    @NotNull(message = "Sort number is required")
    private Integer sortNo = 0;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
