package com.capsule.insurance.catalog.infra;

import com.capsule.insurance.catalog.application.port.CancerProductQueryRepository;
import com.capsule.insurance.catalog.domain.Coverage;
import com.capsule.insurance.catalog.domain.ProductVersion;
import com.capsule.insurance.catalog.domain.TermsClause;
import com.capsule.insurance.catalog.domain.TermsDocument;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCancerProductQueryRepository implements CancerProductQueryRepository {

    private static final String PRODUCT_SELECT = """
            SELECT product.product_version_id,
                   product.product_code,
                   product.version,
                   product.product_name,
                   product.insurer_name,
                   product.insurer_sector,
                   product.sale_from,
                   product.sale_to,
                   product.status,
                   product.base_monthly_premium,
                   product.currency_code,
                   product.is_simulation,
                   COUNT(product_coverage.product_coverage_id) AS coverage_count,
                   terms.terms_document_id,
                   terms.document_version AS terms_version,
                   terms.source_hash AS terms_source_hash
            FROM public.ins_product_version product
            JOIN public.ins_terms_document terms
              ON terms.terms_document_id = product.terms_document_id
            LEFT JOIN public.ins_product_coverage product_coverage
              ON product_coverage.product_version_id = product.product_version_id
            WHERE product.status = 'ON_SALE'
              AND product.sale_from <= CURRENT_DATE
              AND (product.sale_to IS NULL OR product.sale_to >= CURRENT_DATE)
            """;

    private static final String PRODUCT_GROUP_BY = """
            GROUP BY product.product_version_id,
                     terms.terms_document_id,
                     terms.document_version,
                     terms.source_hash
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcCancerProductQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ProductVersion> findOnSaleProducts() {
        String sql = PRODUCT_SELECT + PRODUCT_GROUP_BY + " ORDER BY product.product_version_id";
        return jdbcTemplate.query(sql, this::mapProductVersion);
    }

    @Override
    public Optional<ProductVersion> findOnSaleProduct(Long productVersionId) {
        String sql = PRODUCT_SELECT
                + " AND product.product_version_id = ? "
                + PRODUCT_GROUP_BY;
        return jdbcTemplate.query(sql, this::mapProductVersion, productVersionId).stream().findFirst();
    }

    @Override
    public List<Coverage> findCoverages(Long productVersionId) {
        return jdbcTemplate.query("""
                SELECT product_coverage.product_coverage_id,
                       coverage.coverage_code,
                       coverage.coverage_name,
                       coverage.coverage_category,
                       coverage.benefit_type,
                       coverage.description,
                       product_coverage.insured_amount,
                       product_coverage.currency_code,
                       product_coverage.waiting_period_days,
                       product_coverage.reduction_period_days,
                       product_coverage.reduction_rate,
                       product_coverage.coverage_start_rule,
                       product_coverage.display_order,
                       rule_clause.terms_clause_id,
                       rule_clause.clause_code AS terms_clause_code
                FROM public.ins_product_coverage product_coverage
                JOIN public.ins_coverage coverage
                  ON coverage.coverage_id = product_coverage.coverage_id
                LEFT JOIN LATERAL (
                    SELECT terms_clause.terms_clause_id,
                           terms_clause.clause_code
                    FROM public.ins_coverage_rule coverage_rule
                    JOIN public.ins_terms_clause terms_clause
                      ON terms_clause.terms_clause_id = coverage_rule.terms_clause_id
                    WHERE coverage_rule.product_coverage_id = product_coverage.product_coverage_id
                      AND coverage_rule.is_active = TRUE
                    ORDER BY coverage_rule.priority, coverage_rule.coverage_rule_id
                    LIMIT 1
                ) rule_clause ON TRUE
                WHERE product_coverage.product_version_id = ?
                ORDER BY product_coverage.display_order
                """, this::mapCoverage, productVersionId);
    }

    @Override
    public Optional<TermsDocument> findTermsDocument(Long productVersionId) {
        return jdbcTemplate.query("""
                SELECT terms.terms_document_id,
                       terms.document_code,
                       terms.document_version,
                       terms.title,
                       terms.source_type,
                       terms.source_uri,
                       terms.source_hash,
                       terms.effective_from,
                       terms.effective_to,
                       terms.status,
                       terms.is_simulation
                FROM public.ins_product_version product
                JOIN public.ins_terms_document terms
                  ON terms.terms_document_id = product.terms_document_id
                WHERE product.product_version_id = ?
                """, this::mapTermsDocument, productVersionId).stream().findFirst();
    }

    @Override
    public List<TermsClause> findTermsClauses(Long termsDocumentId) {
        return jdbcTemplate.query("""
                SELECT clause.terms_clause_id,
                       terms.terms_document_id,
                       terms.document_code,
                       terms.document_version,
                       terms.source_hash,
                       terms.is_simulation,
                       clause.clause_code,
                       clause.title,
                       clause.body,
                       clause.page_no,
                       clause.sort_order
                FROM public.ins_terms_clause clause
                JOIN public.ins_terms_document terms
                  ON terms.terms_document_id = clause.terms_document_id
                WHERE terms.terms_document_id = ?
                ORDER BY clause.sort_order
                """, this::mapTermsClause, termsDocumentId);
    }

    @Override
    public Optional<TermsClause> findTermsClause(Long termsClauseId) {
        return jdbcTemplate.query("""
                SELECT clause.terms_clause_id,
                       terms.terms_document_id,
                       terms.document_code,
                       terms.document_version,
                       terms.source_hash,
                       terms.is_simulation,
                       clause.clause_code,
                       clause.title,
                       clause.body,
                       clause.page_no,
                       clause.sort_order
                FROM public.ins_terms_clause clause
                JOIN public.ins_terms_document terms
                  ON terms.terms_document_id = clause.terms_document_id
                WHERE clause.terms_clause_id = ?
                """, this::mapTermsClause, termsClauseId).stream().findFirst();
    }

    private ProductVersion mapProductVersion(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProductVersion(
                resultSet.getLong("product_version_id"),
                resultSet.getString("product_code"),
                resultSet.getString("version"),
                resultSet.getString("product_name"),
                resultSet.getString("insurer_name"),
                resultSet.getString("insurer_sector"),
                resultSet.getObject("sale_from", java.time.LocalDate.class),
                resultSet.getObject("sale_to", java.time.LocalDate.class),
                resultSet.getString("status"),
                resultSet.getBigDecimal("base_monthly_premium"),
                resultSet.getString("currency_code"),
                resultSet.getBoolean("is_simulation"),
                resultSet.getLong("coverage_count"),
                resultSet.getLong("terms_document_id"),
                resultSet.getString("terms_version"),
                resultSet.getString("terms_source_hash")
        );
    }

    private Coverage mapCoverage(ResultSet resultSet, int rowNumber) throws SQLException {
        Long termsClauseId = resultSet.getObject("terms_clause_id", Long.class);
        return new Coverage(
                resultSet.getLong("product_coverage_id"),
                resultSet.getString("coverage_code"),
                resultSet.getString("coverage_name"),
                resultSet.getString("coverage_category"),
                resultSet.getString("benefit_type"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("insured_amount"),
                resultSet.getString("currency_code"),
                resultSet.getInt("waiting_period_days"),
                resultSet.getInt("reduction_period_days"),
                resultSet.getBigDecimal("reduction_rate"),
                resultSet.getString("coverage_start_rule"),
                resultSet.getInt("display_order"),
                termsClauseId,
                resultSet.getString("terms_clause_code")
        );
    }

    private TermsDocument mapTermsDocument(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TermsDocument(
                resultSet.getLong("terms_document_id"),
                resultSet.getString("document_code"),
                resultSet.getString("document_version"),
                resultSet.getString("title"),
                resultSet.getString("source_type"),
                resultSet.getString("source_uri"),
                resultSet.getString("source_hash"),
                resultSet.getObject("effective_from", java.time.LocalDate.class),
                resultSet.getObject("effective_to", java.time.LocalDate.class),
                resultSet.getString("status"),
                resultSet.getBoolean("is_simulation")
        );
    }

    private TermsClause mapTermsClause(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TermsClause(
                resultSet.getLong("terms_clause_id"),
                resultSet.getLong("terms_document_id"),
                resultSet.getString("document_code"),
                resultSet.getString("document_version"),
                resultSet.getString("source_hash"),
                resultSet.getBoolean("is_simulation"),
                resultSet.getString("clause_code"),
                resultSet.getString("title"),
                resultSet.getString("body"),
                resultSet.getObject("page_no", Integer.class),
                resultSet.getInt("sort_order")
        );
    }
}
