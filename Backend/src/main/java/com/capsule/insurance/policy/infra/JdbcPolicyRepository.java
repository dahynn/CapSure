package com.capsule.insurance.policy.infra;

import com.capsule.insurance.policy.application.port.PolicyRepository;
import com.capsule.insurance.policy.domain.InsurancePolicy;
import com.capsule.insurance.quote.domain.QuoteSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPolicyRepository implements PolicyRepository {

    private static final String POLICY_SELECT = """
            SELECT policy_id,
                   policy_no,
                   application_id,
                   policyholder_user_id,
                   insured_user_id,
                   beneficiary_user_id,
                   status,
                   activated_at,
                   created_at,
                   updated_at
            FROM public.ins_policy
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcPolicyRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public InsurancePolicy createPending(
            String policyNo,
            Long applicationId,
            Long policyholderUserId,
            Long insuredUserId,
            Long beneficiaryUserId
    ) {
        Long policyId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_policy (
                    policy_no,
                    application_id,
                    policyholder_user_id,
                    insured_user_id,
                    beneficiary_user_id,
                    status
                ) VALUES (?, ?, ?, ?, ?, 'PENDING_INITIAL_PREMIUM')
                RETURNING policy_id
                """,
                Long.class,
                policyNo,
                applicationId,
                policyholderUserId,
                insuredUserId,
                beneficiaryUserId
        );
        return findById(Objects.requireNonNull(policyId)).orElseThrow();
    }

    @Override
    public InsurancePolicy activateFromPaidOrder(Long paymentOrderId) {
        ActivationContext context = jdbcTemplate.query("""
                SELECT payment_order.status AS payment_status,
                       policy.policy_id,
                       policy.status AS policy_status,
                       quote.product_version_id,
                       product.terms_document_id,
                       quote.snapshot_json::TEXT AS snapshot_json
                FROM public.pay_order payment_order
                JOIN public.ins_policy policy
                  ON policy.policy_id = payment_order.policy_id
                JOIN public.ins_application application
                  ON application.application_id = payment_order.application_id
                JOIN public.ins_quote quote
                  ON quote.quote_id = application.quote_id
                JOIN public.ins_product_version product
                  ON product.product_version_id = quote.product_version_id
                WHERE payment_order.payment_order_id = ?
                FOR UPDATE OF policy
                """, this::mapActivationContext, paymentOrderId).stream().findFirst().orElseThrow();

        if (!"PAID".equals(context.paymentStatus())) {
            throw new IllegalStateException("PAID 결제만 계약을 활성화할 수 있습니다.");
        }
        if ("ACTIVE".equals(context.policyStatus())) {
            return findById(context.policyId()).orElseThrow();
        }

        jdbcTemplate.update("""
                INSERT INTO public.ins_policy_version (
                    policy_id,
                    version,
                    product_version_id,
                    terms_document_id,
                    valid_from,
                    snapshot_json
                ) VALUES (?, 1, ?, ?, NOW(), CAST(? AS JSONB))
                ON CONFLICT (policy_id, version) DO NOTHING
                """,
                context.policyId(),
                context.productVersionId(),
                context.termsDocumentId(),
                toJson(context.snapshot())
        );
        PolicyVersionKey version = jdbcTemplate.query("""
                SELECT policy_version_id, valid_from
                FROM public.ins_policy_version
                WHERE policy_id = ?
                  AND version = 1
                """, (resultSet, rowNumber) -> new PolicyVersionKey(
                resultSet.getLong("policy_version_id"),
                resultSet.getTimestamp("valid_from").toInstant()
        ), context.policyId()).stream().findFirst().orElseThrow();

        for (QuoteSnapshot.CoverageSnapshot coverage : context.snapshot().coverages()) {
            Instant coverageStartAt = version.validFrom()
                    .plus(coverage.waitingPeriodDays(), ChronoUnit.DAYS);
            jdbcTemplate.update("""
                    INSERT INTO public.ins_policy_coverage (
                        policy_version_id,
                        product_coverage_id,
                        coverage_code_snapshot,
                        insured_amount,
                        currency_code,
                        coverage_start_at
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (policy_version_id, product_coverage_id) DO NOTHING
                    """,
                    version.policyVersionId(),
                    coverage.productCoverageId(),
                    coverage.coverageCode(),
                    coverage.insuredAmount(),
                    coverage.currencyCode(),
                    Timestamp.from(coverageStartAt)
            );
        }
        jdbcTemplate.update("""
                UPDATE public.ins_policy
                SET status = 'ACTIVE',
                    activated_at = NOW()
                WHERE policy_id = ?
                  AND status = 'PENDING_INITIAL_PREMIUM'
                """, context.policyId());

        jdbcTemplate.update("""
                INSERT INTO public.ops_outbox_event (
                    event_id,
                    aggregate_type,
                    aggregate_id,
                    event_type,
                    payload_json
                ) VALUES (?, 'POLICY', ?, 'POLICY_ACTIVATED', CAST(? AS JSONB))
                ON CONFLICT (event_id) DO NOTHING
                """,
                "POLICY-ACTIVATED-" + context.policyId(),
                context.policyId().toString(),
                toJson(Map.of(
                        "policyId", context.policyId(),
                        "paymentOrderId", paymentOrderId,
                        "policyVersion", 1
                ))
        );
        return findById(context.policyId()).orElseThrow();
    }

    @Override
    public Optional<InsurancePolicy> findOwned(Long policyId, Long userId) {
        return jdbcTemplate.query(
                POLICY_SELECT + " WHERE policy_id = ? AND policyholder_user_id = ?",
                this::mapPolicy,
                policyId,
                userId
        ).stream().findFirst().map(this::withLatestVersion);
    }

    @Override
    public Optional<InsurancePolicy> findById(Long policyId) {
        return jdbcTemplate.query(
                POLICY_SELECT + " WHERE policy_id = ?",
                this::mapPolicy,
                policyId
        ).stream().findFirst().map(this::withLatestVersion);
    }

    private InsurancePolicy withLatestVersion(InsurancePolicy policy) {
        InsurancePolicy.PolicyVersion version = jdbcTemplate.query("""
                SELECT policy_version_id,
                       version,
                       product_version_id,
                       terms_document_id,
                       valid_from,
                       valid_to,
                       snapshot_json::TEXT AS snapshot_json
                FROM public.ins_policy_version
                WHERE policy_id = ?
                ORDER BY version DESC
                LIMIT 1
                """, this::mapPolicyVersion, policy.policyId()).stream().findFirst().orElse(null);
        if (version != null) {
            version = new InsurancePolicy.PolicyVersion(
                    version.policyVersionId(),
                    version.version(),
                    version.productVersionId(),
                    version.termsDocumentId(),
                    version.validFrom(),
                    version.validTo(),
                    version.snapshot(),
                    findCoverages(version.policyVersionId())
            );
        }
        return new InsurancePolicy(
                policy.policyId(),
                policy.policyNo(),
                policy.applicationId(),
                policy.policyholderUserId(),
                policy.insuredUserId(),
                policy.beneficiaryUserId(),
                policy.status(),
                policy.activatedAt(),
                policy.createdAt(),
                policy.updatedAt(),
                version
        );
    }

    private List<InsurancePolicy.PolicyCoverage> findCoverages(Long policyVersionId) {
        return jdbcTemplate.query("""
                SELECT policy_coverage_id,
                       product_coverage_id,
                       coverage_code_snapshot,
                       insured_amount,
                       currency_code,
                       coverage_start_at,
                       coverage_end_at
                FROM public.ins_policy_coverage
                WHERE policy_version_id = ?
                ORDER BY policy_coverage_id
                """, this::mapPolicyCoverage, policyVersionId);
    }

    private InsurancePolicy mapPolicy(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InsurancePolicy(
                resultSet.getLong("policy_id"),
                resultSet.getString("policy_no"),
                resultSet.getLong("application_id"),
                resultSet.getLong("policyholder_user_id"),
                resultSet.getLong("insured_user_id"),
                resultSet.getLong("beneficiary_user_id"),
                resultSet.getString("status"),
                toInstant(resultSet, "activated_at"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                null
        );
    }

    private InsurancePolicy.PolicyVersion mapPolicyVersion(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InsurancePolicy.PolicyVersion(
                resultSet.getLong("policy_version_id"),
                resultSet.getInt("version"),
                resultSet.getLong("product_version_id"),
                resultSet.getLong("terms_document_id"),
                resultSet.getTimestamp("valid_from").toInstant(),
                toInstant(resultSet, "valid_to"),
                fromJson(resultSet.getString("snapshot_json")),
                List.of()
        );
    }

    private InsurancePolicy.PolicyCoverage mapPolicyCoverage(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new InsurancePolicy.PolicyCoverage(
                resultSet.getLong("policy_coverage_id"),
                resultSet.getLong("product_coverage_id"),
                resultSet.getString("coverage_code_snapshot"),
                resultSet.getBigDecimal("insured_amount"),
                resultSet.getString("currency_code"),
                resultSet.getTimestamp("coverage_start_at").toInstant(),
                toInstant(resultSet, "coverage_end_at")
        );
    }

    private ActivationContext mapActivationContext(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ActivationContext(
                resultSet.getString("payment_status"),
                resultSet.getLong("policy_id"),
                resultSet.getString("policy_status"),
                resultSet.getLong("product_version_id"),
                resultSet.getLong("terms_document_id"),
                fromJson(resultSet.getString("snapshot_json"))
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("계약 원장 JSON을 직렬화하지 못했습니다.", exception);
        }
    }

    private QuoteSnapshot fromJson(String json) {
        try {
            return objectMapper.readValue(json, QuoteSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("계약 snapshot JSON을 역직렬화하지 못했습니다.", exception);
        }
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record ActivationContext(
            String paymentStatus,
            Long policyId,
            String policyStatus,
            Long productVersionId,
            Long termsDocumentId,
            QuoteSnapshot snapshot
    ) {
    }

    private record PolicyVersionKey(Long policyVersionId, Instant validFrom) {
    }
}
