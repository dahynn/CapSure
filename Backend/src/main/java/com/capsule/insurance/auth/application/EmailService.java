package com.capsule.insurance.auth.application;

import com.capsule.insurance.auth.domain.EmailVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;

import java.security.SecureRandom;

@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username:capsule@capsule.com}")
    private String fromEmail;

    private static final long AUTH_CODE_EXPIRATION_MINUTES = 3; // 인증 번호 유효 시간: 3분
    private static final long VERIFIED_EXPIRATION_MINUTES = 30; // 인증 완료 후 유지 시간: 30분

    public EmailService(ObjectProvider<JavaMailSender> javaMailSender, EmailVerificationRepository emailVerificationRepository,
                        PasswordEncoder passwordEncoder) {
        this.javaMailSender = javaMailSender.getIfAvailable();
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendVerificationCode(String email) {
        if (javaMailSender == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "이메일 발송 설정이 없습니다. 운영자에게 문의해주세요.");
        }
        String authCode = generateAuthCode();
        String codeHash = passwordEncoder.encode(authCode);
        if (!emailVerificationRepository.reserveCode(email, codeHash, AUTH_CODE_EXPIRATION_MINUTES)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "인증 메일은 60초 후 다시 요청할 수 있습니다.");
        }
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("[Capsule] 회원가입 이메일 인증 번호"); // 메일 제목
        message.setText("인증 번호는 [" + authCode + "] 입니다.\n3분 이내에 입력해주세요."); // 메일 내용
        
        try {
            javaMailSender.send(message);
        } catch (RuntimeException exception) {
            emailVerificationRepository.invalidateCode(email, codeHash);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "인증 메일을 발송하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    public boolean verifyCode(String email, String inputCode) {
        return emailVerificationRepository.verifyCode(email, inputCode, VERIFIED_EXPIRATION_MINUTES);
    }

    public boolean isEmailVerified(String email) {
        return emailVerificationRepository.isVerified(email);
    }

    public boolean consumeVerified(String email) {
        return emailVerificationRepository.consumeVerified(email);
    }

    private String generateAuthCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
