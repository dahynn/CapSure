package com.capsule.insurance.mydata.api;

import com.capsule.insurance.common.response.ApiResponse;
import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.common.security.AuthenticatedUser;
import com.capsule.insurance.mydata.application.MyDataProxyService;
import com.capsule.insurance.mydata.dto.MyDataUserInsurancesResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("main")
@RestController
@RequestMapping("/mydata")
public class MyDataController {

    private final MyDataProxyService myDataProxyService;

    public MyDataController(MyDataProxyService myDataProxyService) {
        this.myDataProxyService = myDataProxyService;
    }

    @GetMapping("/insurances")
    public ApiResponse<MyDataUserInsurancesResponse> getInsurances(Authentication authentication) {
        return ApiResponse.success(myDataProxyService.getUserInsurances(AuthenticatedUser.id(authentication)));
    }

    @GetMapping("/users/{userId}/insurances")
    public ApiResponse<MyDataUserInsurancesResponse> getUserInsurances(
            @PathVariable Long userId, Authentication authentication
    ) {
        Long authenticatedId = AuthenticatedUser.id(authentication);
        if (!authenticatedId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 보험 정보만 조회할 수 있습니다.");
        }
        return ApiResponse.success(myDataProxyService.getUserInsurances(authenticatedId));
    }

    @GetMapping("/my-insurances")
    public ApiResponse<MyDataUserInsurancesResponse> getMyInsurances(Authentication authentication) {
        return getInsurances(authentication);
    }
}
