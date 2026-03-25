package com.capsule.insurance.mydata.api;

import com.capsule.insurance.mydata.application.MyDataService;
import com.capsule.insurance.mydata.dto.MyDataUserInsurancesResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("mock")
@RestController
@RequestMapping("/mock/mydata")
public class MyDataMockController {

    private final MyDataService myDataService;

    public MyDataMockController(MyDataService myDataService) {
        this.myDataService = myDataService;
    }

    @GetMapping("/users/{userId}/insurances")
    public MyDataUserInsurancesResponse getUserInsurances(@PathVariable Long userId) {
        return myDataService.getUserInsurances(userId);
    }
}
