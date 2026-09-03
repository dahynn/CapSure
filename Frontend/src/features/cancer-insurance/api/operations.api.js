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
