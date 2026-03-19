// #Demo Setting
package com.capsule.insurance.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI capsuleInsuranceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Capsule Insurance API")
                .version("v1")
                .description("Single Spring Boot modular monolith skeleton"));
    }
}
