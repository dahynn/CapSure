package com.capsule.insurance.application.infra;

import com.capsule.insurance.application.application.port.ApplicationRepository;
import com.capsule.insurance.application.domain.ApplicationConsent;
import com.capsule.insurance.application.domain.ApplicationQuote;
import com.capsule.insurance.application.domain.DisclosureAnswers;
import com.capsule.insurance.application.domain.InsuranceApplication;
import com.capsule.insurance.application.domain.UnderwritingDecision;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcApplicationRepository implements ApplicationRepository {

    private static final String APPLICATION_SELECT = """
            SELECT application.application_id,
                   application.application_no,
                   application.quote_id,
                   application.applicant_user_id,
                   application.insured_user_id,
                   application.status,
                   application.disclosure_json::TEXT AS disclosure_json,
                   application.submission_idempotency_key,
                   product.terms_document_id,
                   quote.terms_document_hash,
                   application.submitted_at,
                   application.created_at,
                   application.updated_at
            FROM public.ins_application application
            JOIN public.ins_quote quote
              ON quote.quote_id = application.quote_id
            JOIN public.ins_product_version product
              ON product.product_version_id = quote.product_version_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcApplicationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ApplicationQuote> lockOwnedQuote(Long quoteId, Long userId) {
        return jdbcTemplate.query("""
                SELECT quote.quote_id,
                       quote.user_id,
                       quote.status,
                       quote.expires_at,
                       product.terms_document_id,
                       quote.terms_document_hash
                FROM public.ins_quote quote
                JOIN public.ins_product_version product
                  ON product.product_version_id = quote.product_version_id
                WHERE quote.quote_id = ?
                  AND quote.user_id = ?
                FOR UPDATE OF quote
                """, this::mapApplicationQuote, quoteId, userId).stream().findFirst();
    }

    @Override
    public Optional<InsuranceApplication> findByQuote(Long quoteId) {
        return jdbcTemplate.query(
                APPLICATION_SELECT + " WHERE application.quote_id = ?",
                this::mapApplication,
                quoteId
        ).stream().findFirst();
    }

    @Override
    public InsuranceApplication createDraft(String applicationNo, Long quoteId, Long userId) {
        Long applicationId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_application (
                    application_no,
                    quote_id,
                    applicant_user_id,
                    insured_user_id,
                    status,
                    disclosure_json
                ) VALUES (?, ?, ?, ?, 'DRAFT', '{}'::JSONB)
                RETURNING application_id
                """, Long.class, applicationNo, quoteId, userId, userId);
        return findById(Objects.requireNonNull(applicationId)).orElseThrow();
    }

    @Override
    public void markQuoteUsed(Long quoteId) {
        jdbcTemplate.update("""
                UPDATE public.ins_quote
                SET status = 'USED',
                    used_at = NOW()
                WHERE quote_id = ?
                  AND status = 'ISSUED'
                """, quoteId);
    }

    @Override
    public void expireQuote(Long quoteId) {
        jdbcTemplate.update("""
                UPDATE public.ins_quote
                SET status = 'EXPIRED'
                WHERE quote_id = ?
                  AND status = 'ISSUED'
                """, quoteId);
    }

    @Override
    public Optional<InsuranceApplication> findOwned(Long applicationId, Long userId) {
        return jdbcTemplate.query(
                APPLICATION_SELECT + """
                        WHERE application.application_id = ?
                          AND application.applicant_user_id = ?
                        """,
                this::mapApplication,
                applicationId,
                userId
        ).stream().findFirst();
    }

    @Override
    public Optional<InsuranceApplication> lockOwned(Long applicationId, Long userId) {
        return jdbcTemplate.query(
                APPLICATION_SELECT + """
                        WHERE application.application_id = ?
                          AND application.applicant_user_id = ?
                        FOR UPDATE OF application
                        """,
                this::mapApplication,
                applicationId,
                userId
        ).stream().findFirst();
    }

    @Override
    public InsuranceApplication replaceDisclosure(Long applicationId, DisclosureAnswers answers) {
        jdbcTemplate.update("""
                UPDATE public.ins_application
                SET disclosure_json = CAST(? AS JSONB),
                    status = 'DISCLOSURE_COMPLETED'
                WHERE application_id = ?
                  AND status IN ('DRAFT', 'DISCLOSURE_COMPLETED')
                """, toJson(answers), applicationId);
        return findById(applicationId).orElseThrow();
    }

    @Override
    public Optional<ApplicationConsent> findConsent(Long applicationId, String consentType) {
        return jdbcTemplate.query("""
                SELECT consent_id,
                       application_id,
                       consent_type,
                       terms_document_id,
                       document_hash,
                       is_required,
                       agreed,
                       actor_user_id,
                       agreed_at,
                       request_id,
                       created_at
                FROM public.ins_consent
                WHERE application_id = ?
                  AND consent_type = ?
                """, this::mapConsent, applicationId, consentType).stream().findFirst();
    }

    @Override
    public List<ApplicationConsent> findConsents(Long applicationId) {
        return jdbcTemplate.query("""
                SELECT consent_id,
                       application_id,
                       consent_type,
                       terms_document_id,
                       document_hash,
                       is_required,
                       agreed,
                       actor_user_id,
                       agreed_at,
                       request_id,
                       created_at
                FROM public.ins_consent
                WHERE application_id = ?
                ORDER BY consent_type
                """, this::mapConsent, applicationId);
    }

    @Override
    public ApplicationConsent saveConsent(
            Long applicationId,
            String consentType,
            Long termsDocumentId,
            String documentHash,
            boolean required,
            boolean agreed,
            Long actorUserId,
            String requestId
    ) {
        Long consentId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_consent (
                    application_id,
                    consent_type,
                    terms_document_id,
                    document_hash,
                    is_required,
                    agreed,
                    actor_user_id,
                    agreed_at,
                    request_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CASE WHEN ? THEN NOW() ELSE NULL END, ?)
                RETURNING consent_id
                """,
                Long.class,
                applicationId,
                consentType,
                termsDocumentId,
                documentHash,
                required,
                agreed,
                actorUserId,
                agreed,
                requestId
        );
        return findConsentById(Objects.requireNonNull(consentId)).orElseThrow();
    }

    @Override
    public Optional<UnderwritingDecision> findLatestDecision(Long applicationId) {
        return jdbcTemplate.query("""
                SELECT underwriting_decision_id,
                       application_id,
                       decision_version,
                       decision,
                       rule_version,
                       reason_codes_json::TEXT AS reason_codes_json,
                       input_hash,
                       decided_at
                FROM public.ins_uw_decision
                WHERE application_id = ?
                ORDER BY decision_version DESC
                LIMIT 1
                """, this::mapDecision, applicationId).stream().findFirst();
    }

    @Override
    public InsuranceApplication saveDecision(
            Long applicationId,
            String idempotencyKey,
            String decision,
            String ruleVersion,
            List<String> reasonCodes,
            String inputHash
    ) {
        Integer nextVersion = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(decision_version), 0) + 1
                FROM public.ins_uw_decision
                WHERE application_id = ?
                """, Integer.class, applicationId);
        jdbcTemplate.update("""
                INSERT INTO public.ins_uw_decision (
                    application_id,
                    decision_version,
                    decision,
                    rule_version,
                    reason_codes_json,
                    input_hash
                ) VALUES (?, ?, ?, ?, CAST(? AS JSONB), ?)
                """, applicationId, nextVersion, decision, ruleVersion, toJson(reasonCodes), inputHash);
        jdbcTemplate.update("""
                UPDATE public.ins_application
                SET status = ?,
                    submission_idempotency_key = ?,
                    submitted_at = NOW()
                WHERE application_id = ?
                """, decision, idempotencyKey, applicationId);
        return findById(applicationId).orElseThrow();
    }

    private Optional<InsuranceApplication> findById(Long applicationId) {
        return jdbcTemplate.query(
                APPLICATION_SELECT + " WHERE application.application_id = ?",
                this::mapApplication,
                applicationId
        ).stream().findFirst();
    }

    private Optional<ApplicationConsent> findConsentById(Long consentId) {
        return jdbcTemplate.query("""
                SELECT consent_id,
                       application_id,
                       consent_type,
                       terms_document_id,
                       document_hash,
                       is_required,
                       agreed,
                       actor_user_id,
                       agreed_at,
                       request_id,
                       created_at
                FROM public.ins_consent
                WHERE consent_id = ?
                """, this::mapConsent, consentId).stream().findFirst();
    }

    private ApplicationQuote mapApplicationQuote(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ApplicationQuote(
                resultSet.getLong("quote_id"),
                resultSet.getLong("user_id"),
                resultSet.getString("status"),
                resultSet.getTimestamp("expires_at").toInstant(),
                resultSet.getLong("terms_document_id"),
                resultSet.getString("terms_document_hash")
        );
    }

    private InsuranceApplication mapApplication(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InsuranceApplication(
                resultSet.getLong("application_id"),
                resultSet.getString("application_no"),
                resultSet.getLong("quote_id"),
                resultSet.getLong("applicant_user_id"),
                resultSet.getLong("insured_user_id"),
                resultSet.getString("status"),
                fromJson(resultSet.getString("disclosure_json"), DisclosureAnswers.class),
                resultSet.getString("submission_idempotency_key"),
                resultSet.getLong("terms_document_id"),
                resultSet.getString("terms_document_hash"),
                toInstant(resultSet, "submitted_at"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private ApplicationConsent mapConsent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ApplicationConsent(
                resultSet.getLong("consent_id"),
                resultSet.getLong("application_id"),
                resultSet.getString("consent_type"),
                resultSet.getLong("terms_document_id"),
                resultSet.getString("document_hash"),
                resultSet.getBoolean("is_required"),
                resultSet.getBoolean("agreed"),
                resultSet.getLong("actor_user_id"),
                toInstant(resultSet, "agreed_at"),
                resultSet.getString("request_id"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private UnderwritingDecision mapDecision(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UnderwritingDecision(
                resultSet.getLong("underwriting_decision_id"),
                resultSet.getLong("application_id"),
                resultSet.getInt("decision_version"),
                resultSet.getString("decision"),
                resultSet.getString("rule_version"),
                fromJson(resultSet.getString("reason_codes_json"), new TypeReference<>() {
                }),
                resultSet.getString("input_hash"),
                resultSet.getTimestamp("decided_at").toInstant()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("청약 원장 JSON을 직렬화하지 못했습니다.", exception);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("청약 원장 JSON을 역직렬화하지 못했습니다.", exception);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("청약 원장 JSON을 역직렬화하지 못했습니다.", exception);
        }
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
