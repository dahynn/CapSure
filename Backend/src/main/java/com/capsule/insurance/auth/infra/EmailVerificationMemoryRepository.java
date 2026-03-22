package com.capsule.insurance.auth.infra;

import com.capsule.insurance.auth.domain.EmailVerificationRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EmailVerificationMemoryRepository implements EmailVerificationRepository {

    // email -> authCode
    private final Map<String, String> authCodeMap = new ConcurrentHashMap<>();
    
    // email -> expirationTime (ms)
    private final Map<String, Long> authCodeExpirationMap = new ConcurrentHashMap<>();
    
    // email -> verified expirationTime (ms)
    private final Map<String, Long> verifiedMap = new ConcurrentHashMap<>();

    @Override
    public void saveAuthCode(String email, String authCode, long expirationMinutes) {
        authCodeMap.put(email, authCode);
        authCodeExpirationMap.put(email, System.currentTimeMillis() + expirationMinutes * 60 * 1000);
    }

    @Override
    public String getAuthCode(String email) {
        Long expirationTime = authCodeExpirationMap.get(email);
        if (expirationTime != null && System.currentTimeMillis() > expirationTime) {
            removeAuthCode(email);
            return null;
        }
        return authCodeMap.get(email);
    }

    @Override
    public void removeAuthCode(String email) {
        authCodeMap.remove(email);
        authCodeExpirationMap.remove(email);
    }

    @Override
    public void markAsVerified(String email, long expirationMinutes) {
        verifiedMap.put(email, System.currentTimeMillis() + expirationMinutes * 60 * 1000);
        removeAuthCode(email); // 인증이 성공했으니 코드는 제거
    }

    @Override
    public boolean isVerified(String email) {
        Long expirationTime = verifiedMap.get(email);
        if (expirationTime != null && System.currentTimeMillis() > expirationTime) {
            removeVerified(email);
            return false;
        }
        return expirationTime != null;
    }

    @Override
    public void removeVerified(String email) {
        verifiedMap.remove(email);
    }
}
