package com.eprint.admin.module.template.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateTypeModifyRequest {

    @NotBlank(message = "Type id is required")
    private String id;

    @NotBlank(message = "Type code is required")
    @Size(max = 64, message = "Type code must be at most 64 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Type code can only contain letters, numbers, underscore, and hyphen")
    private String code;

    @NotBlank(message = "Type name is required")
    @Size(max = 100, message = "Type name must be at most 100 characters")
    private String name;

    @NotNull(message = "Status is required")
    @Min(value = 0, message = "Status must be 0 or 1")
    @Max(value = 1, message = "Status must be 0 or 1")
    private Integer status = 1;

    @NotNull(message = "Sort number is required")
    private Integer sortNo = 0;

}
