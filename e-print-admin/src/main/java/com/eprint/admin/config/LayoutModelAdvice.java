package com.eprint.admin.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LayoutModelAdvice {

    @ModelAttribute("currentRequestUri")
    public String currentRequestUri(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
    }
}
