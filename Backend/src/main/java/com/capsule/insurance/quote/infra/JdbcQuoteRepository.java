package com.capsule.insurance.quote.infra;

import com.capsule.insurance.quote.application.port.QuoteRepository;
import com.capsule.insurance.quote.domain.InsuranceQuote;
import com.capsule.insurance.quote.domain.QuoteSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcQuoteRepository implements QuoteRepository {

    private static final String QUOTE_SELECT = """
            SELECT quote_id,
                   quote_no,
                   user_id,
                   product_version_id,
                   status,
                   monthly_premium,
                   currency_code,
                   snapshot_json::TEXT AS snapshot_json,
                   terms_document_hash,
                   expires_at,
                   used_at,
                   created_at
            FROM public.ins_quote
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcQuoteRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public InsuranceQuote save(
            String quoteNo,
            Long userId,
            Long productVersionId,
            BigDecimal monthlyPremium,
            String currencyCode,
            QuoteSnapshot snapshot,
            String termsDocumentHash,
            Instant expiresAt
    ) {
        Long quoteId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ins_quote (
                    quote_no,
                    user_id,
                    product_version_id,
                    status,
                    monthly_premium,
                    currency_code,
                    snapshot_json,
                    terms_document_hash,
                    expires_at
                ) VALUES (?, ?, ?, 'ISSUED', ?, ?, CAST(? AS JSONB), ?, ?)
                RETURNING quote_id
                """,
                Long.class,
                quoteNo,
                userId,
                productVersionId,
                monthlyPremium,
                currencyCode,
                toJson(snapshot),
                termsDocumentHash,
                java.sql.Timestamp.from(expiresAt)
        );
        return findOwned(Objects.requireNonNull(quoteId), userId).orElseThrow();
    }

    @Override
    public Optional<InsuranceQuote> findOwned(Long quoteId, Long userId) {
        return jdbcTemplate.query(
                QUOTE_SELECT + " WHERE quote_id = ? AND user_id = ?",
                this::mapQuote,
                quoteId,
                userId
        ).stream().findFirst();
    }

    @Override
    public void expireIfNeeded(Long quoteId, Instant now) {
        jdbcTemplate.update("""
                UPDATE public.ins_quote
                SET status = 'EXPIRED'
                WHERE quote_id = ?
                  AND status = 'ISSUED'
                  AND expires_at <= ?
                """, quoteId, java.sql.Timestamp.from(now));
    }

    private InsuranceQuote mapQuote(ResultSet resultSet, int rowNumber) throws SQLException {
        return new InsuranceQuote(
                resultSet.getLong("quote_id"),
                resultSet.getString("quote_no"),
                resultSet.getLong("user_id"),
                resultSet.getLong("product_version_id"),
                resultSet.getString("status"),
                resultSet.getBigDecimal("monthly_premium"),
                resultSet.getString("currency_code"),
                fromJson(resultSet.getString("snapshot_json")),
                resultSet.getString("terms_document_hash"),
                resultSet.getTimestamp("expires_at").toInstant(),
                toInstant(resultSet, "used_at"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private String toJson(QuoteSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("견적 snapshot을 직렬화하지 못했습니다.", exception);
        }
    }

    private QuoteSnapshot fromJson(String json) {
        try {
            return objectMapper.readValue(json, QuoteSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("견적 snapshot을 역직렬화하지 못했습니다.", exception);
        }
    }

    private Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
