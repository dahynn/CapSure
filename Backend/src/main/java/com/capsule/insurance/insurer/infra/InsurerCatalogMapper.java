// #Demo Setting
package com.capsule.insurance.insurer.infra;

import com.capsule.insurance.insurer.domain.CapsuleProduct;
import com.capsule.insurance.insurer.domain.ReferenceCoverage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InsurerCatalogMapper {

    List<ReferenceCoverage> findActiveCoverages();

    List<CapsuleProduct> findAvailableCapsuleProducts();

    CapsuleProduct findCapsuleProductById(@Param("capsuleProductId") Long capsuleProductId);
}
