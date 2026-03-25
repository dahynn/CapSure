package com.capsule.insurance.mydata.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.mydata.application.MyDataProxyService;
import com.capsule.insurance.mydata.dto.MyDataUserInsurancesResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("main")
@RestController
@RequestMapping("/mydata")
public class MyDataController {

    private static final long DEFAULT_USER_ID = 1L;

    private final MyDataProxyService myDataProxyService;

    public MyDataController(MyDataProxyService myDataProxyService) {
        this.myDataProxyService = myDataProxyService;
    }

    @GetMapping("/insurances")
    public ApiResponse<MyDataUserInsurancesResponse> getInsurances() {
        return ApiResponse.success(myDataProxyService.getUserInsurances(DEFAULT_USER_ID));
    }

    @GetMapping("/users/{userId}/insurances")
    public ApiResponse<MyDataUserInsurancesResponse> getUserInsurances(@PathVariable Long userId) {
        return ApiResponse.success(myDataProxyService.getUserInsurances(userId));
    }

    @GetMapping("/my-insurances")
    public ApiResponse<MyDataUserInsurancesResponse> getMyInsurances() {
        return ApiResponse.success(myDataProxyService.getUserInsurances(DEFAULT_USER_ID));
    }
}
