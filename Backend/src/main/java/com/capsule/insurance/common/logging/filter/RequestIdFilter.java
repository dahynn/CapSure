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
import org.springframework.web.servlet.HandlerMapping;

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
        long startTime = System.nanoTime();
        String requestId = resolveRequestId(request);
        String sourceIp = resolveSourceIp(request);
        
        MDC.put("requestId", requestId);
        // MDC userId 바인딩 시점 조정 (doFilter 이전 수행)
        MDC.put("userId", resolveUserId());
        MDC.put("sourceIp", sourceIp);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Raw query/path values may contain payment keys, email addresses or identifiers.
        log.info("API START: method={}", request.getMethod());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            log.info(
                    "API END: method={}, uri={}, status={}, elapsedMs={}",
                    request.getMethod(),
                    route == null ? "[unmatched]" : route.toString(),
                    response.getStatus(),
                    elapsedMs
            );
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId != null && requestId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                ? requestId : UUID.randomUUID().toString();
    }

    private String resolveSourceIp(HttpServletRequest request) {
        // Forwarded header trust belongs to the configured reverse proxy/container.
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
