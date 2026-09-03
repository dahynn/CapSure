// #Demo Setting
package com.capsule.insurance.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.auth.application.AuthService;
import com.capsule.insurance.auth.application.EmailService;
import com.capsule.insurance.auth.application.SmsService;
import com.capsule.insurance.auth.dto.AuthResult;
import com.capsule.insurance.common.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @Test
    void loginReturnsApiResponse() throws Exception {
        given(authService.login(any()))
                .willReturn(new AuthResult(
                        "stub-user-1",
                        "stub-refresh-user-1",
                        "Bearer",
                        "demo-user",
                        "ROLE_USER"
                ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo@example.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value("demo-user"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }
}
