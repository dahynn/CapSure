package com.capsule.insurance.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SafeFailureTest {
    @Test void nestedProviderPayloadAndSqlAreNotStoredOrLogged() throws Exception {
        var failure = new IllegalStateException("accessToken=synthetic-secret",
                new IllegalArgumentException("email=person@example.test SELECT card_number FROM usr_user"));
        assertThat(SafeFailure.describe(failure)).isEqualTo("UNEXPECTED_ERROR:IllegalArgumentException");
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            var response = new GlobalExceptionHandler().handleUnhandledException(failure);
            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(response.getBody()))
                    .doesNotContain("synthetic-secret", "person@example.test", "SELECT");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage())
                    .doesNotContain("synthetic-secret", "person@example.test", "SELECT");
            assertThat(appender.list.getFirst().getThrowableProxy()).isNull();
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    @Test void cyclicCausesTerminateAndBusinessCodesRemainUseful() {
        var first = new IllegalStateException("secret-one");
        var second = new IllegalArgumentException("secret-two");
        first.initCause(second); second.initCause(first);
        assertThat(SafeFailure.describe(first)).startsWith("UNEXPECTED_ERROR:").doesNotContain("secret");
        assertThat(SafeFailure.describe(new BusinessException(ErrorCode.INVALID_INPUT, "private value")))
                .isEqualTo("INVALID_INPUT:BusinessException");
    }
}
