package com.capsule.insurance.insurer.infra;

import com.capsule.insurance.insurer.domain.ProductSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductSourceMapper {

    ProductSource findById(@Param("productSourceId") Long productSourceId);
}
