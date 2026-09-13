package com.capsule.insurance.operations.catalog.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import com.capsule.insurance.common.security.SecurityConfig;
import com.capsule.insurance.common.security.jwt.JwtAuthenticationFilter;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import com.capsule.insurance.operations.catalog.application.ProductVersionApprovalService;
import com.capsule.insurance.operations.catalog.dto.ProductVersionApprovalDecision;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductVersionApprovalOperationsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductVersionApprovalSecurityTest {

    private static final String URL =
            "/api/v1/ops/catalog/product-versions/7/approval-decisions";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductVersionApprovalService service;

    @MockitoBean
    JwtTokenProvider tokens;

    @MockitoBean
    TokenBlacklistRepository blacklist;

    @Test
    void anonymousAndCustomerCannotDecideOrReadApprovalHistory() throws Exception {
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"reason\":\"검토 완료\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(URL)
                        .with(user("42").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"reason\":\"검토 완료\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL).with(user("42").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void adminDecisionUsesAuthenticatedActorAndValidatesReason() throws Exception {
        mvc.perform(post(URL)
                        .with(user("99").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"reason\":\"상품·약관 검토 완료\"}"))
                .andExpect(status().isOk());
        verify(service).decide(
                7L,
                ProductVersionApprovalDecision.APPROVE,
                99L,
                "상품·약관 검토 완료"
        );

        mvc.perform(post(URL)
                        .with(user("99").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\",\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanReadApprovalHistory() throws Exception {
        org.mockito.Mockito.when(service.getHistory(7L)).thenReturn(List.of());
        mvc.perform(get(URL).with(user("99").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(service).getHistory(7L);
    }
}

