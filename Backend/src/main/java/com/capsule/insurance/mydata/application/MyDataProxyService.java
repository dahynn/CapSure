package com.capsule.insurance.mydata.application;

import com.capsule.insurance.common.exception.BusinessException;
import com.capsule.insurance.common.exception.ErrorCode;
import com.capsule.insurance.mydata.dto.MyDataContractResponse;
import com.capsule.insurance.mydata.dto.MyDataInsuranceContractsResponse;
import com.capsule.insurance.mydata.dto.MyDataInsuranceCoveragesResponse;
import com.capsule.insurance.mydata.dto.MyDataInsuranceListItemResponse;
import com.capsule.insurance.mydata.dto.MyDataInsuranceListResponse;
import com.capsule.insurance.mydata.dto.MyDataUserInsurancesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
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

    public MyDataUserInsurancesResponse getUserInsurances(Long userId) {
        MyDataInsuranceListResponse insuranceList = get("/mock/v2/insu/insurance", MyDataInsuranceListResponse.class);

        List<MyDataContractResponse> contracts = insuranceList.contracts().stream()
                .map(this::toUserContract)
                .toList();

        return new MyDataUserInsurancesResponse(userId, contracts);
    }

    private MyDataContractResponse toUserContract(MyDataInsuranceListItemResponse contract) {
        String encodedInsuNum = encode(contract.insuNum());

        MyDataContractResponse property = get(
                "/mock/v2/insu/insurances/" + encodedInsuNum + "/property",
                MyDataContractResponse.class
        );
        MyDataInsuranceContractsResponse contracts = get(
                "/mock/v2/insu/insurances/contracts?insuNum=" + encodedInsuNum,
                MyDataInsuranceContractsResponse.class
        );
        MyDataInsuranceCoveragesResponse coverages = get(
                "/mock/v2/insu/insurances/coverages?insuNum=" + encodedInsuNum,
                MyDataInsuranceCoveragesResponse.class
        );

        return new MyDataContractResponse(
                property.mydContractId(),
                property.insuNum(),
                property.providerCode(),
                property.productName(),
                property.businessType(),
                property.insuTypeCode(),
                property.contractStatusCode(),
                property.contractDate(),
                property.startDate(),
                property.endDate(),
                property.premiumAmount(),
                contracts.contractListJson(),
                coverages.coverages()
        );
    }

    private <T> T get(String path, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder(resolveUri(path))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        return send(request, path, responseType);
    }

    private <T> T send(HttpRequest request, String path, Class<T> responseType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "Mydata proxy call failed: " + path + " (" + response.statusCode() + ")"
                );
            }
            return objectMapper.readValue(response.body(), responseType);
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

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
