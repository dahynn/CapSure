package com.capsule.insurance.quote.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.quote.application.QuoteService;
import com.capsule.insurance.quote.dto.CreateQuoteRequest;
import com.capsule.insurance.quote.dto.QuoteResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public ApiResponse<QuoteResponse> issue(
            @Valid @RequestBody CreateQuoteRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(quoteService.issue(AuthenticatedUser.id(authentication), request));
    }

    @GetMapping("/{quoteId}")
    public ApiResponse<QuoteResponse> get(
            @PathVariable Long quoteId,
            Authentication authentication
    ) {
        return ApiResponse.success(quoteService.get(AuthenticatedUser.id(authentication), quoteId));
    }
}
