import { httpClient } from '@/common/api/httpClient';

export const getOperationsDashboard = async (recentLimit = 8) => {
  const response = await httpClient.get(
    `/api/v1/ops/dashboard?recentLimit=${encodeURIComponent(recentLimit)}`
  );
  const payload = response?.data;
  if (!payload?.success) {
    throw new Error(payload?.message || '운영 지표를 불러오지 못했습니다.');
  }
  return payload.data;
};

export const replayDeadLetter = async (eventId, reason) => {
  const response = await httpClient.post(
    `/api/v1/ops/outbox/${encodeURIComponent(eventId)}/replay`,
    { reason }
  );
  const payload = response?.data;
  if (!payload?.success) {
    throw new Error(payload?.message || 'DLQ 이벤트를 재처리하지 못했습니다.');
  }
  return payload.data;
};

export const runPaymentReconciliation = async (reason) => {
  const response = await httpClient.post('/api/v1/ops/jobs/payment-reconciliation', {
    reason,
    chunkSize: 100,
    staleAfterSeconds: 60,
  });
  const payload = response?.data;
  if (!payload?.success) {
    throw new Error(payload?.message || '결제 대사를 실행하지 못했습니다.');
  }
  return payload.data;
};

export const getPremiumCollectionTimeline = async (limit = 8) => {
  const response = await httpClient.get(`/api/v1/ops/premium-collections/timeline?limit=${encodeURIComponent(limit)}`);
  const payload = response?.data;
  if (!payload?.success) throw new Error(payload?.message || '보험료 수납 타임라인을 불러오지 못했습니다.');
  return payload.data;
};

const delinquencyPayload = (response) => {
  const payload = response?.data;
  if (!payload?.success) throw new Error(payload?.message || '미납 배치 요청을 처리하지 못했습니다.');
  return payload.data;
};

export const getDelinquencyRuns = async () => delinquencyPayload(
  await httpClient.get('/api/v1/ops/premium-collections/delinquency/runs')
);

export const runPremiumDelinquency = async (instanceKey, reason) => delinquencyPayload(
  await httpClient.post('/api/v1/ops/premium-collections/delinquency/runs', { instanceKey, reason })
);

export const resumePremiumDelinquency = async (runId, reason) => delinquencyPayload(
  await httpClient.post(`/api/v1/ops/premium-collections/delinquency/runs/${encodeURIComponent(runId)}/resume`, { reason })
);

const billingPayload = (response) => {
  const payload = response?.data;
  if (!payload?.success) throw new Error(payload?.message || '정기 보험료 채권 요청을 처리하지 못했습니다.');
  return payload.data;
};

export const getPremiumBillingRuns = async () => billingPayload(
  await httpClient.get('/api/v1/ops/premium-collections/billing/runs')
);

export const runPremiumBilling = async (instanceKey, billingCycle, reason) => billingPayload(
  await httpClient.post('/api/v1/ops/premium-collections/billing/runs', {
    instanceKey,
    billingCycle,
    reason,
  })
);

export const resumePremiumBilling = async (runId, reason) => billingPayload(
  await httpClient.post(`/api/v1/ops/premium-collections/billing/runs/${encodeURIComponent(runId)}/resume`, { reason })
);

const claimReviewPayload = (response, fallbackMessage) => {
  const payload = response?.data;
  if (!payload?.success) throw new Error(payload?.message || fallbackMessage);
  return payload.data;
};

export const getClaimCopilotReviewQueue = async (status = 'DRAFT', limit = 20) => {
  const query = new URLSearchParams({ status, limit: String(limit) });
  return claimReviewPayload(
    await httpClient.get(`/api/v1/ops/claims/review-copilot/reviews?${query.toString()}`),
  '심사 보조 대기열을 불러오지 못했습니다.'
  );
};

export const updateClaimCopilotReview = async (claimId, requestId, status) => claimReviewPayload(
  await httpClient.post(
    `/api/v1/ops/claims/${encodeURIComponent(claimId)}/review-copilot/drafts/${encodeURIComponent(requestId)}/review`,
    { status }
  ),
  '심사 보조 검토 상태를 저장하지 못했습니다.'
);

export const getClaimCopilotDraft = async (claimId, requestId) => claimReviewPayload(
  await httpClient.get(
    `/api/v1/ops/claims/${encodeURIComponent(claimId)}/review-copilot/drafts/${encodeURIComponent(requestId)}`
  ),
  '심사 보조 초안을 불러오지 못했습니다.'
);

export const getClaimCopilotReadiness = async () => claimReviewPayload(
  await httpClient.get('/api/v1/ops/claims/review-copilot/readiness'),
  '심사 보조 공급자 설정을 확인하지 못했습니다.'
);
