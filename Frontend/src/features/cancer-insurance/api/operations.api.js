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
