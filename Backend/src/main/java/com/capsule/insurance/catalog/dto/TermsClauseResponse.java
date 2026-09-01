package com.capsule.insurance.catalog.dto;

public record TermsClauseResponse(
        Long termsClauseId,
        Long termsDocumentId,
        String documentCode,
        String documentVersion,
        String documentHash,
        boolean simulation,
        String clauseCode,
        String title,
        String content,
        Integer pageNo,
        int sortOrder
) {
}
