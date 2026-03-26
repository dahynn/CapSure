package com.capsule.insurance.auth.domain;

public interface PhoneAuthRepository {
    void save(String phone, String authCode);
    String findByPhone(String phone);
    void deleteByPhone(String phone);
    void saveVerifiedPhone(String phone);
    boolean isPhoneVerified(String phone);
    void deleteVerifiedPhone(String phone);
}
