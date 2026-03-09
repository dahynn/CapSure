// src/features/home/api/home.api.js

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
