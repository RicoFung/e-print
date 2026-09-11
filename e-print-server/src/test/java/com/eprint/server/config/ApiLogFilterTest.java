package com.eprint.server.config;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ApiLogFilterTest {

    @Test
    void preservesResponseAndAddsRequestId() throws Exception {
        ApiLogFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/task");
        request.setContentType("application/json");
        request.setContent("{\"clientId\":\"CLIENT-001\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
                    throws java.io.IOException {
                servletResponse.setContentType("application/json");
                servletResponse.getWriter().write("{\"status\":\"SUCCESS\"}");
            }
        });

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(response.getContentAsString()).isEqualTo("{\"status\":\"SUCCESS\"}");
    }

    @Test
    void keepsCallerRequestId() throws Exception {
        ApiLogFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/task");
        request.addHeader("X-Request-Id", "REQ-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("REQ-001");
    }

    @Test
    void masksSensitiveJsonValuesAndTruncatesLongBodies() {
        ApiLogFilter filter = filter();
        ReflectionTestUtils.setField(filter, "maxBodyLength", 20);

        String masked = filter.mask("{\"password\":\"secret-value\",\"name\":\"printer\"}");

        assertThat(masked).doesNotContain("secret-value").contains("\"password\":\"***\"");
        assertThat(filter.maskQuery("clientId=1&token=secret-value"))
                .isEqualTo("clientId=1&token=***");
        assertThat(filter.truncate(masked)).endsWith("...<truncated>");
    }

    private ApiLogFilter filter() {
        ApiLogFilter filter = new ApiLogFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "maxBodyLength", 8192);
        return filter;
    }
}
