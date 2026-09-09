package com.capsule.insurance.assistantai.claim.application;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 실제 외부 요청을 만들지 않고 공급자 연결 전제만 점검합니다. */
@Service
public class ClaimReviewCopilotReadinessService {

    private final ClaimReviewCopilotProperties properties;

    public ClaimReviewCopilotReadinessService(ClaimReviewCopilotProperties properties) {
        this.properties = properties;
    }

    public Readiness readiness() {
        String provider = properties.getProvider().trim().toLowerCase(java.util.Locale.ROOT);
        List<String> blockers = new ArrayList<>();
        if ("blocked".equals(provider)) {
            blockers.add("외부 공급자가 비활성화되어 있습니다.");
        } else if ("demo".equals(provider)) {
            blockers.add("현재는 외부 API가 아닌 데모 공급자를 사용 중입니다.");
        } else if ("external".equals(provider)) {
            if (!StringUtils.hasText(properties.getBaseUrl())) blockers.add("공급자 base URL이 필요합니다.");
            if (!StringUtils.hasText(properties.getModel())) blockers.add("공급자 모델 ID가 필요합니다.");
            if (!StringUtils.hasText(properties.getApiKey())) blockers.add("환경변수 API 키가 필요합니다.");
            if (!properties.isAllowExternalCalls()) blockers.add("외부 호출 허용 플래그가 꺼져 있습니다.");
            blockers.add("공급자 HTTP 어댑터는 API 확정 후에만 활성화합니다.");
        } else {
            blockers.add("지원하지 않는 공급자 설정입니다.");
        }
        return new Readiness(provider, "external".equals(provider), blockers.isEmpty(), List.copyOf(blockers));
    }

    public record Readiness(String provider, boolean externalProviderSelected, boolean ready, List<String> blockers) {
    }
}
