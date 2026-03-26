package com.capsule.insurance.auth.application;

import com.capsule.insurance.auth.domain.PhoneAuthRepository;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class SmsService {
    private final DefaultMessageService messageService;
    private final PhoneAuthRepository phoneAuthRepository;
    private final String senderNumber;

    public SmsService(
            @Value("${coolsms.api-key}") String apiKey,
            @Value("${coolsms.api-secret}") String apiSecret,
            @Value("${coolsms.from-number}") String senderNumber,
            PhoneAuthRepository phoneAuthRepository) {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
        this.senderNumber = senderNumber;
        this.phoneAuthRepository = phoneAuthRepository;
    }

    public void sendVerificationCode(String phone) {
        String code = generateCode();
        
        String targetPhone = phone.replaceAll("[^0-9]", "");
        String fromPhone = senderNumber.replaceAll("[^0-9]", "");
        
        phoneAuthRepository.save(phone, code); // 조회용 키는 원래 요청된 phone 유지
        
        Message message = new Message();
        message.setFrom(fromPhone);
        message.setTo(targetPhone);
        message.setText("[Capsure] 인증번호는 [" + code + "] 입니다.");
    }

    public boolean verifyCode(String phone, String code) {
        String savedCode = phoneAuthRepository.findByPhone(phone);
        if (savedCode != null && savedCode.equals(code)) {
            phoneAuthRepository.saveVerifiedPhone(phone);
            phoneAuthRepository.deleteByPhone(phone);
            return true;
        }
        return false;
    }

    public boolean isPhoneVerified(String phone) {
        return phoneAuthRepository.isPhoneVerified(phone);
    }

    public void completeSignup(String phone) {
        phoneAuthRepository.deleteVerifiedPhone(phone);
    }

    private String generateCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
}
