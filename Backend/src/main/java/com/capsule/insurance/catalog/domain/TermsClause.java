package com.capsule.insurance.catalog.domain;

public record TermsClause(
        Long termsClauseId,
        Long termsDocumentId,
        String documentCode,
        String documentVersion,
        String documentHash,
        boolean simulation,
        String clauseCode,
        String title,
        String body,
        Integer pageNo,
        int sortOrder
) {
}
