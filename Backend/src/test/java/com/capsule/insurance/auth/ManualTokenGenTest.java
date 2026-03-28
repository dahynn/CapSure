package com.capsule.insurance.auth;

import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ManualTokenGenTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    public void generateToken() {
        // userId: 1, email: demo@example.com, role: ROLE_USER
        String token = jwtTokenProvider.createAccessToken("1", "demo@example.com", "ROLE_USER");
        System.out.println("TOKEN_START:" + token + ":TOKEN_END");
    }
}
