package com.capsule.insurance.insurer.infra;

import com.capsule.insurance.insurer.domain.CapsuleProduct;
import com.capsule.insurance.insurer.domain.ReferenceCoverage;
import com.capsule.insurance.insurer.infra.projection.ProductSourceDetailProjection;
import com.capsule.insurance.insurer.infra.projection.PopularProductProjection;
import com.capsule.insurance.insurer.infra.projection.ProductSourceSummaryProjection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InsurerCatalogMapper {

    List<ReferenceCoverage> findActiveCoverages();

    List<CapsuleProduct> findAvailableCapsuleProducts();

    CapsuleProduct findCapsuleProductById(@Param("productSourceId") Long productSourceId);

    void upsertCapsuleProductFromProductSource(@Param("productSourceId") Long productSourceId);

    List<ProductSourceSummaryProjection> findProductSourcesByFilter(
            @Param("category") String category,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("gender") String gender,
            @Param("userId") Long userId
    );

    ProductSourceDetailProjection findProductSourceDetail(
            @Param("productSourceId") Long productSourceId,
            @Param("gender") String gender
    );

    List<PopularProductProjection> findPopularProductsByCategories(
            @Param("categoryCodes") List<String> categoryCodes,
            @Param("gender") String gender,
            @Param("userId") Long userId
    );
}
