package com.capsule.insurance.auth;

import com.capsule.insurance.insurer.infra.InsurerCatalogMapper;
import com.capsule.insurance.insurer.infra.projection.ProductSourceSummaryProjection;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
public class ManualDbCheckTest {

    @Autowired
    private InsurerCatalogMapper insurerCatalogMapper;

    @Test
    void checkProductRetrieval() {
        List<ProductSourceSummaryProjection> products =
                insurerCatalogMapper.findProductSourcesByFilter("CANCER", BigDecimal.valueOf(1000000), "M", 1L);
        System.out.println("Fetched products: " + products.size());
        if (!products.isEmpty()) {
            ProductSourceSummaryProjection first = products.get(0);
            System.out.println("First product: " + first.productName());
        }
    }
}
