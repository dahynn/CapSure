package com.capsule.insurance.catalog.application.port;

import com.capsule.insurance.catalog.domain.Coverage;
import com.capsule.insurance.catalog.domain.ProductVersion;
import com.capsule.insurance.catalog.domain.TermsClause;
import com.capsule.insurance.catalog.domain.TermsDocument;
import java.util.List;
import java.util.Optional;

public interface CancerProductQueryRepository {

    List<ProductVersion> findOnSaleProducts();

    Optional<ProductVersion> findOnSaleProduct(Long productVersionId);

    List<Coverage> findCoverages(Long productVersionId);

    Optional<TermsDocument> findTermsDocument(Long productVersionId);

    List<TermsClause> findTermsClauses(Long termsDocumentId);

    Optional<TermsClause> findTermsClause(Long termsClauseId);
}
