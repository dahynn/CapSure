package com.capsule.insurance.operations.catalog.infra;

import com.capsule.insurance.operations.catalog.application.port.ProductVersionApprovalRepository;
import com.capsule.insurance.operations.catalog.domain.ProductVersionApprovalEvent;
import com.capsule.insurance.operations.catalog.domain.ProductVersionRelease;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProductVersionApprovalRepository implements ProductVersionApprovalRepository {

    private static final String EVENT_SELECT = """
            SELECT approval_event_id,
                   product_version_id,
                   product_code_snapshot,
                   product_version_snapshot,
                   terms_document_id,
                   terms_version_snapshot,
                   decision,
                   previous_status,
                   resulting_status,
                   actor_user_id,
                   reason,
                   decided_at
            FROM public.ops_product_version_approval_event
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcProductVersionApprovalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ProductVersionRelease> findForUpdate(Long productVersionId) {
        return jdbcTemplate.query("""
                SELECT product.product_version_id,
                       product.product_code,
                       product.version AS product_version,
                       product.terms_document_id,
                       terms.document_version AS terms_version,
                       product.release_status
                FROM public.ins_product_version product
                JOIN public.ins_terms_document terms
                  ON terms.terms_document_id = product.terms_document_id
                WHERE product.product_version_id = ?
                FOR UPDATE OF product
                """, this::mapRelease, productVersionId).stream().findFirst();
    }

    @Override
    public boolean exists(Long productVersionId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.ins_product_version
                    WHERE product_version_id = ?
                )
                """, Boolean.class, productVersionId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public ProductVersionApprovalEvent saveDecision(
            ProductVersionRelease release,
            String decision,
            String resultingStatus,
            Long actorUserId,
            String reason,
            Instant decidedAt
    ) {
        int updated = jdbcTemplate.update("""
                UPDATE public.ins_product_version
                SET release_status = ?
                WHERE product_version_id = ?
                  AND release_status = 'PENDING_APPROVAL'
                """, resultingStatus, release.productVersionId());
        if (updated != 1) {
            throw new IllegalStateException("승인 대기 상품 버전의 상태를 변경하지 못했습니다.");
        }

        Long eventId = jdbcTemplate.queryForObject("""
                INSERT INTO public.ops_product_version_approval_event (
                    product_version_id,
                    product_code_snapshot,
                    product_version_snapshot,
                    terms_document_id,
                    terms_version_snapshot,
                    decision,
                    previous_status,
                    resulting_status,
                    actor_user_id,
                    reason,
                    decided_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'PENDING_APPROVAL', ?, ?, ?, ?)
                RETURNING approval_event_id
                """,
                Long.class,
                release.productVersionId(),
                release.productCode(),
                release.productVersion(),
                release.termsDocumentId(),
                release.termsVersion(),
                decision,
                resultingStatus,
                actorUserId,
                reason,
                Timestamp.from(decidedAt)
        );
        return findEvent(Objects.requireNonNull(eventId));
    }

    @Override
    public List<ProductVersionApprovalEvent> findHistory(Long productVersionId) {
        return jdbcTemplate.query(
                EVENT_SELECT + " WHERE product_version_id = ? ORDER BY approval_event_id",
                this::mapEvent,
                productVersionId
        );
    }

    private ProductVersionApprovalEvent findEvent(Long eventId) {
        return jdbcTemplate.query(
                EVENT_SELECT + " WHERE approval_event_id = ?",
                this::mapEvent,
                eventId
        ).stream().findFirst().orElseThrow();
    }

    private ProductVersionRelease mapRelease(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProductVersionRelease(
                resultSet.getLong("product_version_id"),
                resultSet.getString("product_code"),
                resultSet.getString("product_version"),
                resultSet.getLong("terms_document_id"),
                resultSet.getString("terms_version"),
                resultSet.getString("release_status")
        );
    }

    private ProductVersionApprovalEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProductVersionApprovalEvent(
                resultSet.getLong("approval_event_id"),
                resultSet.getLong("product_version_id"),
                resultSet.getString("product_code_snapshot"),
                resultSet.getString("product_version_snapshot"),
                resultSet.getLong("terms_document_id"),
                resultSet.getString("terms_version_snapshot"),
                resultSet.getString("decision"),
                resultSet.getString("previous_status"),
                resultSet.getString("resulting_status"),
                resultSet.getLong("actor_user_id"),
                resultSet.getString("reason"),
                resultSet.getTimestamp("decided_at").toInstant()
        );
    }
}

