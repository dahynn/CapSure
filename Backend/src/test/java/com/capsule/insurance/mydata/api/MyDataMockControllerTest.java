package com.capsule.insurance.mydata.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.mydata.application.MyDataService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MyDataMockControllerTest {
    @Test void syntheticProviderUsesUserScopeForEveryResource() throws Exception {
        MyDataService service = mock(MyDataService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new MyDataMockController(service)).build();
        mvc.perform(get("/mock/v2/insu/insurance").param("userId", "42")).andExpect(status().isOk());
        mvc.perform(get("/mock/v2/insu/insurances/INS-42/property").param("userId", "42")).andExpect(status().isOk());
        for (String resource : new String[]{"basic", "contracts", "coverages"}) {
            mvc.perform(get("/mock/v2/insu/insurances/" + resource).param("userId", "42").param("insuNum", "INS-42"))
                    .andExpect(status().isOk());
        }
        verify(service).getInsuranceList(42L);
        verify(service).getInsuranceProperty(42L, "INS-42");
        verify(service).getInsuranceBasic(42L, "INS-42");
        verify(service).getInsuranceContracts(42L, "INS-42");
        verify(service).getInsuranceCoverages(42L, "INS-42");
        verifyNoMoreInteractions(service);
    }
}
