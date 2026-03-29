package com.capsule.insurance.insurer.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.auth.domain.Gender;
import com.capsule.insurance.auth.domain.UserAccount;
import com.capsule.insurance.auth.infra.UserAccountMapper;
import com.capsule.insurance.insurer.domain.CoverageCategory;
import com.capsule.insurance.insurer.domain.InsurerSector;
import com.capsule.insurance.insurer.infra.projection.ProductSourceDetailProjection;
import com.capsule.insurance.insurer.infra.InsurerCatalogMapper;
import com.capsule.insurance.insurer.infra.ProductSourceMapper;
import com.capsule.insurance.insurer.infra.projection.ProductSourceSummaryProjection;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class InsurerServiceTest {

    @Mock
    private InsurerCatalogMapper insurerCatalogMapper;

    @Mock
    private ProductSourceMapper productSourceMapper;

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    @SuppressWarnings("rawtypes")
    private ObjectProvider chatClientBuilderProvider;

    private InsurerService insurerService;

    private final String category = "CANCER";
    private final Integer budget = 20000;

    @BeforeEach
    void setUp() {
        insurerService = new InsurerService(productSourceMapper, insurerCatalogMapper, userAccountMapper, chatClientBuilderProvider, "");
    }

    @Test
    @DisplayName("남성 유저라면 Male 가격이 포함된 리스트가 반환되어야 한다 (자동 조회)")
    void testMalePriceSelection() {
        // given
        Long userId = 1L;
        UserAccount mockUser = UserAccount.builder()
                .userId(userId)
                .gender(Gender.M)
                .build();

        ProductSourceSummaryProjection mockDto = new ProductSourceSummaryProjection(
                1L,
                "Capsule Insurer",
                "Mock Product",
                InsurerSector.LIFE,
                CoverageCategory.CANCER.name(),
                "COV001",
                BigDecimal.valueOf(15000),
                null,
                "/terms/mock-product.pdf",
                null,
                null
        );

        BigDecimal expectedMaxPrice = BigDecimal.valueOf(20000);

        when(userAccountMapper.findByUserId(userId)).thenReturn(mockUser);
        when(insurerCatalogMapper.countProductSourcesByFilter(eq(category), eq(expectedMaxPrice), eq("M"), eq(userId)))
                .thenReturn(1L);
        when(insurerCatalogMapper.findProductSourcesByFilterPaged(eq(category), eq(expectedMaxPrice), eq("M"), eq(userId), eq("price_desc"), eq(12), eq(0)))
                .thenReturn(List.of(mockDto));

        // when
        var result = insurerService.getProducts(category, budget, "popular", 0, 12, userId);

        // then
        verify(insurerCatalogMapper).findProductSourcesByFilterPaged(category, expectedMaxPrice, "M", userId, "price_desc", 12, 0);
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.items().get(0).productName()).isEqualTo("Mock Product");
        assertThat(result.items().get(0).monthlyPrice()).isEqualTo(BigDecimal.valueOf(15000));
        assertThat(result.items().get(0).termsUri()).isEqualTo("/terms/mock-product.pdf");
    }

    @Test
    @DisplayName("여성 유저라면 Female 가격이 반환되어야 한다 (자동 조회)")
    void testFemalePriceSelection() {
        // given
        Long userId = 2L;
        UserAccount mockUser = UserAccount.builder()
                .userId(userId)
                .gender(Gender.F)
                .build();

        ProductSourceSummaryProjection mockDto = new ProductSourceSummaryProjection(
                2L,
                "Capsule Insurer",
                "Female Product",
                InsurerSector.NONLIFE,
                CoverageCategory.CANCER.name(),
                "COV002",
                BigDecimal.valueOf(12000),
                null,
                "/terms/female-product.pdf",
                null,
                null
        );

        when(userAccountMapper.findByUserId(userId)).thenReturn(mockUser);
        when(insurerCatalogMapper.countProductSourcesByFilter(eq(null), eq(null), eq("F"), eq(userId)))
                .thenReturn(1L);
        when(insurerCatalogMapper.findProductSourcesByFilterPaged(eq(null), eq(null), eq("F"), eq(userId), eq("price_desc"), eq(12), eq(0)))
                .thenReturn(List.of(mockDto));

        // when
        var result = insurerService.getProducts(null, null, null, 0, 12, userId);

        // then
        verify(insurerCatalogMapper).findProductSourcesByFilterPaged(null, null, "F", userId, "price_desc", 12, 0);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productName()).isEqualTo("Female Product");
        assertThat(result.items().get(0).monthlyPrice()).isEqualTo(BigDecimal.valueOf(12000));
        assertThat(result.items().get(0).termsUri()).isEqualTo("/terms/female-product.pdf");
    }

    @Test
    @DisplayName("유저 정보가 없어도 기본 성별(M)로 조회되어야 한다")
    void testExcludeZeroPriceProduct() {
        // given
        Long userId = 3L;
        when(userAccountMapper.findByUserId(userId)).thenReturn(null);
        when(insurerCatalogMapper.countProductSourcesByFilter(eq(null), eq(null), eq("M"), eq(userId)))
                .thenReturn(0L);
        when(insurerCatalogMapper.findProductSourcesByFilterPaged(eq(null), eq(null), eq("M"), eq(userId), eq("price_desc"), eq(12), eq(0)))
                .thenReturn(List.of());

        // when
        var result = insurerService.getProducts(null, null, null, 0, 12, userId);

        // then
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("상품 상세 조회는 projection을 API DTO로 변환해야 한다")
    void getProductDetailMapsProjectionToResponse() {
        Long userId = 10L;
        Long productSourceId = 99L;

        when(userAccountMapper.findByUserId(userId)).thenReturn(UserAccount.builder()
                .userId(userId)
                .gender(Gender.F)
                .build());
        when(insurerCatalogMapper.findProductSourceDetail(productSourceId, "F"))
                .thenReturn(new ProductSourceDetailProjection(
                        productSourceId,
                        "Capsule Insurer",
                        "Detail Product",
                        InsurerSector.LIFE,
                        "ONLINE",
                        "암 보장",
                        "암 진단 시",
                        "1000만원",
                        "1구좌",
                        "10000원",
                        "월납",
                        "20년납",
                        "90세만기",
                        CoverageCategory.CANCER.name(),
                        "COV010",
                        BigDecimal.valueOf(12345),
                        "요약",
                        "특징",
                        "유의사항",
                        "1588-0000",
                        null,
                        "연 2.0%",
                        "연 1.5%",
                        "연 1.0%"
                ));

        var detail = insurerService.getProductDetail(productSourceId, userId);

        verify(insurerCatalogMapper).findProductSourceDetail(productSourceId, "F");
        assertThat(detail.productSourceId()).isEqualTo(productSourceId);
        assertThat(detail.productName()).isEqualTo("Detail Product");
        assertThat(detail.monthlyPrice()).isEqualByComparingTo("12345");
    }

    @Test
    @DisplayName("상품 상세 조회 대상이 없으면 예외를 던져야 한다")
    void getProductDetailThrowsWhenMissing() {
        when(userAccountMapper.findByUserId(1L)).thenReturn(null);
        when(insurerCatalogMapper.findProductSourceDetail(404L, "M")).thenReturn(null);

        assertThatThrownBy(() -> insurerService.getProductDetail(404L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("product_source");
    }
}
