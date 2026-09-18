package com.eprint.admin.config;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TracingInterceptorTest {

    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";
    private static final String SPAN_ID = "0123456789abcdef";

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesB3HeadersAndClearsMdcAfterCompletion() {
        TracingInterceptor interceptor = interceptor();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-B3-TraceId", TRACE_ID.toUpperCase());
        request.addHeader("X-B3-SpanId", SPAN_ID.toUpperCase());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(MDC.get("traceId")).isEqualTo(TRACE_ID);
        assertThat(MDC.get("spanId")).isEqualTo(SPAN_ID);
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo(TRACE_ID);
        assertThat(response.getHeader("X-Span-Id")).isEqualTo(SPAN_ID);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("spanId")).isNull();
    }

    @Test
    void generatesTraceIdsWhenHeadersAreMissing() {
        TracingInterceptor interceptor = interceptor();
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(new MockHttpServletRequest(), response, new Object());

        assertThat(response.getHeader("X-Trace-Id")).matches("[0-9a-f]{32}");
        assertThat(response.getHeader("X-Span-Id")).matches("[0-9a-f]{16}");
    }

    @SuppressWarnings("unchecked")
    private TracingInterceptor interceptor() {
        ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
        when(tracerProvider.getIfAvailable()).thenReturn(null);
        return new TracingInterceptor(tracerProvider);
    }
}
