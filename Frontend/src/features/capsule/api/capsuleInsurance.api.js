1// src/features/capsule/api/capsuleInsurance.api.js

/**
 * 캡슐 조합(블록 보험)에서 사용하는 Mock API
 */

const dummyCapsuleItems = [
    // 실손 보험 (shilson)
    { id: 's1', categoryId: 'shilson', name: '기본형 실손의료비', company: '삼성화재', price: 1 },
    { id: 's2', categoryId: 'shilson', name: '종합형 실손의료비', company: '현대해상', price: 3 },
    { id: 's3', categoryId: 'shilson', name: '프리미엄 실손 보장', company: '메리츠화재', price: 5 },
    { id: 's4', categoryId: 'shilson', name: '다이렉트 실손', company: 'DB손해보험', price: 2 },

    // 질병 보험 (disease)
    { id: 'd1', categoryId: 'disease', name: '암 진단비 집중', company: '한화생명', price: 3 },
    { id: 'd2', categoryId: 'disease', name: '3대 질병 든든보장', company: '교보생명', price: 5 },
    { id: 'd3', categoryId: 'disease', name: '경증 질환 커버', company: '신한라이프', price: 1 },
    { id: 'd4', categoryId: 'disease', name: '수술비 전용 보험', company: '흥국생명', price: 4 },

    // 생활 배상 보험 (liability)
    { id: 'l1', categoryId: 'liability', name: '가족 일상생활중 배상', company: 'KB손해보험', price: 1 },
    { id: 'l2', categoryId: 'liability', name: '자전거/킥보드 배상', company: 'DB손해보험', price: 2 },
    { id: 'l3', categoryId: 'liability', name: '주택 화재 배상 종합', company: '삼성화재', price: 4 },

    // 펫 보험 (pet)
    { id: 'p1', categoryId: 'pet', name: '댕댕이 의료비 70%', company: '메리츠화재', price: 3 },
    { id: 'p2', categoryId: 'pet', name: '냥냥이 의료비 50%', company: 'DB손해보험', price: 2 },
    { id: 'p3', categoryId: 'pet', name: '반려견 배상책임 전용', company: '현대해상', price: 1 },
    { id: 'p4', categoryId: 'pet', name: '프리미엄 펫 케어', company: '삼성화재', price: 5 },

    // 상시 운전자 보험 (driver)
    { id: 'dr1', categoryId: 'driver', name: '초보 운전자 안심', company: '한화손해보험', price: 3 },
    { id: 'dr2', categoryId: 'driver', name: '핵심 보장 운전자', company: 'DB손해보험', price: 1 },
    { id: 'dr3', categoryId: 'driver', name: '주말 운전자 전용', company: 'KB손해보험', price: 2 },
    { id: 'dr4', categoryId: 'driver', name: 'VIP 운전자 종합', company: '삼성화재', price: 5 },
];

/**
 * 카테고리와 필터 조건에 맞는 아이템 목록 반환
 * @param {string} categoryId - 카테고리 ID 문자열
 * @param {number|null} maxPrice - 상한 금액 (단위: 만원), null이면 전채 금액
 * @returns {Promise<Array>} 아이템 객체 배열
 */
export const getCapsuleItems = async (categoryId, maxPrice = null) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            let filtered = dummyCapsuleItems.filter(item => item.categoryId === categoryId);

            if (maxPrice !== null) {
                filtered = filtered.filter(item => item.price <= maxPrice);
            }

            resolve(filtered);
        }, 400); // 0.4s delay Network Mocking
    });
};
