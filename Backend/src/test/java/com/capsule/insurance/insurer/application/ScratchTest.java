package com.capsule.insurance.insurer.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.capsule.insurance.insurer.infra.ProductSourceMapper;
import com.capsule.insurance.insurer.domain.ProductSource;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties={"spring.ai.openai.api-key=dummy"})
public class ScratchTest {

    @Autowired
    private com.capsule.insurance.insurer.infra.InsurerCatalogMapper catalogMapper;

    @Test
    public void testFindProductSources() {
        System.out.println("FOUND MAPPER1: catalogMapper!");
        
        java.util.List<com.capsule.insurance.insurer.infra.projection.ProductSourceSummaryProjection> list =
                catalogMapper.findProductSourcesByFilterPaged(null, null, "M", 1L, 20, 0);
        System.out.println("FOUND CATALOG: " + list.size());
    }
}
