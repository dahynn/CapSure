// #Demo Setting
package com.capsule.insurance.insurer.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.insurer.application.InsurerService;
import com.capsule.insurance.insurer.dto.InsurerProductSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/insurers")
public class InsurerController {

    private final InsurerService insurerService;

    public InsurerController(InsurerService insurerService) {
        this.insurerService = insurerService;
    }

    @GetMapping("/products")
    public ApiResponse<List<InsurerProductSummary>> getProducts() {
        return ApiResponse.success(insurerService.getProducts());
    }
}
