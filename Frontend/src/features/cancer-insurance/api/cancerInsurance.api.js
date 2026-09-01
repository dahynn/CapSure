import { httpClient } from '@/common/api/httpClient';

const unwrap = (response) => {
    const payload = response?.data;
    if (!payload?.success) {
        throw new Error(payload?.message || '요청을 처리하지 못했습니다.');
    }
    return payload.data;
};

export const getCancerProducts = async () => (
    unwrap(await httpClient.get('/api/v1/cancer-products'))
);

export const getCancerProduct = async (productVersionId) => (
    unwrap(await httpClient.get(`/api/v1/cancer-products/${productVersionId}`))
);

export const getCancerProductTerms = async (productVersionId) => (
    unwrap(await httpClient.get(`/api/v1/cancer-products/${productVersionId}/terms/summary`))
);

export const getTermsClause = async (termsClauseId) => (
    unwrap(await httpClient.get(`/api/v1/terms/clauses/${termsClauseId}`))
);

export const issueCancerQuote = async (productVersionId, selectedProductCoverageIds) => (
    unwrap(await httpClient.post('/api/v1/quotes', {
        productVersionId,
        selectedProductCoverageIds,
    }))
);

export const getCancerQuote = async (quoteId) => (
    unwrap(await httpClient.get(`/api/v1/quotes/${quoteId}`))
);

export const createCancerApplication = async (quoteId) => (
    unwrap(await httpClient.post('/api/v1/applications', { quoteId }))
);

export const replaceCancerDisclosures = async (applicationId, disclosures) => (
    unwrap(await httpClient.put(`/api/v1/applications/${applicationId}/disclosures`, disclosures))
);

export const recordCancerConsent = async (applicationId, consentType, documentHash) => (
    unwrap(await httpClient.post(`/api/v1/applications/${applicationId}/consents`, {
        consentType,
        documentHash,
        agreed: true,
    }))
);

export const submitCancerApplication = async (applicationId, idempotencyKey) => (
    unwrap(await httpClient.post(
        `/api/v1/applications/${applicationId}/submit`,
        undefined,
        { headers: { 'Idempotency-Key': idempotencyKey } },
    ))
);

export const getCancerApplication = async (applicationId) => (
    unwrap(await httpClient.get(`/api/v1/applications/${applicationId}`))
);

export const createInitialPremiumOrder = async (applicationId, idempotencyKey) => (
    unwrap(await httpClient.post(
        `/api/v1/applications/${applicationId}/payment-orders`,
        undefined,
        { headers: { 'Idempotency-Key': idempotencyKey } },
    ))
);

export const confirmInitialPremiumPayment = async (
    paymentOrderId,
    providerPaymentKey,
    amount,
    idempotencyKey,
) => (
    unwrap(await httpClient.post(
        `/api/v1/payments/${paymentOrderId}/confirm`,
        { providerPaymentKey, amount },
        { headers: { 'Idempotency-Key': idempotencyKey } },
    ))
);

export const getInitialPremiumPayment = async (paymentOrderId) => (
    unwrap(await httpClient.get(`/api/v1/payments/${paymentOrderId}`))
);

export const getCancerPolicy = async (policyId) => (
    unwrap(await httpClient.get(`/api/v1/policies/${policyId}`))
);
