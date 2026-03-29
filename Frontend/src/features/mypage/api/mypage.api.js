import { httpClient } from '@/common/api/httpClient';

/**
 * 가상(더미) 결제 내역 반환
 */
export const getPaymentHistory = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve([
                {
                    id: 'h1',
                    date: '2023. 10. 15',
                    status: '결제 완료',
                    totalAmount: 50000,
                    receiptNumber: 'RCP-20231015-001',
                    items: [
                        { name: '암 진단비 집중 (3만)', company: '한화생명', type: '질병 보험', amount: 30000 },
                        { name: '자전거/킥보드 배상 (2만)', company: 'DB손해보험', type: '생활 배상 책임', amount: 20000 }
                    ],
                    coverages: [
                        { label: '입원 일당', amount: '300만원' },
                        { label: '수술비 지원', amount: '900만원' },
                        { label: '사망', amount: '2000만원' },
                        { label: '진료비', amount: '500만원' },
                        { label: '자전거 수리 배상', amount: '100만원' }
                    ]
                },
                {
                    id: 'h2',
                    date: '2023. 09. 15',
                    status: '구독 만료',
                    totalAmount: 20000,
                    receiptNumber: 'RCP-20230915-081',
                    items: [
                        { name: '자전거/킥보드 배상 (2만)', company: 'DB손해보험', type: '생활 배상 책임', amount: 20000 }
                    ],
                    coverages: [
                        { label: '입원 일당', amount: '200만원' },
                        { label: '자전거 수리 배상', amount: '100만원' }
                    ]
                },
                {
                    id: 'h3',
                    date: '2023. 05. 11',
                    status: '구독 만료',
                    totalAmount: 30000,
                    receiptNumber: 'RCP-20230511-204',
                    items: [
                        { name: '댕댕이 의료비 70%', company: '메리츠화재', type: '펫 보험', amount: 30000 }
                    ],
                    coverages: [
                        { label: '입원 일당', amount: '300만원' },
                        { label: '수술비 지원', amount: '900만원' },
                        { label: '개물림 사고 처벌 벌금 지원', amount: '300만원' }
                    ]
                }
            ]);
        }, 500); // 0.5s network delay
    });
};

/**
 * 프로필 정보 조회
 */
export const getUserProfile = async () => {
    const response = await httpClient.get('/auth/profile');
    return response.data.data;
};

/**
 * 프로필 정보 수정
 */
export const updateUserProfile = async (updateData) => {
    const response = await httpClient.put('/auth/profile', updateData);
    return response.data.data;
};

/**
 * 캡슐(구독) 상세 정보 조회
 */
export const getCapsuleDetail = async (subscriptionId) => {
    const response = await httpClient.get(`/subscriptions/${subscriptionId}/detail`);
    return response.data.data;
};

export const getMyCapsules = async () => {
    const response = await httpClient.get('/subscriptions/me/capsules');
    return response.data.data ?? [];
};

export const getProductDetail = async (productSourceId) => {
    const response = await httpClient.get(`/insurers/products/${productSourceId}`);
    return response.data.data;
};

/**
 * 익월 예약 보험 조회
 */
export const getNextItems = async (subscriptionId) => {
    const response = await httpClient.get(`/subscriptions/${subscriptionId}/next-items`);
    return response.data.data;
};

/**
 * 익월 보험 예약 (추가)
 */
export const reserveNextItem = async (subscriptionId, capsuleProductId) => {
    const response = await httpClient.post(`/subscriptions/${subscriptionId}/next-items`, { capsuleProductId });
    return response.data.data;
};

/**
 * 익월 보험 예약 취소
 */
export const cancelNextItem = async (subscriptionId, subscriptionItemId) => {
    const response = await httpClient.delete(`/subscriptions/${subscriptionId}/next-items/${subscriptionItemId}`);
    return response.data;
};

/**
 * 익월 캡슐 변경 확정
 */
export const confirmNext = async (subscriptionId) => {
    const response = await httpClient.put(`/subscriptions/${subscriptionId}/confirm-next`, {});
    return response.data.data;
};

/**
 * 현재 결제 수단 조회
 */
export const getCurrentPaymentMethod = async () => {
    const response = await httpClient.get('/subscriptions/payment-methods/current');
    return response.data.data;
};

/**
 * 결제 수단 등록/변경
 */
export const registerPaymentMethod = async (paymentMethodData) => {
    const response = await httpClient.post('/subscriptions/payment-methods', paymentMethodData);
    return response.data.data;
};
