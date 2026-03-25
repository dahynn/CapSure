package com.capsule.insurance.mydata.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("main")
@Service
public class MyDataProxyService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final Duration timeout;

    public MyDataProxyService(
            ObjectMapper objectMapper,
            @Value("${mydata.mock.base-url}") String baseUrl,
            @Value("${mydata.mock.timeout:5s}") Duration timeout
    ) {
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    public JsonNode getUserInsurances(Long userId) {
        String path = "/mock/mydata/users/" + userId + "/insurances";
        HttpRequest request = HttpRequest.newBuilder(resolveUri(path))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Mydata proxy call failed: " + path + " (" + response.statusCode() + ")"
                );
            }
            return objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Mydata proxy call failed: " + path);
        }
    }

    private URI resolveUri(String path) {
        try {
            return new URI(baseUrl + path);
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Invalid mydata proxy URL");
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
