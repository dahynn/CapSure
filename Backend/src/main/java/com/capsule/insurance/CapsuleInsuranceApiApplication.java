// #Demo Setting
package com.capsule.insurance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class CapsuleInsuranceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapsuleInsuranceApiApplication.class, args);
    }
}
