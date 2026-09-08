package com.capsule.insurance.mydata.api;

import com.capsule.insurance.mydata.application.MyDataService;
import com.capsule.insurance.mydata.dto.MyDataContractResponse;
import com.capsule.insurance.mydata.dto.MyDataInsuranceContractsResponse;
import com.capsule.insurance.mydata.dto.MyDataInsuranceCoveragesResponse;
import com.capsule.insurance.mydata.dto.MyDataInsuranceListResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("mock")
@RestController
@RequestMapping("/mock/v2/insu")
public class MyDataMockController {

    private final MyDataService myDataService;

    public MyDataMockController(MyDataService myDataService) {
        this.myDataService = myDataService;
    }

    @GetMapping("/insurance")
    public MyDataInsuranceListResponse getInsuranceList(@RequestParam(defaultValue = "1") Long userId) {
        return myDataService.getInsuranceList(userId);
    }

    @GetMapping("/insurances/{insuNum}/property")
    public MyDataContractResponse getInsuranceProperty(@PathVariable("insuNum") String insuNum,
            @RequestParam(defaultValue = "1") Long userId) {
        return myDataService.getInsuranceProperty(userId, insuNum);
    }

    @GetMapping("/insurances/basic")
    public MyDataContractResponse getInsuranceBasic(@RequestParam("insuNum") String insuNum,
            @RequestParam(defaultValue = "1") Long userId) {
        return myDataService.getInsuranceBasic(userId, insuNum);
    }

    @GetMapping("/insurances/contracts")
    public MyDataInsuranceContractsResponse getInsuranceContracts(@RequestParam("insuNum") String insuNum,
            @RequestParam(defaultValue = "1") Long userId) {
        return myDataService.getInsuranceContracts(userId, insuNum);
    }

    @GetMapping("/insurances/coverages")
    public MyDataInsuranceCoveragesResponse getInsuranceCoverages(@RequestParam("insuNum") String insuNum,
            @RequestParam(defaultValue = "1") Long userId) {
        return myDataService.getInsuranceCoverages(userId, insuNum);
    }
}
