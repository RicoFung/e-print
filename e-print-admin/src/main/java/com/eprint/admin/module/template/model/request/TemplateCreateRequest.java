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
public class TemplateCreateRequest {

    @NotBlank(message = "Template type is required")
    private String templateTypeId;

    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Template code must be at most 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Template code can only contain letters, numbers, underscore, and hyphen")
    private String templateCode;

    @NotBlank(message = "Bucket name is required")
    @Size(max = 100, message = "Bucket name must be at most 100 characters")
    private String bucketName;

    @NotBlank(message = "Object name is required")
    @Size(max = 500, message = "Object name must be at most 500 characters")
    private String objectName;

    @NotNull(message = "Status is required")
    @Min(value = 0, message = "Status must be 0 or 1")
    @Max(value = 1, message = "Status must be 0 or 1")
    private Integer status = 1;

    @NotBlank(message = "Template content is required")
    private String content;

}
