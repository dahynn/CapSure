package com.capsule.insurance.auth.domain;

public interface RefreshTokenRepository {
    void save(String userId, String refreshToken);
    boolean replaceIfMatches(String userId, String previousToken, String nextToken);
    void deleteByUserId(String userId);
}
