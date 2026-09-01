package com.capsule.insurance.quote.application;

import com.capsule.insurance.catalog.application.port.CancerProductQueryRepository;
import com.capsule.insurance.catalog.domain.Coverage;
import com.capsule.insurance.catalog.domain.ProductVersion;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.quote.application.port.QuoteRepository;
import com.capsule.insurance.quote.domain.InsuranceQuote;
import com.capsule.insurance.quote.domain.QuoteSnapshot;
import com.capsule.insurance.quote.dto.CreateQuoteRequest;
import com.capsule.insurance.quote.dto.QuoteResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteService {

    private static final Duration QUOTE_VALIDITY = Duration.ofMinutes(30);

    private final CancerProductQueryRepository productRepository;
    private final QuoteRepository quoteRepository;
    private final Clock clock;

    public QuoteService(
            CancerProductQueryRepository productRepository,
            QuoteRepository quoteRepository
    ) {
        this(productRepository, quoteRepository, Clock.systemUTC());
    }

    QuoteService(
            CancerProductQueryRepository productRepository,
            QuoteRepository quoteRepository,
            Clock clock
    ) {
        this.productRepository = productRepository;
        this.quoteRepository = quoteRepository;
        this.clock = clock;
    }

    @Transactional
    public QuoteResponse issue(Long userId, CreateQuoteRequest request) {
        ProductVersion product = productRepository.findOnSaleProduct(request.productVersionId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "판매 중인 암보험 상품 버전을 찾을 수 없습니다."
                ));

        List<Coverage> productCoverages = productRepository.findCoverages(product.productVersionId());
        Set<Long> selectedIds = new LinkedHashSet<>(request.selectedProductCoverageIds());
        List<Coverage> selectedCoverages = productCoverages.stream()
                .filter(coverage -> selectedIds.contains(coverage.productCoverageId()))
                .toList();

        if (selectedIds.size() != request.selectedProductCoverageIds().size()
                || selectedCoverages.size() != selectedIds.size()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "선택한 담보가 중복되었거나 상품 버전에 속하지 않습니다."
            );
        }

        QuoteSnapshot snapshot = new QuoteSnapshot(
                product.productVersionId(),
                product.productCode(),
                product.version(),
                product.productName(),
                product.baseMonthlyPremium(),
                product.currencyCode(),
                product.termsDocumentId(),
                product.termsVersion(),
                product.termsSourceHash(),
                selectedCoverages.stream().map(this::toCoverageSnapshot).toList()
        );

        InsuranceQuote quote = quoteRepository.save(
                "Q-" + UUID.randomUUID(),
                userId,
                product.productVersionId(),
                product.baseMonthlyPremium(),
                product.currencyCode(),
                snapshot,
                product.termsSourceHash(),
                Instant.now(clock).plus(QUOTE_VALIDITY)
        );
        return toResponse(quote);
    }

    @Transactional
    public QuoteResponse get(Long userId, Long quoteId) {
        quoteRepository.expireIfNeeded(quoteId, Instant.now(clock));
        return quoteRepository.findOwned(quoteId, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "견적을 찾을 수 없습니다."
                ));
    }

    private QuoteSnapshot.CoverageSnapshot toCoverageSnapshot(Coverage coverage) {
        return new QuoteSnapshot.CoverageSnapshot(
                coverage.productCoverageId(),
                coverage.coverageCode(),
                coverage.coverageName(),
                coverage.insuredAmount(),
                coverage.currencyCode(),
                coverage.waitingPeriodDays(),
                coverage.reductionPeriodDays(),
                coverage.reductionRate(),
                coverage.coverageStartRule(),
                coverage.termsClauseId(),
                coverage.termsClauseCode()
        );
    }

    private QuoteResponse toResponse(InsuranceQuote quote) {
        return new QuoteResponse(
                quote.quoteId(),
                quote.quoteNo(),
                quote.status(),
                quote.productVersionId(),
                quote.monthlyPremium(),
                quote.currencyCode(),
                quote.snapshot(),
                quote.termsDocumentHash(),
                quote.expiresAt(),
                quote.usedAt(),
                quote.createdAt()
        );
    }
}
