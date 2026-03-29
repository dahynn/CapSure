import { httpClient } from '@/common/api/httpClient';

export const getMonthlyBilling = async () => {
    const response = await httpClient.get('/subscriptions/me/monthly-billing');
    return response.data.data;
};

export const getScheduleBilling = async () => {
    const response = await httpClient.get('/subscriptions/me/schedule-billing');
    return response.data.data;
};

export const getDiagnosisReport = async () => {
    const response = await httpClient.get('/analysis/diagnosis-report');
    return response.data.data;
};

export const getCoveragePercentile = async () => {
    const response = await httpClient.get('/analysis/coverage-percentile');
    return response.data.data;
};

export const getDashboardProductDetail = async (productSourceId) => {
    const response = await httpClient.get(`/insurers/products/${productSourceId}`);
    return response.data.data;
};
