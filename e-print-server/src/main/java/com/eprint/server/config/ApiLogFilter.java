package com.eprint.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiLogFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern SENSITIVE_JSON_VALUE = Pattern.compile(
            "(?i)(\\\"(?:password|accessToken|refreshToken|token|secret|authorization)\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")");
    private static final Pattern SENSITIVE_QUERY_VALUE = Pattern.compile(
            "(?i)((?:^|&)(?:password|accessToken|refreshToken|token|secret|authorization)=)([^&]*)");

    @Value("${app.api-log.enabled:true}")
    private boolean enabled;

    @Value("${app.api-log.max-body-length:8192}")
    private int maxBodyLength;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || request.getRequestURI().startsWith("/ws/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(
                request, Math.max(maxBodyLength + 1, 1));
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        String requestId = resolveRequestId(request);
        long startNanos = System.nanoTime();

        MDC.put(REQUEST_ID, requestId);
        responseWrapper.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            restoreTracingContext(responseWrapper);
            log.info("API\n"
                            + "  Request      : {} {} | query={} | clientIp={}\n"
                            + "  Response     : status={} | duration={}ms\n"
                            + "  Request body : {}\n"
                            + "  Response body: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    maskQuery(request.getQueryString()),
                    resolveClientIp(request),
                    responseWrapper.getStatus(),
                    (System.nanoTime() - startNanos) / 1_000_000,
                    readBody(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding(),
                            request.getContentType()),
                    readBody(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding(),
                            responseWrapper.getContentType()));
            try {
                responseWrapper.copyBodyToResponse();
            } finally {
                MDC.remove(REQUEST_ID);
                MDC.remove(TRACE_ID);
                MDC.remove(SPAN_ID);
            }
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || !requestId.matches("[a-zA-Z0-9._-]{1,128}")) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void restoreTracingContext(HttpServletResponse response) {
        putIfPresent(TRACE_ID, response.getHeader("X-Trace-Id"));
        putIfPresent(SPAN_ID, response.getHeader("X-Span-Id"));
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private String readBody(byte[] content, String encoding, String contentType) {
        if (content.length == 0) {
            return "-";
        }
        if (!isTextContent(contentType)) {
            return "<" + content.length + " bytes>";
        }

        Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        return truncate(mask(new String(content, charset)).replaceAll("[\\r\\n]+", " "));
    }

    private boolean isTextContent(String contentType) {
        if (contentType == null) {
            return true;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return "text".equals(mediaType.getType())
                    || MediaType.APPLICATION_JSON.includes(mediaType)
                    || MediaType.APPLICATION_XML.includes(mediaType)
                    || MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    String mask(String body) {
        return SENSITIVE_JSON_VALUE.matcher(body).replaceAll("$1***$3");
    }

    String maskQuery(String query) {
        return query == null || query.isBlank()
                ? "-"
                : SENSITIVE_QUERY_VALUE.matcher(query).replaceAll("$1***");
    }

    String truncate(String body) {
        int limit = Math.max(maxBodyLength, 0);
        return body.length() <= limit ? body : body.substring(0, limit) + "...<truncated>";
    }

}
