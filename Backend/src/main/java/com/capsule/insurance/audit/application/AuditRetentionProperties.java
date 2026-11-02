package com.capsule.insurance.audit.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 감사로그 정리 정책입니다. 실제 보존 기간은 서비스의 법적·운영 정책을 확인한 뒤 환경변수로 설정합니다.
 * 기본값은 실수로 기록을 제거하지 않도록 비활성입니다.
 */
@Component
@ConfigurationProperties(prefix = "audit.retention")
public class AuditRetentionProperties {

    private boolean enabled;
    private int days = 365;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public void validate() {
        if (days < 1) {
            throw new IllegalStateException("감사로그 보존 기간은 하루 이상이어야 합니다.");
        }
    }
}
