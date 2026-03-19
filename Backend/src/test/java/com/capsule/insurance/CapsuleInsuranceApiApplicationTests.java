// #Demo Setting
package com.capsule.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@ActiveProfiles("test")
class CapsuleInsuranceApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
