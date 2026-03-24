package com.capsule.insurance.auth.domain;

public interface RefreshTokenRepository {
    void save(String userId, String refreshToken);
    String findByUserId(String userId);
    void deleteByUserId(String userId);
}
