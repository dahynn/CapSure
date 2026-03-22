package com.capsule.insurance.auth.application;

import com.capsule.insurance.auth.domain.EmailVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final EmailVerificationRepository emailVerificationRepository;

    @Value("${spring.mail.username:capsule@capsule.com}")
    private String fromEmail;

    private static final long AUTH_CODE_EXPIRATION_MINUTES = 3; // 인증 번호 유효 시간: 3분
    private static final long VERIFIED_EXPIRATION_MINUTES = 30; // 인증 완료 후 유지 시간: 30분

    public EmailService(JavaMailSender javaMailSender, EmailVerificationRepository emailVerificationRepository) {
        this.javaMailSender = javaMailSender;
        this.emailVerificationRepository = emailVerificationRepository;
    }

    public void sendVerificationCode(String email) {
        String authCode = generateAuthCode();
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("[Capsule] 회원가입 이메일 인증 번호"); // 메일 제목
        message.setText("인증 번호는 [" + authCode + "] 입니다.\n3분 이내에 입력해주세요."); // 메일 내용
        
        javaMailSender.send(message);
        
        emailVerificationRepository.saveAuthCode(email, authCode, AUTH_CODE_EXPIRATION_MINUTES);
    }

    public boolean verifyCode(String email, String inputCode) {
        String savedCode = emailVerificationRepository.getAuthCode(email);
        
        if (savedCode != null && savedCode.equals(inputCode)) {
            emailVerificationRepository.markAsVerified(email, VERIFIED_EXPIRATION_MINUTES);
            return true;
        }
        
        return false;
    }

    public boolean isEmailVerified(String email) {
        return emailVerificationRepository.isVerified(email);
    }

    public void completeSignup(String email) {
        emailVerificationRepository.removeVerified(email);
    }

    private String generateAuthCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
