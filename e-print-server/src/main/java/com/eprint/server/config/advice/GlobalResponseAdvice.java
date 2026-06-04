package com.eprint.server.config.advice;

import com.niko.boot.model.result.NikoResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (NikoResult.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return true;
        }

        HttpServletRequest request = attributes.getRequest();
        String path = request.getRequestURI();
        return !isExcludedPath(path);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        NikoResult result = NikoResult.data(body);
        result.set("message", result.getMsg());

        if (String.class.equals(returnType.getGenericParameterType())) {
            try {
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                throw new IllegalStateException("Serialize response failed", e);
            }
        }

        return result;
    }

    private boolean isExcludedPath(String path) {
        return path == null
                || path.contains("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator")
                || path.startsWith("/error");
    }
}
