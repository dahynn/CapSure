// #Demo Setting
package com.capsule.insurance.common.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTokenProvider {

    public String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        // TODO: 실제 JWT 검증 로직 보강 필요
        return StringUtils.hasText(token) && token.startsWith("stub-");
    }

    public Authentication getAuthentication(String token) {
        String userId = token.replaceFirst("^stub-", "");
        User principal = new User(userId, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
    }
}
