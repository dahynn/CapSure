/**
 * 캡슐 취소 Mock API
 */
export const getCancelTermsAndRefund = async (groupIds) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                originalAmount: 45000,
                penaltyAmount: 4500,
                refundAmount: 40500,
                termsList: [
                    { id: 'ct1', title: '취소 환불 규정 동의', content: '보험 해지 시 해지환급금 및 위약금 산정 방식에 대해 설명합니다...\n1. 취소 시점별 환불 비율...\n2. 위약금 공제 안내...' },
                    { id: 'ct2', title: '개인정보 처리 방침', content: '보험 해지 처리를 위한 개인정보 이용에 동의합니다...' }
                ]
            });
        }, 300);
    });
};

export const executeCancel = async (groupIds) => {
    return new Promise((resolve) => {
        setTimeout(() => resolve({ success: true }), 1000);
    });
};
