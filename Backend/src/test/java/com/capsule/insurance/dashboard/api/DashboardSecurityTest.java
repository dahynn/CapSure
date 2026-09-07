package com.capsule.insurance.dashboard.api;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import com.capsule.insurance.common.security.SecurityConfig;
import com.capsule.insurance.common.security.jwt.JwtAuthenticationFilter;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import com.capsule.insurance.dashboard.application.DashboardService;
import com.capsule.insurance.dashboard.dto.DashboardSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean DashboardService service;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean TokenBlacklistRepository blacklist;

    @Test void anonymousRequestsNeverFallBackToUserOne() throws Exception {
        mvc.perform(get("/dashboard/home")).andExpect(status().isUnauthorized());
        mvc.perform(get("/dashboard/summary")).andExpect(status().isUnauthorized());
        mvc.perform(post("/dashboard/audits/read")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void summaryAndReadCursorUseAuthenticatedIdentity() throws Exception {
        when(service.getSummary(42L)).thenReturn(new DashboardSummary(3, 1, 2));
        when(service.markAuditsRead(42L)).thenReturn(new DashboardSummary(3, 1, 0));
        mvc.perform(get("/dashboard/summary").with(user("42"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadAudits").value(2));
        mvc.perform(post("/dashboard/audits/read").with(user("42"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadAudits").value(0));
        mvc.perform(get("/dashboard/summary").with(user("invalid"))).andExpect(status().isUnauthorized());
        verify(service).getSummary(42L);
        verify(service).markAuditsRead(42L);
        verifyNoMoreInteractions(service);
    }
}
