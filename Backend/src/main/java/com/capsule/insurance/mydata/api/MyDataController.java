// #Demo Setting
package com.capsule.insurance.mydata.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.mydata.application.MyDataService;
import com.capsule.insurance.mydata.dto.InsuranceSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mydata")
public class MyDataController {

    private final MyDataService myDataService;

    public MyDataController(MyDataService myDataService) {
        this.myDataService = myDataService;
    }

    @GetMapping("/insurances")
    public ApiResponse<List<InsuranceSummary>> getInsurances() {
        return ApiResponse.success(myDataService.getInsuranceSummaries());
    }
}
