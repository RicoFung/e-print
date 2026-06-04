package com.eprint.server.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class TracingInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String UNKNOWN = "unknown";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ObjectProvider<Tracer> tracerProvider;

    public TracingInterceptor(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TraceIds traceIds = resolveTraceIds(request);

        MDC.put(TRACE_ID, traceIds.traceId());
        MDC.put(SPAN_ID, traceIds.spanId());
        response.setHeader("X-Trace-Id", traceIds.traceId());
        response.setHeader("X-Span-Id", traceIds.spanId());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
    }

    private TraceIds resolveTraceIds(HttpServletRequest request) {
        TraceIds fromB3 = resolveFromB3(request);
        if (fromB3 != null) {
            return fromB3;
        }

        TraceIds fromCustomHeader = resolveFromCustomHeader(request);
        if (fromCustomHeader != null) {
            return fromCustomHeader;
        }

        TraceIds fromTraceParent = resolveFromTraceParent(request.getHeader("traceparent"));
        if (fromTraceParent != null) {
            return fromTraceParent;
        }

        TraceIds fromCurrentSpan = resolveFromCurrentSpan();
        if (fromCurrentSpan != null) {
            return fromCurrentSpan;
        }

        return new TraceIds(randomHex(32), randomHex(16));
    }

    private TraceIds resolveFromB3(HttpServletRequest request) {
        String traceId = normalizeHex(request.getHeader("X-B3-TraceId"), 16, 32);
        String spanId = normalizeHex(request.getHeader("X-B3-SpanId"), 16, 16);
        if (traceId == null || spanId == null) {
            return null;
        }

        return new TraceIds(traceId, spanId);
    }

    private TraceIds resolveFromCustomHeader(HttpServletRequest request) {
        String traceId = normalizeValue(request.getHeader("X-Trace-Id"));
        String spanId = normalizeValue(request.getHeader("X-Span-Id"));
        if (traceId == null || spanId == null) {
            return null;
        }

        return new TraceIds(traceId, spanId);
    }

    private TraceIds resolveFromTraceParent(String traceParent) {
        if (!isNotBlank(traceParent)) {
            return null;
        }

        String[] parts = traceParent.split("-");
        if (parts.length < 4) {
            return null;
        }

        String traceId = normalizeHex(parts[1], 32, 32);
        String spanId = normalizeHex(parts[2], 16, 16);
        if (traceId == null || spanId == null) {
            return null;
        }

        return new TraceIds(traceId, spanId);
    }

    private TraceIds resolveFromCurrentSpan() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            return null;
        }

        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return null;
        }

        TraceContext context = currentSpan.context();
        if (context == null || !isNotBlank(context.traceId()) || !isNotBlank(context.spanId())) {
            return null;
        }

        return new TraceIds(context.traceId(), context.spanId());
    }

    private String normalizeHex(String value, int minLength, int maxLength) {
        String normalized = normalizeValue(value);
        if (normalized == null || normalized.length() < minLength || normalized.length() > maxLength) {
            return null;
        }

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return null;
            }
        }

        return normalized;
    }

    private String normalizeValue(String value) {
        if (!isNotBlank(value)) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return UNKNOWN.equals(normalized) ? null : normalized;
    }

    private static String randomHex(int length) {
        byte[] bytes = new byte[length / 2];
        RANDOM.nextBytes(bytes);

        StringBuilder builder = new StringBuilder(length);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record TraceIds(String traceId, String spanId) {
    }
}
