package com.capsule.insurance.auth.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.capsule.insurance.auth.domain.EmailVerificationRepository;
import com.capsule.insurance.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class EmailServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void deliveryFailureInvalidatesReservedCode() {
        var provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        var sender = mock(JavaMailSender.class);
        var repository = mock(EmailVerificationRepository.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(repository.reserveCode(eq("one@example.test"), anyString(), eq(3L))).thenReturn(true);
        doThrow(new IllegalStateException("synthetic SMTP failure")).when(sender).send(any(SimpleMailMessage.class));
        var service = new EmailService(provider, repository, new BCryptPasswordEncoder(4));
        assertThatThrownBy(() -> service.sendVerificationCode("one@example.test"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("발송하지 못했습니다");
        verify(repository).invalidateCode(eq("one@example.test"), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingSmtpAndDisabledSmsNeverReportDeliverySuccess() {
        var provider = (ObjectProvider<JavaMailSender>) mock(ObjectProvider.class);
        var repository = mock(EmailVerificationRepository.class);
        var service = new EmailService(provider, repository, new BCryptPasswordEncoder(4));
        assertThatThrownBy(() -> service.sendVerificationCode("one@example.test"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("발송 설정");
        verifyNoInteractions(repository);
        assertThatThrownBy(() -> new SmsService().sendVerificationCode("01000000000"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("현재 제공하지 않습니다");
        assertThatThrownBy(() -> new SmsService().verifyCode("01000000000", "123456"))
                .isInstanceOf(BusinessException.class);
    }
}
