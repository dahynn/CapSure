package com.capsule.insurance.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidTime = 1000L * 60 * 60; // 1시간
    private final long refreshTokenValidTime = 1000L * 60 * 60 * 24 * 7; // 7일

    public JwtTokenProvider(@Value("${jwt.secret:${JWT_SECRET:}}") String secret) {
        // Local sessions use a process-local random key when no deployment secret is supplied.
        this.secretKey = StringUtils.hasText(secret)
                ? Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))
                : Jwts.SIG.HS512.key().build();
    }

    public String createAccessToken(String userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("tokenType", "ACCESS")
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenValidTime))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String userId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenValidTime))
                .signWith(secretKey)
                .compact();
    }

    public String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        if (!isAccessClaims(claims)) {
            throw new IllegalArgumentException("API 인증에는 Access Token이 필요합니다.");
        }
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        User principal = new User(userId, "", List.of(new SimpleGrantedAuthority(role)));
        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }

    public boolean validateAccessToken(String token) {
        try {
            return isAccessClaims(Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload());
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            String type = claims.get("tokenType", String.class);
            return (type == null || "REFRESH".equals(type))
                    && claims.get("role") == null && StringUtils.hasText(claims.getSubject());
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isAccessClaims(Claims claims) {
        String type = claims.get("tokenType", String.class);
        return (type == null || "ACCESS".equals(type))
                && StringUtils.hasText(claims.get("role", String.class))
                && StringUtils.hasText(claims.getSubject());
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public long getExpirationRemaining(String token) {
        try {
            Date expiration = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return remaining > 0 ? remaining : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
