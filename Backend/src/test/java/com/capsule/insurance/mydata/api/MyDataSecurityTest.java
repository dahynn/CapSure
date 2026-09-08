package com.capsule.insurance.mydata.api;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import com.capsule.insurance.common.security.SecurityConfig;
import com.capsule.insurance.common.security.jwt.JwtAuthenticationFilter;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import com.capsule.insurance.mydata.application.MyDataProxyService;
import com.capsule.insurance.mydata.dto.MyDataUserInsurancesResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MyDataController.class)
@ActiveProfiles("main")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MyDataSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean MyDataProxyService service;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean TokenBlacklistRepository blacklist;

    @Test void anonymousAndExpiredSessionsReceiveRefreshableUnauthorizedResponse() throws Exception {
        for (String path : List.of("/mydata/insurances", "/mydata/my-insurances", "/mydata/users/1/insurances")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        }
        when(tokens.resolveToken(any())).thenReturn("expired-test-token");
        when(tokens.validateAccessToken("expired-test-token")).thenReturn(false);
        mvc.perform(get("/api/v1/ops/premium-collections/billing/runs")
                        .header("Authorization", "Bearer expired-test-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void allAliasesUseAuthenticatedUserAndRejectOtherUsers() throws Exception {
        when(service.getUserInsurances(42L)).thenReturn(new MyDataUserInsurancesResponse(42L, List.of()));
        for (String path : List.of("/mydata/insurances", "/mydata/my-insurances", "/mydata/users/42/insurances")) {
            mvc.perform(get(path).with(user("42").roles("USER")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(42));
        }
        mvc.perform(get("/mydata/users/1/insurances").with(user("42").roles("USER")))
                .andExpect(status().isForbidden());
        verify(service, times(3)).getUserInsurances(42L);
        verifyNoMoreInteractions(service);
    }
}
