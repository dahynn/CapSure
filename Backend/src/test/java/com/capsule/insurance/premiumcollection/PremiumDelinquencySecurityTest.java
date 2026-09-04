package com.capsule.insurance.premiumcollection;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import com.capsule.insurance.common.security.SecurityConfig;
import com.capsule.insurance.common.security.jwt.JwtAuthenticationFilter;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import com.capsule.insurance.premiumcollection.api.PremiumDelinquencyController;
import com.capsule.insurance.premiumcollection.application.PremiumDelinquencyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PremiumDelinquencyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PremiumDelinquencySecurityTest {
    static final String URL = "/api/v1/ops/premium-collections/delinquency/runs";
    @Autowired MockMvc mvc;
    @MockitoBean PremiumDelinquencyService service;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean TokenBlacklistRepository blacklist;

    @Test void anonymousAndCustomerCannotReadOrRunOrResume() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isForbidden());
        mvc.perform(get(URL).with(user("1").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL).with(user("1").roles("USER")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL + "/1/resume").with(user("1").roles("USER")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void adminCanReadRunAndResumeWithAuditedActor() throws Exception {
        when(service.recent()).thenReturn(List.of());
        mvc.perform(get(URL).with(user("42").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post(URL).with(user("42").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceKey\":\"manual-1\",\"reason\":\"일일 점검\"}"))
                .andExpect(status().isOk());
        verify(service).run("manual-1", 42L, "일일 점검");
        mvc.perform(post(URL + "/1/resume").with(user("42").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"실패 원인 조치 후 재개\"}"))
                .andExpect(status().isOk());
        verify(service).resume(1L, 42L, "실패 원인 조치 후 재개");
    }

    @Test void missingReasonAndKeyAreRejected() throws Exception {
        mvc.perform(post(URL).with(user("42").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(URL + "/1/resume").with(user("42").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
