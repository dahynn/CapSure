package com.capsule.insurance.insurer.infra;

import com.capsule.insurance.insurer.domain.CapsuleProduct;
import com.capsule.insurance.insurer.domain.ReferenceCoverage;
import com.capsule.insurance.insurer.dto.ProductDetailResponse;
import com.capsule.insurance.insurer.dto.ProductSummaryResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InsurerCatalogMapper {

    List<ReferenceCoverage> findActiveCoverages();

    List<CapsuleProduct> findAvailableCapsuleProducts();

    CapsuleProduct findCapsuleProductById(@Param("capsuleProductId") Long capsuleProductId);

    List<ProductSummaryResponse> findProductSourcesByFilter(
            @Param("category") String category,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("gender") String gender
    );

    ProductDetailResponse findProductSourceDetail(
            @Param("productSourceId") Long productSourceId,
            @Param("gender") String gender
    );
}
