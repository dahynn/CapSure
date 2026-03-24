package com.capsule.insurance.auth.infra;

import com.capsule.insurance.auth.domain.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RefreshTokenMemoryRepository implements RefreshTokenRepository {

    private final ConcurrentHashMap<String, String> refreshTokenMap = new ConcurrentHashMap<>();

    @Override
    public void save(String userId, String refreshToken) {
        refreshTokenMap.put(userId, refreshToken);
    }

    @Override
    public String findByUserId(String userId) {
        return refreshTokenMap.get(userId);
    }

    @Override
    public void deleteByUserId(String userId) {
        refreshTokenMap.remove(userId);
    }
}
