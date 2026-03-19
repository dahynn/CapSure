// #Demo Setting
package com.capsule.insurance.common.logging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class RequestIdFilterTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .addFilters(new RequestIdFilter())
            .build();

    @Test
    void requestIdIsGeneratedAndReturnedInResponseHeader() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/filter-test"))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = mvcResult.getResponse().getHeader("X-Request-Id");

        assertThat(requestId).isNotBlank();
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("ok");
    }

    @RestController
    static class TestController {

        @GetMapping("/filter-test")
        String ping() {
            return "ok";
        }
    }
}
