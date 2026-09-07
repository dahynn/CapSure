package com.capsule.insurance.mydata.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MyDataProxyServiceTest {
    @Test void passesUserIdentityThroughAllProviderRequests() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> queries = new ArrayList<>();
        server.createContext("/mock/v2/insu/", exchange -> {
            queries.add(exchange.getRequestURI().getQuery());
            String path = exchange.getRequestURI().getPath();
            String body = path.endsWith("/insurance")
                    ? "{\"contracts\":[{\"insuNum\":\"INS-42\"}]}"
                    : path.endsWith("/coverages") ? "{\"insuNum\":\"INS-42\",\"coverages\":[]}"
                    : "{\"insuNum\":\"INS-42\",\"contractListJson\":\"[]\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            var proxy = new MyDataProxyService(new ObjectMapper(),
                    "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(5));
            var response = proxy.getUserInsurances(42L);
            assertThat(response.userId()).isEqualTo(42L);
            assertThat(response.contracts()).hasSize(1);
            assertThat(queries).hasSize(4).allSatisfy(query -> assertThat(query).contains("userId=42"));
        } finally {
            server.stop(0);
        }
    }
}
