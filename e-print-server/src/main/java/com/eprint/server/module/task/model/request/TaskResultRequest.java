package com.eprint.server.module.task.model.request;

import jakarta.validation.constraints.NotBlank;

public class TaskResultRequest {

    @NotBlank
    private String status;

    private String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
