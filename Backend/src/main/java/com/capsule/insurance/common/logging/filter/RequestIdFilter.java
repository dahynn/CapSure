package com.capsule.insurance.common.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(1)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestId = resolveRequestId(request);
        String sourceIp = resolveSourceIp(request);
        
        // 마스킹 로직 정교화 (경로 유지, 파라미터 값만 마스킹)
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String displayUri = uri;
        if (StringUtils.hasText(queryString)) {
            String maskedQuery = maskSensitiveParams(queryString);
            displayUri = uri + "?" + maskedQuery;
        }

        MDC.put("requestId", requestId);
        // MDC userId 바인딩 시점 조정 (doFilter 이전 수행)
        MDC.put("userId", resolveUserId());
        MDC.put("sourceIp", sourceIp);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        log.info("API START: method={}, uri={}", request.getMethod(), displayUri);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info(
                    "API END: method={}, uri={}, status={}, elapsedMs={}",
                    request.getMethod(),
                    displayUri,
                    response.getStatus(),
                    elapsedMs
            );
            MDC.clear();
        }
    }

    private String maskSensitiveParams(String queryString) {
        if (!StringUtils.hasText(queryString)) {
            return queryString;
        }
        // password, token, secret 등의 파라미터 값을 ***로 치환
        return queryString.replaceAll("(?i)(password|token|secret|credentials)=([^&]+)", "$1=***");
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
    }

    private String resolveSourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
