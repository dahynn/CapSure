package com.capsule.insurance.auth.domain;

public interface EmailVerificationRepository {

    void saveAuthCode(String email, String authCode, long expirationMinutes);
    
    String getAuthCode(String email);
    
    void removeAuthCode(String email);
    
    void markAsVerified(String email, long expirationMinutes);
    
    boolean isVerified(String email);
    
    void removeVerified(String email);
}
