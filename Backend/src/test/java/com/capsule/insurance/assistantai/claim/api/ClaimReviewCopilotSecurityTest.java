package com.capsule.insurance.assistantai.claim.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.assistantai.claim.application.ClaimAssessmentAssistantService;
import com.capsule.insurance.assistantai.claim.application.ClaimCopilotReviewService;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReview;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEvent;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewEventType;
import com.capsule.insurance.assistantai.claim.domain.ClaimCopilotReviewStatus;
import com.capsule.insurance.auth.domain.TokenBlacklistRepository;
import com.capsule.insurance.common.security.SecurityConfig;
import com.capsule.insurance.common.security.jwt.JwtAuthenticationFilter;
import com.capsule.insurance.common.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClaimReviewCopilotController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ClaimReviewCopilotSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClaimAssessmentAssistantService assessmentService;

    @MockitoBean
    private ClaimCopilotReviewService reviewService;

    @MockitoBean
    private JwtTokenProvider tokens;

    @MockitoBean
    private TokenBlacklistRepository blacklist;

    @Test
    void anonymousAndCustomerCannotCreateOrReviewCopilotDrafts() throws Exception {
        String draftPath = "/api/v1/ops/claims/100/review-copilot/drafts";
        String reviewPath = draftPath + "/request-1/review";
        String historyPath = reviewPath + "/history";
        String queuePath = "/api/v1/ops/claims/review-copilot/reviews";

        mvc.perform(post(draftPath).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"request-1\",\"instruction\":\"약관 확인\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(draftPath).with(user("7").roles("USER")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"request-1\",\"instruction\":\"약관 확인\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(reviewPath).with(user("7").roles("USER")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(reviewPath).with(user("7").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get(historyPath).with(user("7").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get(queuePath).with(user("7").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(assessmentService, reviewService);
    }

    @Test
    void adminReviewUsesTheAuthenticatedReviewerId() throws Exception {
        when(reviewService.review(100L, "request-1", ClaimCopilotReviewStatus.CONFIRMED, 99L))
                .thenReturn(new ClaimCopilotReview(
                        100L, "request-1", ClaimCopilotReviewStatus.CONFIRMED, 99L, Instant.parse("2026-09-10T00:00:00Z")));

        mvc.perform(post("/api/v1/ops/claims/100/review-copilot/drafts/request-1/review")
                        .with(user("99").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());

        verify(reviewService).review(100L, "request-1", ClaimCopilotReviewStatus.CONFIRMED, 99L);
    }

    @Test
    void adminCanReadTheCurrentReviewStatus() throws Exception {
        when(reviewService.get(100L, "request-1"))
                .thenReturn(new ClaimCopilotReview(
                        100L, "request-1", ClaimCopilotReviewStatus.DRAFT, null, Instant.parse("2026-09-10T00:00:00Z")));

        mvc.perform(get("/api/v1/ops/claims/100/review-copilot/drafts/request-1/review")
                        .with(user("99").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(reviewService).get(100L, "request-1");
    }

    @Test
    void adminCanReadReviewHistoryWithoutCustomerOrPromptContent() throws Exception {
        when(reviewService.history(100L, "request-1"))
                .thenReturn(List.of(new ClaimCopilotReviewEvent(
                        100L,
                        "request-1",
                        ClaimCopilotReviewEventType.DRAFT_CREATED,
                        ClaimCopilotReviewStatus.DRAFT,
                        null,
                        Instant.parse("2026-09-10T00:00:00Z")
                )));

        mvc.perform(get("/api/v1/ops/claims/100/review-copilot/drafts/request-1/review/history")
                        .with(user("99").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(reviewService).history(100L, "request-1");
    }

    @Test
    void adminCanReadOnlyDraftReviewsFromTheQueue() throws Exception {
        when(reviewService.recent(ClaimCopilotReviewStatus.DRAFT, 20))
                .thenReturn(List.of(new ClaimCopilotReview(
                        100L, "request-1", ClaimCopilotReviewStatus.DRAFT, null, Instant.parse("2026-09-10T00:00:00Z"))));

        mvc.perform(get("/api/v1/ops/claims/review-copilot/reviews")
                        .param("status", "DRAFT")
                        .with(user("99").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(reviewService).recent(ClaimCopilotReviewStatus.DRAFT, 20);
    }
}
