package com.capsule.insurance.auth.infra;

import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TokenBlacklistMemoryRepository implements TokenBlacklistRepository {
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void save(String token, long expireTimeMillis) {
        blacklist.put(token, System.currentTimeMillis() + expireTimeMillis);
    }

    @Override
    public boolean isBlacklisted(String token) {
        Long expiration = blacklist.get(token);
        if (expiration == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiration) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
