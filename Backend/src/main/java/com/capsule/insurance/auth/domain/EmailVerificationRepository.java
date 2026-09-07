package com.capsule.insurance.auth.domain;

public interface EmailVerificationRepository {

    boolean reserveCode(String email, String codeHash, long expirationMinutes);
    void invalidateCode(String email, String codeHash);
    boolean verifyCode(String email, String code, long verifiedMinutes);
    boolean isVerified(String email);
    
    boolean consumeVerified(String email);
}
