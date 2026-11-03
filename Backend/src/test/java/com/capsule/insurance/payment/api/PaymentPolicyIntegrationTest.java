package com.capsule.insurance.payment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capsule.insurance.application.application.ApplicationService;
import com.capsule.insurance.application.dto.CreateApplicationRequest;
import com.capsule.insurance.application.dto.CreateConsentRequest;
import com.capsule.insurance.application.dto.ReplaceDisclosuresRequest;
import com.capsule.insurance.application.infra.JdbcApplicationRepository;
import com.capsule.insurance.catalog.infra.JdbcCancerProductQueryRepository;
import com.capsule.insurance.common.exception.GlobalExceptionHandler;
import com.capsule.insurance.payment.adapter.FakePremiumPaymentGateway;
import com.capsule.insurance.payment.adapter.JournaledPremiumPaymentGateway;
import com.capsule.insurance.payment.application.PaymentService;
import com.capsule.insurance.payment.application.port.PremiumPaymentGateway;
import com.capsule.insurance.payment.domain.GatewayPaymentResult;
import com.capsule.insurance.payment.dto.PaymentOrderResponse;
import com.capsule.insurance.payment.infra.JdbcPaymentRepository;
import com.capsule.insurance.payment.infra.JdbcFinancialInterfaceJournalRepository;
import com.capsule.insurance.payment.webhook.api.FakePaymentWebhookOperationsController;
import com.capsule.insurance.payment.webhook.application.PaymentWebhookService;
import com.capsule.insurance.payment.webhook.infra.JdbcPaymentWebhookRepository;
import com.capsule.insurance.policy.api.PolicyController;
import com.capsule.insurance.policy.application.PolicyService;
import com.capsule.insurance.policy.application.port.PolicyRepository;
import com.capsule.insurance.policy.domain.InsurancePolicy;
import com.capsule.insurance.policy.infra.JdbcPolicyRepository;
import com.capsule.insurance.quote.application.QuoteService;
import com.capsule.insurance.quote.dto.CreateQuoteRequest;
import com.capsule.insurance.quote.infra.JdbcQuoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PaymentPolicyIntegrationTest {

    private static final String TERMS_HASH =
            "c4ac2f41311096ba768d0550eecef4bdf21937ac3afb8ef8dad2bf5a6c03f04a";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("capsure_payment_test")
            .withUsername("capsure")
            .withPassword("capsure");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static JdbcTemplate jdbcTemplate;
    private static DriverManagerDataSource dataSource;
    private static QuoteService quoteService;
    private static ApplicationService applicationService;
    private static JdbcPolicyRepository policyRepository;
    private static FakePremiumPaymentGateway paymentGateway;
    private static PaymentService paymentService;
    private static MockMvc mockMvc;
    private static MockMvc webhookMockMvc;
    private static Long productVersionId;
    private static List<Long> productCoverageIds;
    private static List<Long> userIds;

    @BeforeAll
    static void setUp() throws Exception {
        createLegacySchema();

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .cleanDisabled(true)
                .load()
                .migrate();

        dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        userIds = new ArrayList<>();
        for (int index = 1; index <= 8; index++) {
            userIds.add(insertUser(
                    "payment-user-" + index + "@capsure.test",
                    "결제" + index,
                    "010-4000-40" + String.format("%02d", index)
            ));
        }
        productVersionId = jdbcTemplate.queryForObject("""
                SELECT product_version_id
                FROM public.ins_product_version
                WHERE product_code = 'CAPSURE-DEMO-CANCER'
                  AND version = '1.0.0'
                """, Long.class);
        productCoverageIds = jdbcTemplate.queryForList("""
                SELECT product_coverage_id
                FROM public.ins_product_coverage
                WHERE product_version_id = ?
                ORDER BY display_order
                """, Long.class, productVersionId);

        quoteService = new QuoteService(
                new JdbcCancerProductQueryRepository(jdbcTemplate),
                new JdbcQuoteRepository(jdbcTemplate, OBJECT_MAPPER)
        );
        applicationService = new ApplicationService(
                new JdbcApplicationRepository(jdbcTemplate, OBJECT_MAPPER),
                new DataSourceTransactionManager(dataSource)
        );
        policyRepository = new JdbcPolicyRepository(jdbcTemplate, OBJECT_MAPPER);
        paymentGateway = new FakePremiumPaymentGateway();
        paymentService = newPaymentService(policyRepository);
        mockMvc = buildMockMvc(paymentService, policyRepository);
        PaymentWebhookService webhookService = new PaymentWebhookService(
                new JdbcPaymentWebhookRepository(jdbcTemplate),
                paymentService,
                OBJECT_MAPPER,
                new DataSourceTransactionManager(dataSource)
        );
        webhookMockMvc = MockMvcBuilders
                .standaloneSetup(new FakePaymentWebhookOperationsController(webhookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("인수 승인 전에는 주문을 만들 수 없고 승인 후 서버 견적 금액으로 한 주문만 만든다")
    void createsOneServerPricedOrderOnlyAfterApproval() throws Exception {
        Long userId = userIds.get(0);
        Long draftApplicationId = createDraftApplication(userId);

        mockMvc.perform(post("/api/v1/applications/{applicationId}/payment-orders", draftApplicationId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "order-before-approval"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        Long approvedApplicationId = approveApplication(userId);
        JsonNode first = createOrder(userId, approvedApplicationId, "order-approved-1");
        JsonNode repeated = createOrder(userId, approvedApplicationId, "order-approved-2");

        assertThat(repeated.path("paymentOrderId").asLong())
                .isEqualTo(first.path("paymentOrderId").asLong());
        assertThat(first.path("amount").decimalValue()).isEqualByComparingTo("29900.00");
        assertThat(first.path("status").asText()).isEqualTo("CREATED");
        assertThat(first.path("policyStatus").asText()).isEqualTo("PENDING_INITIAL_PREMIUM");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.pay_order
                WHERE application_id = ?
                """, Integer.class, approvedApplicationId)).isEqualTo(1);
    }

    @Test
    void otherUsersCannotReadPolicyOrPaymentAndResponsesOmitProviderKeys() throws Exception {
        Long owner = userIds.get(0);
        Long stranger = userIds.get(1);
        Long application = approveApplication(owner);
        JsonNode order = createOrder(owner, application, "privacy-order");
        long paymentId = order.path("paymentOrderId").asLong();
        long policyId = order.path("policyId").asLong();
        for (String path : List.of("/api/v1/payments/" + paymentId, "/api/v1/policies/" + policyId)) {
            mockMvc.perform(get(path).principal(authentication(owner))).andExpect(status().isOk());
            mockMvc.perform(get(path).principal(authentication(stranger))).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
        var response = paymentService.confirm(owner, paymentId, "privacy-confirm",
                new com.capsule.insurance.payment.dto.ConfirmPaymentRequest("fake-timeout-private-key", new java.math.BigDecimal("29900.00")));
        assertThat(response.attempts()).hasSize(1);
        mockMvc.perform(get("/api/v1/payments/{id}", paymentId).principal(authentication(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempts[0].providerPaymentKey").doesNotExist());
    }

    @Test
    @DisplayName("Toss 승인 주문번호가 서버 주문과 다르면 PG 호출 전에 거절한다")
    void rejectsMismatchedTossOrderIdBeforeGatewayCall() throws Exception {
        Long userId = userIds.get(0);
        AtomicBoolean gatewayCalled = new AtomicBoolean(false);
        PremiumPaymentGateway tossGateway = new PremiumPaymentGateway() {
            @Override
            public String providerCode() {
                return "TOSS";
            }

            @Override
            public GatewayPaymentResult confirm(ConfirmCommand command) {
                gatewayCalled.set(true);
                return GatewayPaymentResult.paid(command.providerPaymentKey(), "test-transaction");
            }

            @Override
            public GatewayPaymentResult inquire(InquiryCommand command) {
                return GatewayPaymentResult.unknown(command.providerPaymentKey(), "TEST_NOT_FOUND");
            }
        };
        PaymentService tossPaymentService = new PaymentService(
                new JdbcPaymentRepository(jdbcTemplate),
                policyRepository,
                tossGateway,
                new DataSourceTransactionManager(dataSource),
                OBJECT_MAPPER
        );
        MockMvc tossMockMvc = buildMockMvc(tossPaymentService, policyRepository);
        JsonNode order = createOrder(
                tossMockMvc,
                userId,
                approveApplication(userId),
                "order-toss-mismatch"
        );
        Long orderId = order.path("paymentOrderId").asLong();

        tossMockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-toss-mismatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(Map.of(
                                "providerPaymentKey", "toss-payment-key",
                                "providerOrderId", "PAY-TAMPERED",
                                "amount", "29900.00"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        assertThat(gatewayCalled).isFalse();
        assertThat(count("pay_attempt", "payment_order_id", orderId)).isZero();
    }

    @Test
    @DisplayName("위변조 금액은 PG 호출 전에 거절하고 동일 confirm 100회는 승인·시도·활성 계약을 한 건만 만든다")
    void confirmsOnePaymentAndPolicyForOneHundredRetries() throws Exception {
        Long userId = userIds.get(1);
        Long applicationId = approveApplication(userId);
        JsonNode order = createOrder(userId, applicationId, "order-confirm-100");
        Long orderId = order.path("paymentOrderId").asLong();
        Long policyId = order.path("policyId").asLong();
        int invocationsBefore = paymentGateway.confirmationInvocationCount();

        mockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-tampered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("fake-paid-tampered", "1.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
        assertThat(paymentGateway.confirmationInvocationCount()).isEqualTo(invocationsBefore);

        Long firstAttemptId = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            JsonNode response = confirm(
                    userId,
                    orderId,
                    "confirm-100-same-key",
                    "fake-paid-confirm-100"
            );
            assertThat(response.path("status").asText()).isEqualTo("PAID");
            Long attemptId = response.path("attempts").get(0).path("paymentAttemptId").asLong();
            if (firstAttemptId == null) {
                firstAttemptId = attemptId;
            }
            assertThat(attemptId).isEqualTo(firstAttemptId);
        }

        assertThat(paymentGateway.confirmationInvocationCount()).isEqualTo(invocationsBefore + 1);
        mockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-100-same-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("fake-paid-different-payload", "29900.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));
        assertThat(paymentGateway.confirmationInvocationCount()).isEqualTo(invocationsBefore + 1);
        assertThat(count("pay_attempt", "payment_order_id", orderId)).isEqualTo(1);
        assertThat(count("ins_policy_version", "policy_id", policyId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ins_policy_coverage coverage
                JOIN public.ins_policy_version version
                  ON version.policy_version_id = coverage.policy_version_id
                WHERE version.policy_id = ?
                """, Integer.class, policyId)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ops_outbox_event
                WHERE aggregate_type = 'POLICY'
                  AND aggregate_id = ?
                  AND event_type = 'POLICY_ACTIVATED'
                """, Integer.class, policyId.toString())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ops_outbox_event
                WHERE event_type IN ('PAYMENT_PAID', 'POLICY_ACTIVATED')
                  AND payload_json ->> 'policyId' = ?
                """, Integer.class, policyId.toString())).isEqualTo(2);

        mockMvc.perform(get("/api/v1/policies/{policyId}", policyId)
                        .principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.policyVersion.version").value(1))
                .andExpect(jsonPath("$.data.policyVersion.coverages.length()").value(3));
    }

    @Test
    @DisplayName("결제 거절은 계약을 활성화하지 않는다")
    void keepsPolicyPendingWhenPaymentFails() throws Exception {
        Long userId = userIds.get(2);
        JsonNode order = createOrder(userId, approveApplication(userId), "order-failed");
        Long orderId = order.path("paymentOrderId").asLong();
        Long policyId = order.path("policyId").asLong();

        JsonNode failed = confirm(userId, orderId, "confirm-failed", "fake-failed-card");

        assertThat(failed.path("status").asText()).isEqualTo("FAILED");
        assertThat(failed.path("policyStatus").asText()).isEqualTo("PENDING_INITIAL_PREMIUM");
        assertThat(count("ins_policy_version", "policy_id", policyId)).isZero();
    }

    @Test
    @DisplayName("PG timeout은 UNKNOWN과 비활성 계약을 유지하고 대사 성공 시 정확히 한 계약을 활성화한다")
    void reconcilesUnknownPaymentIntoOneActivePolicy() throws Exception {
        Long userId = userIds.get(3);
        JsonNode order = createOrder(userId, approveApplication(userId), "order-timeout");
        Long orderId = order.path("paymentOrderId").asLong();
        Long policyId = order.path("policyId").asLong();

        mockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-timeout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("fake-timeout-reconcile", "29900.00")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.policyStatus").value("PENDING_INITIAL_PREMIUM"));
        assertThat(count("ins_policy_version", "policy_id", policyId)).isZero();

        paymentGateway.settleAsPaid("fake-timeout-reconcile");
        PaymentOrderResponse reconciled = paymentService.reconcile(orderId);

        assertThat(reconciled.status()).isEqualTo("PAID");
        assertThat(reconciled.policyStatus()).isEqualTo("ACTIVE");
        assertThat(attemptStatus(orderId)).isEqualTo("PAID");
        assertThat(count("ins_policy_version", "policy_id", policyId)).isEqualTo(1);
        assertThat(policyActivationOutboxCount(policyId)).isEqualTo(1);
        assertThat(count("ops_reconciliation", "target_id", orderId.toString())).isEqualTo(1);
    }

    @Test
    @DisplayName("현재 PG와 다른 provider의 미확정 시도는 외부 조회하지 않는다")
    void doesNotReconcileAnAttemptThroughAnotherProvider() throws Exception {
        Long userId = userIds.get(3);
        JsonNode order = createOrder(
                userId,
                approveApplication(userId),
                "order-provider-boundary"
        );
        Long orderId = order.path("paymentOrderId").asLong();
        mockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-provider-boundary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("fake-timeout-provider-boundary", "29900.00")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("UNKNOWN"));

        AtomicBoolean inquiryCalled = new AtomicBoolean(false);
        PremiumPaymentGateway tossGateway = new PremiumPaymentGateway() {
            @Override
            public String providerCode() {
                return "TOSS";
            }

            @Override
            public GatewayPaymentResult confirm(ConfirmCommand command) {
                return GatewayPaymentResult.unknown(command.providerPaymentKey(), "TEST_UNUSED");
            }

            @Override
            public GatewayPaymentResult inquire(InquiryCommand command) {
                inquiryCalled.set(true);
                return GatewayPaymentResult.failed(command.providerPaymentKey(), "TEST_WRONG_PROVIDER");
            }
        };
        PaymentService tossPaymentService = new PaymentService(
                new JdbcPaymentRepository(jdbcTemplate),
                policyRepository,
                tossGateway,
                new DataSourceTransactionManager(dataSource),
                OBJECT_MAPPER
        );

        PaymentOrderResponse unchanged = tossPaymentService.reconcile(orderId);

        assertThat(unchanged.status()).isEqualTo("UNKNOWN");
        assertThat(inquiryCalled).isFalse();
        assertThat(attemptStatus(orderId)).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("PG 승인 뒤 증권 발행 장애는 APPROVING으로 남고 대사 재시작이 중복 없이 복구한다")
    void recoversPolicyIssuanceFailureAfterGatewayApproval() throws Exception {
        Long userId = userIds.get(4);
        Long applicationId = approveApplication(userId);
        FailsOncePolicyRepository failsOncePolicyRepository =
                new FailsOncePolicyRepository(policyRepository);
        PaymentService recoverablePaymentService = newPaymentService(failsOncePolicyRepository);
        MockMvc recoverableMockMvc = buildMockMvc(recoverablePaymentService, failsOncePolicyRepository);
        JsonNode order = createOrder(
                recoverableMockMvc,
                userId,
                applicationId,
                "order-saga-recovery"
        );
        Long orderId = order.path("paymentOrderId").asLong();
        Long policyId = order.path("policyId").asLong();

        recoverableMockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-saga-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("fake-paid-saga-recovery", "29900.00")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));

        assertThat(paymentStatus(orderId)).isEqualTo("APPROVING");
        assertThat(attemptStatus(orderId)).isEqualTo("PROCESSING");
        assertThat(policyStatus(policyId)).isEqualTo("PENDING_INITIAL_PREMIUM");
        assertThat(count("ins_policy_version", "policy_id", policyId)).isZero();

        PaymentOrderResponse recovered = recoverablePaymentService.reconcile(orderId);

        assertThat(recovered.status()).isEqualTo("PAID");
        assertThat(recovered.policyStatus()).isEqualTo("ACTIVE");
        assertThat(count("pay_attempt", "payment_order_id", orderId)).isEqualTo(1);
        assertThat(count("ins_policy_version", "policy_id", policyId)).isEqualTo(1);
        assertThat(policyActivationOutboxCount(policyId)).isEqualTo(1);
        assertThat(count("ops_reconciliation", "target_id", orderId.toString())).isEqualTo(1);
    }

    @Test
    @DisplayName("외부 결제 전문은 요청과 응답을 상관관계·멱등키로 함께 기록하고 중복 승인 호출은 남기지 않는다")
    void journalsExternalPaymentRequestAndResponseWithoutDuplicateCall() throws Exception {
        Long userId = userIds.get(5);
        Long applicationId = approveApplication(userId);
        FakePremiumPaymentGateway delegate = new FakePremiumPaymentGateway();
        PaymentService journaledPaymentService = new PaymentService(
                new JdbcPaymentRepository(jdbcTemplate),
                policyRepository,
                new JournaledPremiumPaymentGateway(
                        delegate,
                        new JdbcFinancialInterfaceJournalRepository(jdbcTemplate),
                        OBJECT_MAPPER,
                        Clock.systemUTC(),
                        3,
                        Duration.ofSeconds(30)
                ),
                new DataSourceTransactionManager(dataSource),
                OBJECT_MAPPER
        );
        MockMvc journaledMockMvc = buildMockMvc(journaledPaymentService, policyRepository);
        JsonNode order = createOrder(journaledMockMvc, userId, applicationId, "order-interface-journal");
        Long orderId = order.path("paymentOrderId").asLong();
        String orderNo = order.path("orderNo").asText();

        MvcResult first = journaledMockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-interface-journal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("fake-paid-interface-journal", "29900.00")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(OBJECT_MAPPER.readTree(first.getResponse().getContentAsByteArray())
                .path("data").path("status").asText()).isEqualTo("PAID");

        journaledMockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-interface-journal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("fake-paid-interface-journal", "29900.00")))
                .andExpect(status().isOk());

        assertThat(delegate.confirmationInvocationCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ifc_financial_message
                WHERE business_key = ?
                """, Integer.class, orderNo)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("""
                SELECT direction || ':' || status
                FROM public.ifc_financial_message
                WHERE business_key = ?
                ORDER BY financial_message_id
                """, String.class, orderNo)).containsExactly(
                "OUTBOUND_REQUEST:REQUESTED",
                "INBOUND_RESPONSE:SUCCEEDED"
        );
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ifc_financial_message
                WHERE business_key = ?
                  AND idempotency_key = 'confirm-interface-journal'
                  AND payload_hash ~ '^[0-9a-f]{64}$'
                """, Integer.class, orderNo)).isEqualTo(2);
    }

    @Test
    @DisplayName("동일 webhook 100회는 inbox와 결제·계약 상태를 한 번만 전이시키고 payload 변조를 거절한다")
    void deduplicatesOneHundredWebhookDeliveries() throws Exception {
        Long userId = userIds.get(5);
        JsonNode order = createOrder(userId, approveApplication(userId), "order-webhook");
        Long orderId = order.path("paymentOrderId").asLong();
        Long policyId = order.path("policyId").asLong();
        int invocationsBefore = paymentGateway.confirmationInvocationCount();

        mockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", "confirm-webhook-timeout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest("fake-timeout-webhook", "29900.00")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("UNKNOWN"));

        Long inboxId = null;
        for (int delivery = 0; delivery < 100; delivery++) {
            MvcResult result = webhookMockMvc.perform(post("/api/v1/ops/webhooks/fake/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookRequest("fake-event-100", "fake-timeout-webhook", "PAID")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.processingStatus").value("PROCESSED"))
                    .andExpect(jsonPath("$.data.duplicate").value(delivery > 0))
                    .andReturn();
            Long currentInboxId = OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray())
                    .path("data").path("paymentWebhookEventId").asLong();
            if (inboxId == null) {
                inboxId = currentInboxId;
            }
            assertThat(currentInboxId).isEqualTo(inboxId);
        }

        assertThat(paymentGateway.confirmationInvocationCount()).isEqualTo(invocationsBefore + 1);
        assertThat(paymentStatus(orderId)).isEqualTo("PAID");
        assertThat(attemptStatus(orderId)).isEqualTo("PAID");
        assertThat(policyStatus(policyId)).isEqualTo("ACTIVE");
        assertThat(count("pay_webhook_event", "provider_event_id", "fake-event-100")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT payload_hash
                FROM public.pay_webhook_event
                WHERE provider = 'FAKE'
                  AND provider_event_id = 'fake-event-100'
                """, String.class)).matches("[0-9a-f]{64}");
        assertThat(count("ins_policy_version", "policy_id", policyId)).isEqualTo(1);
        assertThat(policyActivationOutboxCount(policyId)).isEqualTo(1);

        webhookMockMvc.perform(post("/api/v1/ops/webhooks/fake/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookRequest("fake-event-100", "fake-timeout-webhook", "FAILED")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_CONFLICT"));
    }

    private static PaymentService newPaymentService(PolicyRepository selectedPolicyRepository) {
        return new PaymentService(
                new JdbcPaymentRepository(jdbcTemplate),
                selectedPolicyRepository,
                paymentGateway,
                new DataSourceTransactionManager(dataSource),
                OBJECT_MAPPER
        );
    }

    @Test
    @DisplayName("confirm 재요청·webhook·대사가 경합해도 승인과 계약 활성화는 한 번이다")
    void racesConfirmationWebhookAndReconciliation() throws Exception {
        Long userId=userIds.get(0);
        Long applicationId=approveApplication(userId);
        JsonNode order=createOrder(userId,applicationId,"race-create");
        Long orderId=order.path("paymentOrderId").asLong();
        Long policyId=order.path("policyId").asLong();
        String providerKey="fake-paid-race";
        var approved=new java.util.concurrent.CountDownLatch(1);
        var release=new java.util.concurrent.CountDownLatch(1);
        var delegate=new FakePremiumPaymentGateway();
        var gateway=new com.capsule.insurance.payment.application.port.PremiumPaymentGateway(){
            public com.capsule.insurance.payment.domain.GatewayPaymentResult confirm(ConfirmCommand command){
                var result=delegate.confirm(command); approved.countDown();
                try {if(!release.await(20,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("release timed out");}
                catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
                return result;
            }
            public com.capsule.insurance.payment.domain.GatewayPaymentResult inquire(InquiryCommand command){return delegate.inquire(command);}
        };
        var service=new PaymentService(new JdbcPaymentRepository(jdbcTemplate),policyRepository,gateway,
                new DataSourceTransactionManager(dataSource),OBJECT_MAPPER);
        var webhook=new PaymentWebhookService(new JdbcPaymentWebhookRepository(jdbcTemplate),service,
                OBJECT_MAPPER,new DataSourceTransactionManager(dataSource));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(16);
        try {
            var first=pool.submit(() -> service.confirm(userId,orderId,"race-confirm",
                    new com.capsule.insurance.payment.dto.ConfirmPaymentRequest(providerKey,new java.math.BigDecimal("29900.00"))));
            assertThat(approved.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var start=new java.util.concurrent.CountDownLatch(1);
            var futures=new ArrayList<java.util.concurrent.Future<?>>();
            for(int i=0;i<30;i++){
                int path=i%3;
                futures.add(pool.submit(() -> {
                    start.await();
                    if(path==0) service.confirm(userId,orderId,"race-confirm",
                            new com.capsule.insurance.payment.dto.ConfirmPaymentRequest(providerKey,new java.math.BigDecimal("29900.00")));
                    if(path==1) webhook.receiveFakeWebhook(new com.capsule.insurance.payment.webhook.dto.FakePaymentWebhookRequest(
                            "race-event",providerKey,"PAYMENT_STATUS_CHANGED","PAID"));
                    if(path==2) service.reconcile(orderId);
                    return null;
                }));
            }
            start.countDown();
            for(var future:futures) future.get(15,java.util.concurrent.TimeUnit.SECONDS);
            release.countDown(); first.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(delegate.confirmationInvocationCount()).isEqualTo(1);
            assertThat(paymentStatus(orderId)).isEqualTo("PAID");
            assertThat(count("pay_attempt","payment_order_id",orderId)).isEqualTo(1);
            assertThat(count("ins_policy_version","policy_id",policyId)).isEqualTo(1);
            assertThat(policyActivationOutboxCount(policyId)).isEqualTo(1);
            assertThat(count("pay_webhook_event","provider_event_id","race-event")).isEqualTo(1);
        } finally { release.countDown();pool.shutdownNow(); }
    }

    @Test
    @DisplayName("PG 승인 후 JVM을 강제 종료해도 새 JVM에서 재승인 없이 계약을 복구한다")
    void recoversAfterProcessIsKilledFollowingProviderApproval() throws Exception {
        Long userId=userIds.get(0);
        JsonNode order=createOrder(userId,approveApplication(userId),"kill-create");
        Long orderId=order.path("paymentOrderId").asLong();
        Long policyId=order.path("policyId").asLong();
        // Test-only provider ledger survives application JVM death. Separate connections commit it independently.
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test_provider_ledger(provider_key TEXT PRIMARY KEY,confirm_calls INTEGER NOT NULL)");
        var marker=java.nio.file.Files.createTempFile("capsure-pg-approved-",".marker");
        java.nio.file.Files.delete(marker);
        var log=java.nio.file.Files.createTempFile("capsure-payment-worker-",".log");
        Process child=startWorker("confirm",userId,orderId,marker,log);
        try {
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
            while(!java.nio.file.Files.exists(marker) && child.isAlive() && System.nanoTime()<deadline) Thread.sleep(25);
            assertThat(java.nio.file.Files.exists(marker)).withFailMessage(java.nio.file.Files.readString(log)).isTrue();
            child.destroyForcibly(); assertThat(child.waitFor(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(paymentStatus(orderId)).isEqualTo("APPROVING");
            assertThat(count("ins_policy_version","policy_id",policyId)).isZero();
            Process restarted=startWorker("reconcile",userId,orderId,marker,log);
            try {
                assertThat(restarted.waitFor(20,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThat(restarted.exitValue()).withFailMessage(java.nio.file.Files.readString(log)).isZero();
            } finally {if(restarted.isAlive()) restarted.destroyForcibly();}
            assertThat(paymentStatus(orderId)).isEqualTo("PAID");
            assertThat(policyStatus(policyId)).isEqualTo("ACTIVE");
            assertThat(count("ins_policy_version","policy_id",policyId)).isEqualTo(1);
            assertThat(policyActivationOutboxCount(policyId)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT confirm_calls FROM test_provider_ledger WHERE provider_key=?",Integer.class,"fake-paid-kill-"+orderId)).isEqualTo(1);
        } finally {if(child.isAlive())child.destroyForcibly();java.nio.file.Files.deleteIfExists(marker);}
    }

    private static Process startWorker(String mode,Long userId,Long orderId,java.nio.file.Path marker,java.nio.file.Path log) throws Exception {
        return new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-cp",System.getProperty("capsure.test.classpath"),ForkedPaymentWorker.class.getName(),
                mode,POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword(),userId.toString(),orderId.toString(),marker.toString())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
    }

    public static class ForkedPaymentWorker {
        public static void main(String[] args) throws Exception {
            var source=new DriverManagerDataSource(args[1],args[2],args[3]);
            var db=new JdbcTemplate(source);
            var gateway=new com.capsule.insurance.payment.application.port.PremiumPaymentGateway(){
                public com.capsule.insurance.payment.domain.GatewayPaymentResult confirm(ConfirmCommand command){
                    db.update("INSERT INTO test_provider_ledger VALUES(?,1) ON CONFLICT(provider_key) DO UPDATE SET confirm_calls=test_provider_ledger.confirm_calls+1",command.providerPaymentKey());
                    try{java.nio.file.Files.writeString(java.nio.file.Path.of(args[6]),"APPROVED");Thread.sleep(60000);}
                    catch(Exception ex){throw new IllegalStateException(ex);}
                    return com.capsule.insurance.payment.domain.GatewayPaymentResult.paid(command.providerPaymentKey(),"TEST-DURABLE-PG");
                }
                public com.capsule.insurance.payment.domain.GatewayPaymentResult inquire(InquiryCommand command){
                    String key=command.providerPaymentKey();
                    if(db.queryForObject("SELECT COUNT(*) FROM test_provider_ledger WHERE provider_key=?",Integer.class,key)!=1) throw new IllegalStateException("missing approval");
                    return com.capsule.insurance.payment.domain.GatewayPaymentResult.paid(key,"TEST-DURABLE-PG");
                }
            };
            var service=new PaymentService(new JdbcPaymentRepository(db),new JdbcPolicyRepository(db,OBJECT_MAPPER),gateway,
                    new DataSourceTransactionManager(source),OBJECT_MAPPER);
            Long userId=Long.valueOf(args[4]),orderId=Long.valueOf(args[5]);
            if("confirm".equals(args[0])) service.confirm(userId,orderId,"kill-confirm-"+orderId,
                    new com.capsule.insurance.payment.dto.ConfirmPaymentRequest("fake-paid-kill-"+orderId,new java.math.BigDecimal("29900.00")));
            else service.reconcile(orderId);
        }
    }

    @Test
    @DisplayName("선택 보장 보험료가 결제 금액·계약에 반영되고 다음 달 변경과 청구가 일치한다")
    void pricesSelectionsAndSchedulesNextMonthCoverage() throws Exception {
        Long userId=userIds.get(0);
        var single=quoteService.issue(userId,new CreateQuoteRequest(productVersionId,List.of(productCoverageIds.get(0))));
        assertThat(single.monthlyPremium()).isEqualByComparingTo("19900.00");
        assertThat(single.snapshot().coverages()).hasSize(1);
        var pair=quoteService.issue(userId,new CreateQuoteRequest(productVersionId,productCoverageIds.subList(0,2)));
        assertThat(pair.monthlyPremium()).isEqualByComparingTo("26900.00");
        // Activate a full plan, then reserve a reduction. Quote and order retain server-owned prices.
        JsonNode order=createOrder(userId,approveApplication(userId),"coverage-create");
        Long orderId=order.path("paymentOrderId").asLong();
        confirm(userId,orderId,"coverage-confirm","fake-paid-coverage");
        Long policyId=order.path("policyId").asLong();
        var changes=new com.capsule.insurance.policy.change.CoverageChangeService(jdbcTemplate,policyRepository,
                new JdbcQuoteRepository(jdbcTemplate,OBJECT_MAPPER),new com.capsule.insurance.premiumcollection.infra.JdbcPremiumCollectionRepository(jdbcTemplate),
                OBJECT_MAPPER,new DataSourceTransactionManager(dataSource));
        var request=new com.capsule.insurance.policy.change.CoverageChangeService.ChangeRequest(single.quoteId(),TERMS_HASH,true);
        var reserved=changes.schedule(userId,policyId,"coverage-schedule",request);
        assertThat(changes.schedule(userId,policyId,"coverage-schedule",request).policyChangeId()).isEqualTo(reserved.policyChangeId());
        assertThat(reserved.quote().monthlyPremium()).isEqualByComparingTo("19900.00");
        assertThat(policyRepository.findOwned(policyId,userId).orElseThrow().policyVersion().version()).isEqualTo(1);
        assertThat(policyRepository.findOwned(policyId,userId).orElseThrow().policyVersion().coverages()).hasSize(3);
        var beforeRepository=new JdbcPolicyRepository(jdbcTemplate,OBJECT_MAPPER,Clock.fixed(reserved.effectiveAt().minusNanos(1000),java.time.ZoneOffset.UTC));
        assertThat(beforeRepository.findOwned(policyId,userId).orElseThrow().policyVersion().version()).isEqualTo(1);
        var afterRepository=new JdbcPolicyRepository(jdbcTemplate,OBJECT_MAPPER,Clock.fixed(reserved.effectiveAt(),java.time.ZoneOffset.UTC));
        var after=afterRepository.findOwned(policyId,userId).orElseThrow();
        assertThat(after.policyVersion().version()).isEqualTo(2);
        assertThat(after.policyVersion().coverages()).hasSize(1);
        assertThat(after.policyVersion().snapshot().quote().monthlyPremium()).isEqualByComparingTo("19900.00");
        var billing=changes.createNextPremium(userId,policyId);
        assertThat(billing.amountDue()).isEqualByComparingTo("19900.00");
        assertThat(jdbcTemplate.queryForObject("SELECT policy_version_id FROM ins_premium_receivable WHERE premium_receivable_id=?",Long.class,billing.premiumReceivableId()))
                .isEqualTo(after.policyVersion().policyVersionId());
        assertThat(changes.createNextPremium(userId,policyId).premiumReceivableId()).isEqualTo(billing.premiumReceivableId());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> changes.schedule(userIds.get(1),policyId,"coverage-other-user",request))
                .isInstanceOf(com.capsule.insurance.common.exception.BusinessException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> changes.schedule(userId,policyId,"coverage-schedule",
                new com.capsule.insurance.policy.change.CoverageChangeService.ChangeRequest(pair.quoteId(),TERMS_HASH,true)))
                .isInstanceOf(com.capsule.insurance.common.exception.BusinessException.class);
        // A historical coverage remains bounded by its version end, even if its own end is null.
        Long oldCoverage=jdbcTemplate.queryForObject("""
                SELECT c.policy_coverage_id FROM ins_policy_coverage c JOIN ins_policy_version v USING(policy_version_id)
                WHERE v.policy_id=? AND v.version=1 ORDER BY c.policy_coverage_id LIMIT 1
                """,Long.class,policyId);
        var claims=new com.capsule.insurance.claim.infra.JdbcClaimRepository(jdbcTemplate,OBJECT_MAPPER);
        var claim=claims.createDraft("C-BOUNDARY-"+policyId,policyId,oldCoverage,userId,reserved.effectiveAt(),"DEMO_GENERAL_CANCER","a".repeat(64));
        assertThat(claims.findAssessmentContext(claim.claimId()).coverageEndAt()).isEqualTo(reserved.effectiveAt());
    }

    @Test
    @DisplayName("선택 보장만 결제·계약에 반영하며 보장 추가의 면책과 변경 요청 검증을 유지한다")
    void checksSelectedCheckoutAndCoverageChangeGuards() throws Exception {
        Long userId=userIds.get(0);
        var selected=quoteService.issue(userId,new CreateQuoteRequest(productVersionId,List.of(productCoverageIds.getFirst())));
        Long appId=applicationService.create(userId,new CreateApplicationRequest(selected.quoteId())).applicationId();
        applicationService.replaceDisclosures(userId,appId,new ReplaceDisclosuresRequest(false,false,false));
        applicationService.recordConsent(userId,appId,new CreateConsentRequest("PRODUCT_TERMS",TERMS_HASH,true));
        applicationService.recordConsent(userId,appId,new CreateConsentRequest("PRODUCT_EXPLANATION",TERMS_HASH,true));
        applicationService.submit(userId,appId,"selected-submit-"+appId);
        var order=paymentService.createInitialPremiumOrder(userId,appId,"selected-order-"+appId);
        assertThat(order.amount()).isEqualByComparingTo("19900.00");
        paymentService.confirm(userId,order.paymentOrderId(),"selected-confirm-"+appId,
                new com.capsule.insurance.payment.dto.ConfirmPaymentRequest("fake-paid-selected-"+appId,order.amount()));
        Long policyId=order.policyId();
        var policy=policyRepository.findOwned(policyId,userId).orElseThrow();
        assertThat(policy.policyVersion().coverages()).hasSize(1);
        assertThat(policy.policyVersion().snapshot().quote().monthlyPremium()).isEqualByComparingTo("19900.00");
        var changes=new com.capsule.insurance.policy.change.CoverageChangeService(jdbcTemplate,policyRepository,
                new JdbcQuoteRepository(jdbcTemplate,OBJECT_MAPPER),new com.capsule.insurance.premiumcollection.infra.JdbcPremiumCollectionRepository(jdbcTemplate),
                OBJECT_MAPPER,new DataSourceTransactionManager(dataSource));
        var quote=quoteService.issue(userId,new CreateQuoteRequest(productVersionId,productCoverageIds));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> changes.schedule(userId,policyId,"no-consent",
                new com.capsule.insurance.policy.change.CoverageChangeService.ChangeRequest(quote.quoteId(),TERMS_HASH,false)))
                .isInstanceOf(com.capsule.insurance.common.exception.BusinessException.class);
        jdbcTemplate.update("UPDATE ins_quote SET expires_at=NOW()-INTERVAL '1 minute' WHERE quote_id=?",quote.quoteId());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> changes.schedule(userId,policyId,"expired-change",
                new com.capsule.insurance.policy.change.CoverageChangeService.ChangeRequest(quote.quoteId(),TERMS_HASH,true)))
                .isInstanceOf(com.capsule.insurance.common.exception.BusinessException.class);
        var valid=quoteService.issue(userId,new CreateQuoteRequest(productVersionId,productCoverageIds));
        var reservation=changes.schedule(userId,policyId,"valid-change",
                new com.capsule.insurance.policy.change.CoverageChangeService.ChangeRequest(valid.quoteId(),TERMS_HASH,true));
        var futureRepository=new JdbcPolicyRepository(jdbcTemplate,OBJECT_MAPPER,Clock.fixed(reservation.effectiveAt(),java.time.ZoneOffset.UTC));
        var future=futureRepository.findOwned(policyId,userId).orElseThrow();
        assertThat(future.policyVersion().coverages().stream().filter(c -> c.productCoverageId().equals(productCoverageIds.get(1))).findFirst().orElseThrow().coverageStartAt())
                .isEqualTo(reservation.effectiveAt().plusSeconds(90*86400L));
        assertThat(future.policyVersion().coverages().stream().filter(c -> c.productCoverageId().equals(productCoverageIds.getFirst())).findFirst().orElseThrow().coverageStartAt())
                .isEqualTo(policy.policyVersion().coverages().getFirst().coverageStartAt());
        var extra=quoteService.issue(userId,new CreateQuoteRequest(productVersionId,productCoverageIds.subList(0,2)));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> changes.schedule(userId,policyId,"second-change",
                new com.capsule.insurance.policy.change.CoverageChangeService.ChangeRequest(extra.quoteId(),TERMS_HASH,true)))
                .isInstanceOf(com.capsule.insurance.common.exception.BusinessException.class);
        jdbcTemplate.update("UPDATE ins_policy_coverage SET paid_benefit_count=1 WHERE policy_coverage_id=?",policy.policyVersion().coverages().getFirst().policyCoverageId());
        var claims=new com.capsule.insurance.claim.infra.JdbcClaimRepository(jdbcTemplate,OBJECT_MAPPER);
        assertThat(claims.lockPaidBenefitCount(future.policyVersion().coverages().getFirst().policyCoverageId())).isEqualTo(1);
        var api=MockMvcBuilders.standaloneSetup(new com.capsule.insurance.policy.change.CoverageChangeController(changes))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        api.perform(get("/api/v1/policies/{policyId}/coverage-changes",policyId).principal(authentication(userIds.get(1))))
                .andExpect(status().isNotFound());
        api.perform(get("/api/v1/policies/{policyId}/coverage-changes",policyId))
                .andExpect(status().isUnauthorized());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"UNKNOWN", "FAILED"})
    @DisplayName("승인 완료 후 다른 ID로 늦게 도착한 알림은 계약과 결제를 되돌리지 않는다")
    void lateDifferentEventsCannotRegressPaidPayment(String lateStatus) throws Exception {
        Long userId = userIds.getFirst();
        String prefix = "reordered-" + lateStatus;
        JsonNode order = createOrder(userId, approveApplication(userId), prefix + "-order");
        long orderId = order.path("paymentOrderId").asLong();
        long policyId = order.path("policyId").asLong();
        String paymentKey = "fake-timeout-" + prefix;
        int callsBefore = paymentGateway.confirmationInvocationCount();
        paymentService.confirm(userId, orderId, prefix + "-confirm",
                new com.capsule.insurance.payment.dto.ConfirmPaymentRequest(paymentKey, new java.math.BigDecimal("29900")));
        for (String eventStatus : List.of("PAID", lateStatus)) {
            webhookMockMvc.perform(post("/api/v1/ops/webhooks/fake/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(webhookRequest(prefix + "-event-" + eventStatus, paymentKey, eventStatus)))
                    .andExpect(status().isOk());
        }
        assertThat(paymentStatus(orderId)).isEqualTo("PAID");
        assertThat(attemptStatus(orderId)).isEqualTo("PAID");
        assertThat(policyStatus(policyId)).isEqualTo("ACTIVE");
        assertThat(count("ins_policy_version", "policy_id", policyId)).isEqualTo(1);
        assertThat(policyActivationOutboxCount(policyId)).isEqualTo(1);
        assertThat(paymentGateway.confirmationInvocationCount()).isEqualTo(callsBefore + 1);
    }

    @Test
    @DisplayName("대사 중 먼저 읽은 UNKNOWN 응답이 나중에 돌아와도 먼저 반영된 승인 알림을 유지한다")
    void delayedInquiryCannotOverwriteNewerPaidWebhook() throws Exception {
        Long userId = userIds.getFirst();
        JsonNode order = createOrder(userId, approveApplication(userId), "stale-inquiry-order");
        long orderId = order.path("paymentOrderId").asLong();
        long policyId = order.path("policyId").asLong();
        String key = "fake-timeout-stale-inquiry";
        var captured = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        PremiumPaymentGateway gateway = new PremiumPaymentGateway() {
            public GatewayPaymentResult confirm(ConfirmCommand command) {
                return GatewayPaymentResult.unknown(command.providerPaymentKey(), "TEST_TIMEOUT");
            }
            public GatewayPaymentResult inquire(InquiryCommand command) {
                GatewayPaymentResult old = GatewayPaymentResult.unknown(command.providerPaymentKey(), "OLD_QUERY_RESULT");
                captured.countDown();
                try {
                    if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("query release timed out");
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                return old;
            }
        };
        var service = new PaymentService(new JdbcPaymentRepository(jdbcTemplate), policyRepository, gateway,
                new DataSourceTransactionManager(dataSource), OBJECT_MAPPER);
        service.confirm(userId, orderId, "stale-inquiry-confirm",
                new com.capsule.insurance.payment.dto.ConfirmPaymentRequest(key, new java.math.BigDecimal("29900")));
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(1)) {
            var pending = workers.submit(() -> service.reconcile(orderId));
            try {
                assertThat(captured.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                service.applyProviderNotification("FAKE", key, GatewayPaymentResult.paid(key, "NEWER-PAID"));
                release.countDown();
                assertThat(pending.get(5, java.util.concurrent.TimeUnit.SECONDS).status()).isEqualTo("PAID");
            } finally { release.countDown(); }
        }
        assertThat(paymentStatus(orderId)).isEqualTo("PAID");
        assertThat(attemptStatus(orderId)).isEqualTo("PAID");
        assertThat(policyStatus(policyId)).isEqualTo("ACTIVE");
        assertThat(policyActivationOutboxCount(policyId)).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 모순된 최종 결과는 임의로 덮어쓰지 않고 실패 inbox에 남긴다")
    void conflictingFinalOutcomeRemainsVisibleForReview() throws Exception {
        Long userId = userIds.getFirst();
        JsonNode order = createOrder(userId, approveApplication(userId), "conflicting-final-order");
        long orderId = order.path("paymentOrderId").asLong();
        long policyId = order.path("policyId").asLong();
        String key = "fake-timeout-conflicting-final";
        paymentService.confirm(userId, orderId, "conflicting-final-confirm",
                new com.capsule.insurance.payment.dto.ConfirmPaymentRequest(key, new java.math.BigDecimal("29900")));
        webhookMockMvc.perform(post("/api/v1/ops/webhooks/fake/payments").contentType(MediaType.APPLICATION_JSON)
                        .content(webhookRequest("conflicting-final-failed", key, "FAILED"))).andExpect(status().isOk());
        webhookMockMvc.perform(post("/api/v1/ops/webhooks/fake/payments").contentType(MediaType.APPLICATION_JSON)
                        .content(webhookRequest("conflicting-final-paid", key, "PAID")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));
        assertThat(paymentStatus(orderId)).isEqualTo("FAILED");
        assertThat(policyStatus(policyId)).isEqualTo("PENDING_INITIAL_PREMIUM");
        assertThat(count("ins_policy_version", "policy_id", policyId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT processing_status FROM pay_webhook_event WHERE provider_event_id=?",
                String.class, "conflicting-final-paid")).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Toss 알림 payload의 상태와 순서를 믿지 않고 기존 거래 조회 결과만 계약에 반영한다")
    void tossWebhookUsesAuthoritativeInquiryForReorderedPayloads() throws Exception {
        Long userId = userIds.getFirst();
        var inquiries = new java.util.concurrent.atomic.AtomicInteger();
        PremiumPaymentGateway gateway = new PremiumPaymentGateway() {
            public String providerCode() { return "TOSS"; }
            public GatewayPaymentResult confirm(ConfirmCommand command) {
                return GatewayPaymentResult.unknown(command.providerPaymentKey(), "TEST_TIMEOUT");
            }
            public GatewayPaymentResult inquire(InquiryCommand command) {
                inquiries.incrementAndGet();
                return GatewayPaymentResult.paid(command.providerPaymentKey(), "AUTHORITATIVE-PAID");
            }
        };
        var service = new PaymentService(new JdbcPaymentRepository(jdbcTemplate), policyRepository, gateway,
                new DataSourceTransactionManager(dataSource), OBJECT_MAPPER);
        var order = service.createInitialPremiumOrder(userId, approveApplication(userId), "toss-reordered-order");
        String key = "toss-reordered-key";
        service.confirm(userId, order.paymentOrderId(), "toss-reordered-confirm",
                new com.capsule.insurance.payment.dto.ConfirmPaymentRequest(key, order.orderNo(), order.amount()));
        var webhook = new com.capsule.insurance.payment.webhook.application.TossPaymentWebhookService(
                new JdbcPaymentWebhookRepository(jdbcTemplate), service, OBJECT_MAPPER, new DataSourceTransactionManager(dataSource));
        for (String payloadStatus : List.of("IN_PROGRESS", "DONE", "ABORTED")) {
            String payload = OBJECT_MAPPER.writeValueAsString(Map.of("eventType", "PAYMENT_STATUS_CHANGED",
                    "data", Map.of("paymentKey", key, "status", payloadStatus)));
            assertThat(webhook.receive("toss-reordered-" + payloadStatus, payload).paymentStatus()).isEqualTo("PAID");
        }
        assertThat(inquiries.get()).isEqualTo(1);
        assertThat(policyStatus(order.policyId())).isEqualTo("ACTIVE");
        assertThat(policyActivationOutboxCount(order.policyId())).isEqualTo(1);
    }

    @Test
    @DisplayName("PG 전송 전 용량 부족은 승인 시도를 만들지 않고 같은 키로 재시도할 수 있다")
    void localCapacityRejectionDoesNotStrandUnsentPayment() throws Exception {
        Long owner=userIds.getFirst();
        var remote=new FakePremiumPaymentGateway();
        var gateway=new JournaledPremiumPaymentGateway(remote,message->{},OBJECT_MAPPER,Clock.systemUTC(),3,Duration.ofSeconds(30),
                new com.capsule.insurance.payment.adapter.InMemoryPaymentCircuitStateStore(Clock.systemUTC()),1);
        var service=new PaymentService(new JdbcPaymentRepository(jdbcTemplate),policyRepository,gateway,new DataSourceTransactionManager(dataSource),OBJECT_MAPPER);
        var order=service.createInitialPremiumOrder(owner,approveApplication(owner),"capacity-unsent-order");
        var api=buildMockMvc(service,policyRepository);
        var held=gateway.tryReserveConfirmation();
        assertThat(held).isNotNull();
        try {
            api.perform(post("/api/v1/payments/{paymentOrderId}/confirm",order.paymentOrderId())
                            .principal(authentication(owner)).header("Idempotency-Key","capacity-unsent-confirm")
                            .contentType(MediaType.APPLICATION_JSON).content(confirmRequest("fake-paid-capacity-unsent","29900")))
                    .andExpect(status().isServiceUnavailable());
            assertThat(paymentStatus(order.paymentOrderId())).isEqualTo("CREATED");
            assertThat(policyStatus(order.policyId())).isEqualTo("PENDING_INITIAL_PREMIUM");
            assertThat(count("pay_attempt","payment_order_id",order.paymentOrderId())).isZero();
            assertThat(remote.confirmationInvocationCount()).isZero();
        } finally {held.close();}
        var paid=service.confirm(owner,order.paymentOrderId(),"capacity-unsent-confirm",
                new com.capsule.insurance.payment.dto.ConfirmPaymentRequest("fake-paid-capacity-unsent",order.amount()));
        assertThat(paid.status()).isEqualTo("PAID");
        assertThat(remote.confirmationInvocationCount()).isEqualTo(1);
        assertThat(count("pay_attempt","payment_order_id",order.paymentOrderId())).isEqualTo(1);
    }

    private static MockMvc buildMockMvc(
            PaymentService selectedPaymentService,
            PolicyRepository selectedPolicyRepository
    ) {
        return MockMvcBuilders
                .standaloneSetup(
                        new PaymentController(selectedPaymentService),
                        new PolicyController(new PolicyService(selectedPolicyRepository))
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static Long createDraftApplication(Long userId) {
        Long quoteId = quoteService.issue(
                userId,
                new CreateQuoteRequest(productVersionId, productCoverageIds)
        ).quoteId();
        return applicationService.create(userId, new CreateApplicationRequest(quoteId)).applicationId();
    }

    private static Long approveApplication(Long userId) {
        Long applicationId = createDraftApplication(userId);
        applicationService.replaceDisclosures(
                userId,
                applicationId,
                new ReplaceDisclosuresRequest(false, false, false)
        );
        applicationService.recordConsent(
                userId,
                applicationId,
                new CreateConsentRequest("PRODUCT_TERMS", TERMS_HASH, true)
        );
        applicationService.recordConsent(
                userId,
                applicationId,
                new CreateConsentRequest("PRODUCT_EXPLANATION", TERMS_HASH, true)
        );
        applicationService.submit(userId, applicationId, "approve-for-payment-" + applicationId);
        return applicationId;
    }

    private static JsonNode createOrder(Long userId, Long applicationId, String idempotencyKey)
            throws Exception {
        return createOrder(mockMvc, userId, applicationId, idempotencyKey);
    }

    private static JsonNode createOrder(
            MockMvc selectedMockMvc,
            Long userId,
            Long applicationId,
            String idempotencyKey
    ) throws Exception {
        MvcResult result = selectedMockMvc.perform(
                        post("/api/v1/applications/{applicationId}/payment-orders", applicationId)
                                .principal(authentication(userId))
                                .header("Idempotency-Key", idempotencyKey)
                )
                .andExpect(status().isOk())
                .andReturn();
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private static JsonNode confirm(
            Long userId,
            Long orderId,
            String idempotencyKey,
            String providerPaymentKey
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments/{paymentOrderId}/confirm", orderId)
                        .principal(authentication(userId))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest(providerPaymentKey, "29900.00")))
                .andExpect(status().isOk())
                .andReturn();
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private static String confirmRequest(String providerPaymentKey, String amount) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "providerPaymentKey", providerPaymentKey,
                "amount", amount
        ));
    }

    private static String webhookRequest(
            String providerEventId,
            String providerPaymentKey,
            String paymentStatus
    ) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "providerEventId", providerEventId,
                "providerPaymentKey", providerPaymentKey,
                "eventType", "PAYMENT_STATUS_CHANGED",
                "status", paymentStatus
        ));
    }

    private static int count(String table, String column, Object value) {
        String sql = "SELECT COUNT(*) FROM public." + table + " WHERE " + column + " = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, value);
    }

    private static int policyActivationOutboxCount(Long policyId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.ops_outbox_event
                WHERE aggregate_type = 'POLICY'
                  AND aggregate_id = ?
                  AND event_type = 'POLICY_ACTIVATED'
                """, Integer.class, policyId.toString());
    }

    private static String paymentStatus(Long orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM public.pay_order WHERE payment_order_id = ?",
                String.class,
                orderId
        );
    }

    private static String attemptStatus(Long orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM public.pay_attempt WHERE payment_order_id = ?",
                String.class,
                orderId
        );
    }

    private static String policyStatus(Long policyId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM public.ins_policy WHERE policy_id = ?",
                String.class,
                policyId
        );
    }

    private static UsernamePasswordAuthenticationToken authentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId.toString(), "", List.of());
    }

    private static Long insertUser(String email, String name, String phone) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.usr_user (email, name, phone, user_status)
                VALUES (?, ?, ?, 'ACTIVE')
                RETURNING user_id
                """, Long.class, email, name, phone);
    }

    private static void createLegacySchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
             InputStream input = new ClassPathResource("db/schema/schema.sql").getInputStream();
             Statement statement = connection.createStatement()) {
            statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static final class FailsOncePolicyRepository implements PolicyRepository {

        private final PolicyRepository delegate;
        private final AtomicBoolean shouldFail = new AtomicBoolean(true);

        private FailsOncePolicyRepository(PolicyRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public InsurancePolicy createPending(
                String policyNo,
                Long applicationId,
                Long policyholderUserId,
                Long insuredUserId,
                Long beneficiaryUserId
        ) {
            return delegate.createPending(
                    policyNo,
                    applicationId,
                    policyholderUserId,
                    insuredUserId,
                    beneficiaryUserId
            );
        }

        @Override
        public InsurancePolicy activateFromPaidOrder(Long paymentOrderId) {
            InsurancePolicy policy = delegate.activateFromPaidOrder(paymentOrderId);
            if (shouldFail.compareAndSet(true, false)) {
                throw new IllegalStateException("증권 발행 후 검증용 장애");
            }
            return policy;
        }

        @Override
        public java.util.Optional<InsurancePolicy> findOwned(Long policyId, Long userId) {
            return delegate.findOwned(policyId, userId);
        }

        @Override
        public java.util.Optional<InsurancePolicy> findById(Long policyId) {
            return delegate.findById(policyId);
        }
    }
}
