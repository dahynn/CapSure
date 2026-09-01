package com.capsule.insurance.catalog.application;

import com.capsule.insurance.catalog.application.port.CancerProductQueryRepository;
import com.capsule.insurance.catalog.domain.Coverage;
import com.capsule.insurance.catalog.domain.ProductVersion;
import com.capsule.insurance.catalog.domain.TermsClause;
import com.capsule.insurance.catalog.domain.TermsDocument;
import com.capsule.insurance.catalog.dto.CancerProductDetailResponse;
import com.capsule.insurance.catalog.dto.CancerProductSummaryResponse;
import com.capsule.insurance.catalog.dto.CancerProductTermsSummaryResponse;
import com.capsule.insurance.catalog.dto.TermsClauseResponse;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CancerProductService {

    private static final String SIMULATION_DISCLAIMER =
            "학습·포트폴리오용 합성 암보험 안내입니다. 실제 보험계약, 보험료, 의학적 판단 또는 보험금 지급 기준이 아닙니다.";

    private static final Map<String, String> HIGHLIGHT_CATEGORIES = Map.ofEntries(
            Map.entry("ARTICLE-04", "APPLICATION"),
            Map.entry("ARTICLE-05", "DISCLOSURE"),
            Map.entry("ARTICLE-06", "POLICY_ACTIVATION"),
            Map.entry("ARTICLE-07", "COVERAGE_START"),
            Map.entry("ARTICLE-08", "COVERAGE_START"),
            Map.entry("ARTICLE-12", "REDUCTION"),
            Map.entry("ARTICLE-13", "CLAIM_EVIDENCE"),
            Map.entry("ARTICLE-14", "EXCLUSION"),
            Map.entry("ARTICLE-15", "CLAIM_DECISION")
    );

    private final CancerProductQueryRepository repository;

    public CancerProductService(CancerProductQueryRepository repository) {
        this.repository = repository;
    }

    public List<CancerProductSummaryResponse> getOnSaleProducts() {
        return repository.findOnSaleProducts().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public CancerProductDetailResponse getProduct(Long productVersionId) {
        ProductVersion product = getOnSaleProductOrThrow(productVersionId);
        List<CancerProductDetailResponse.CoverageResponse> coverages = repository
                .findCoverages(productVersionId)
                .stream()
                .map(this::toCoverageResponse)
                .toList();

        return new CancerProductDetailResponse(toSummaryResponse(product), coverages);
    }

    public CancerProductTermsSummaryResponse getTermsSummary(Long productVersionId) {
        ProductVersion product = getOnSaleProductOrThrow(productVersionId);
        TermsDocument document = repository.findTermsDocument(productVersionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "상품 버전에 연결된 약관 문서를 찾을 수 없습니다."));

        List<CancerProductTermsSummaryResponse.CoverageConditionResponse> coverageConditions = repository
                .findCoverages(productVersionId)
                .stream()
                .map(this::toCoverageConditionResponse)
                .toList();

        List<CancerProductTermsSummaryResponse.TermsHighlightResponse> highlights = repository
                .findTermsClauses(document.termsDocumentId())
                .stream()
                .filter(clause -> HIGHLIGHT_CATEGORIES.containsKey(clause.clauseCode()))
                .map(this::toHighlightResponse)
                .toList();

        return new CancerProductTermsSummaryResponse(
                product.productVersionId(),
                product.productCode(),
                product.version(),
                product.productName(),
                product.simulation(),
                toTermsDocumentResponse(document),
                coverageConditions,
                highlights,
                SIMULATION_DISCLAIMER
        );
    }

    public TermsClauseResponse getTermsClause(Long termsClauseId) {
        TermsClause clause = repository.findTermsClause(termsClauseId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "약관 조항을 찾을 수 없습니다."));

        return new TermsClauseResponse(
                clause.termsClauseId(),
                clause.termsDocumentId(),
                clause.documentCode(),
                clause.documentVersion(),
                clause.documentHash(),
                clause.simulation(),
                clause.clauseCode(),
                clause.title(),
                clause.body(),
                clause.pageNo(),
                clause.sortOrder()
        );
    }

    private ProductVersion getOnSaleProductOrThrow(Long productVersionId) {
        return repository.findOnSaleProduct(productVersionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "판매 중인 암보험 상품 버전을 찾을 수 없습니다."));
    }

    private CancerProductSummaryResponse toSummaryResponse(ProductVersion product) {
        return new CancerProductSummaryResponse(
                product.productVersionId(),
                product.productCode(),
                product.version(),
                product.productName(),
                product.insurerName(),
                product.insurerSector(),
                product.saleFrom(),
                product.saleTo(),
                product.status(),
                product.baseMonthlyPremium(),
                product.currencyCode(),
                product.simulation(),
                product.coverageCount(),
                product.termsDocumentId(),
                product.termsVersion(),
                product.termsSourceHash()
        );
    }

    private CancerProductDetailResponse.CoverageResponse toCoverageResponse(Coverage coverage) {
        return new CancerProductDetailResponse.CoverageResponse(
                coverage.productCoverageId(),
                coverage.coverageCode(),
                coverage.coverageName(),
                coverage.coverageCategory(),
                coverage.benefitType(),
                coverage.description(),
                coverage.insuredAmount(),
                coverage.currencyCode(),
                coverage.waitingPeriodDays(),
                coverage.reductionPeriodDays(),
                coverage.reductionRate(),
                coverage.coverageStartRule(),
                coverage.displayOrder(),
                coverage.termsClauseId(),
                coverage.termsClauseCode()
        );
    }

    private CancerProductTermsSummaryResponse.CoverageConditionResponse toCoverageConditionResponse(
            Coverage coverage
    ) {
        return new CancerProductTermsSummaryResponse.CoverageConditionResponse(
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

    private CancerProductTermsSummaryResponse.TermsDocumentResponse toTermsDocumentResponse(
            TermsDocument document
    ) {
        return new CancerProductTermsSummaryResponse.TermsDocumentResponse(
                document.termsDocumentId(),
                document.documentCode(),
                document.documentVersion(),
                document.title(),
                document.sourceType(),
                document.sourceUri(),
                document.sourceHash(),
                document.effectiveFrom(),
                document.effectiveTo(),
                document.status(),
                document.simulation()
        );
    }

    private CancerProductTermsSummaryResponse.TermsHighlightResponse toHighlightResponse(TermsClause clause) {
        return new CancerProductTermsSummaryResponse.TermsHighlightResponse(
                HIGHLIGHT_CATEGORIES.get(clause.clauseCode()),
                clause.termsClauseId(),
                clause.clauseCode(),
                clause.title(),
                clause.body()
        );
    }
}
