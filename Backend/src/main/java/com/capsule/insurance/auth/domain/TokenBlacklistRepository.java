package com.capsule.insurance.auth.domain;

public interface TokenBlacklistRepository {
    void save(String token, long expireTimeMillis);
    boolean isBlacklisted(String token);
}
