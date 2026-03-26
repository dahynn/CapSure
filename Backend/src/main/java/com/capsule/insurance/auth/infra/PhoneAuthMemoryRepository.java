package com.capsule.insurance.auth.infra;

import com.capsule.insurance.auth.domain.PhoneAuthRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PhoneAuthMemoryRepository implements PhoneAuthRepository {
    private final Map<String, String> authCodes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> verifiedPhones = new ConcurrentHashMap<>();

    @Override
    public void save(String phone, String authCode) {
        authCodes.put(phone, authCode);
    }

    @Override
    public String findByPhone(String phone) {
        return authCodes.get(phone);
    }

    @Override
    public void deleteByPhone(String phone) {
        authCodes.remove(phone);
    }

    @Override
    public void saveVerifiedPhone(String phone) {
        verifiedPhones.put(phone, true);
    }

    @Override
    public boolean isPhoneVerified(String phone) {
        return verifiedPhones.containsKey(phone);
    }

    @Override
    public void deleteVerifiedPhone(String phone) {
        verifiedPhones.remove(phone);
    }
}
