package com.capsule.insurance.auth.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    public void sendVerificationCode(String phone) {
        // 사용자 요청: 실제 SMS 발송은 우선 비활성화한다.
        // 재개 시 발송 어댑터·인증 만료·시도 제한을 구현한 뒤 다음 호출을 연결한다.
        // messageService.sendOne(new SingleMessageSendingRequest(message));
        throw disabled();
    }

    public boolean verifyCode(String phone, String code) {
        throw disabled();
    }

    private BusinessException disabled() {
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "휴대폰 문자 인증은 현재 제공하지 않습니다.");
    }
}
