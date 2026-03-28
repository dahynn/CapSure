// src/features/home/api/home.api.js
import { httpClient } from '@/common/api/httpClient';

/**
 * 더미 데이터를 제공하는 Mock API
 * 실제 백엔드 API가 준비되면 axios(/src/common/api/httpClient.js) 요청으로 교체할 예정
 */

// 유효한 일일 보험 내역 조회를 위한 Mock API
export const getValidDailyInsurances = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve([
                {
                    id: 1,
                    name: '참좋은 운전자 보험',
                    company: 'DB손해보험',
                    totalTime: 24 * 60, // in minutes (24 hours)
                    passedTime: 10 * 60, // 10 hours passed
                    guarantees: [
                        { item: '대인배상', amount: '5천만원' },
                        { item: '입원비', amount: '3천만원' },
                        { item: '골절진단비', amount: '1백만원' }
                    ]
                }
            ]);
        }, 500); // 0.5초 딜레이
    });
};

// 마이데이터 동의 여부 조회를 위한 Mock 상태
let mockMyDataAgreed = false;

// 사용자 마이데이터 동의 여부 조회 API
export const getMyDataStatus = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve(mockMyDataAgreed);
        }, 300); // 0.3초 딜레이
    });
};

// 마이데이터 동의하기 API
export const agreeMyData = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            mockMyDataAgreed = true; // 상태 변경
            resolve({ success: true, message: "마이데이터 동의 완료" });
        }, 500); // 0.5초 딜레이
    });
};

// 보장 수준 백분위 분석 데이터 API
export const getCoverageAnalysis = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({ name: '김캡슐', percentile: 18 });
        }, 300);
    });
};

// 캡슐 보험 구독 요약 데이터 API
export const getSubscriptionSummary = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                totalAmount: 80000,
                details: [
                    { name: '실손보험', company: '한화', percentage: 40 },
                    { name: '암보험', company: 'DB', percentage: 30 },
                    { name: '자동차보험', company: '삼성', percentage: 20 },
                    { name: '운전자보험', company: '미래에셋', percentage: 10 }
                ]
            });
        }, 400);
    });
};

// 누적 혜택 금액 반환 데이터 API
export const getBenefitTracker = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({ savedAmount: '???,???' }); // 금액이 아닌 물음표
        }, 300);
    });
};

// 내 보험 보관함 데이터 API (타사 보험 목록)
export const getMyInsurances = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve([
                { id: 1, name: '다이렉트 치아 보험', company: '삼성화재' },
                { id: 2, name: '무배당 수호천사 더나은 펫보험', company: '동양생명' },
                { id: 3, name: '다이렉트 해외여행보험', company: '현대해상' }
            ]);
        }, 500);
    });
};

export const getHomeDashboard = async () => {
    const response = await httpClient.get('/dashboard/home');
    const payload = response.data;
    if (!payload?.success) {
        throw new Error(payload?.message || '홈 대시보드 조회에 실패했습니다.');
    }
    return payload.data;
};

export const getCategoryRecommendations = async () => {
    const response = await httpClient.get('/insurers/products/category-recommendations');
    const payload = response.data;
    if (!payload?.success) {
        throw new Error(payload?.message || '카테고리 추천 조회에 실패했습니다.');
    }
    return payload.data ?? [];
};
