// #Demo Setting
package com.capsule.insurance.common.logging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class RequestIdFilterTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .addFilters(new RequestIdFilter())
            .build();

    @Test
    void requestIdIsGeneratedAndReturnedInResponseHeader() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/filter-test"))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = mvcResult.getResponse().getHeader("X-Request-Id");

        assertThat(requestId).isNotBlank();
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("ok");
    }

    @RestController
    static class TestController {

        @GetMapping("/filter-test")
        String ping() {
            return "ok";
        }

        @GetMapping("/filter-test/{identifier}")
        String path() { return "ok"; }
    }

    @Test
    void rawQueryPathAndUntrustedRequestIdAreExcludedFromLogs() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RequestIdFilter.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>() {
            @Override protected void append(ch.qos.logback.classic.spi.ILoggingEvent event) {
                event.prepareForDeferredProcessing();
                super.append(event);
            }
        };
        appender.start(); logger.addAppender(appender);
        try {
            var result = mockMvc.perform(get("/filter-test/synthetic-path-secret")
                            .queryParam("paymentKey", "synthetic-payment-secret")
                            .queryParam("email", "synthetic@example.test")
                            .queryParam("access_token", "synthetic-access-secret")
                            .header("X-Request-Id", "untrusted-secret\nforged-log")
                            .header("X-Forwarded-For", "untrusted-forwarded-secret"))
                    .andExpect(status().isOk()).andReturn();
            String output = appender.list.stream().map(e -> e.getFormattedMessage())
                    .collect(java.util.stream.Collectors.joining("\n"));
            assertThat(output).contains("/filter-test/{identifier}")
                    .doesNotContain("synthetic-path-secret", "synthetic-payment-secret", "synthetic@example.test",
                            "synthetic-access-secret", "untrusted-secret", "forged-log", "untrusted-forwarded-secret");
            assertThat(result.getResponse().getHeader("X-Request-Id"))
                    .matches("[0-9a-f-]{36}");
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getMDCPropertyMap().get("requestId"))
                        .isEqualTo(result.getResponse().getHeader("X-Request-Id"));
                assertThat(event.getMDCPropertyMap().get("sourceIp"))
                        .isEqualTo("127.0.0.1");
            });
            assertThat(org.slf4j.MDC.getCopyOfContextMap()).isNullOrEmpty();
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}
