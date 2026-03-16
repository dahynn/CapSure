1// src/features/capsule/api/capsuleInsurance.api.js

/**
 * 캡슐 조합(캡슐 보험)에서 사용하는 Mock API
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

            // Add coverages and terms text to each item
            const enhanced = filtered.map(item => {
                const coverages = [
                    { label: '입원 일당', amount: item.price * 100 + '만원' },
                    { label: '수술비 지원', amount: item.price * 300 + '만원' },
                ];
                if (item.categoryId === 'pet') {
                    coverages.push({ label: '반려동물 치료비', amount: '300만원' });
                } else if (item.categoryId === 'disease') {
                    coverages.push({ label: '진단비', amount: '2000만원' });
                }

                return {
                    ...item,
                    coverages,
                    termsText: `${item.name} 약관 상세 내용입니다.\n\n제1조(목적)\n이 약관은 보험계약자와 보험회사 간의 권리와 의무를 규정합니다.\n\n제2조(용어의 정의)\n1. "보험계약자"란 회사와 계약을 체결하고 보험료를 납입할 의무를 지는 사람을 말합니다.\n2. "피보험자"란 보험사고의 대상이 되는 사람을 말합니다.`
                };
            });

            resolve(enhanced);
        }, 400); // 0.4s delay Network Mocking
    });
};

/**
 * 보험 ID 배열을 받아서 각 보험별 보장 내용을 반환
 * @param {Array<string>} insuranceIds
 */
export const getInsuranceCoverages = async (insuranceIds) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            const coverages = insuranceIds.map(id => {
                const item = dummyCapsuleItems.find(i => i.id === id);
                if (!item) return null;

                // Mock dynamic coverages based on category/price
                const detail = [
                    { label: '입원 일당', amount: item.price * 100 + '만원' },
                    { label: '수술비 지원', amount: item.price * 300 + '만원' },
                ];
                if (item.categoryId === 'pet') {
                    detail.push({ label: '개물림 사고 처벌 벌금 지원', amount: '300만원' });
                } else if (item.categoryId === 'disease') {
                    detail.push({ label: '사망', amount: '2000만원' });
                    detail.push({ label: '진료비', amount: '500만원' });
                }

                return {
                    id: item.id,
                    title: `${item.name} - ${item.company}`,
                    details: detail
                };
            }).filter(Boolean);

            resolve(coverages);
        }, 300);
    });
};

/**
 * 보험 ID 배열을 받아서 각 보험별 약관 내용을 반환
 * @param {Array<string>} insuranceIds
 */
export const getInsuranceTerms = async (insuranceIds) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            const terms = insuranceIds.map(id => {
                const item = dummyCapsuleItems.find(i => i.id === id);
                if (!item) return null;

                return {
                    id: item.id,
                    title: `${item.name} - ${item.company}`,
                    termsList: [
                        { id: `${id}-t1`, title: '가입 동의서 및 주요 내용 설명서', content: '본 약관은 보험계약자와 보험회사 간의 권리와 의무를 규정합니다. 엄청 긴 내용의 약관이 여기에 포함되어 확인이 필요합니다. \n\n제1조 (목적)\n이 약관은 당사와 보험계약자 사이에 체결된 보험계약에 대하여 적용됩니다...\n제2조 (용어의 정의)\n이 약관에서 사용하는 용어의 정의는 다음과 같습니다...' },
                        { id: `${id}-t2`, title: '개인정보 수집 및 이용 동의', content: '당사는 보험계약의 체결 및 이행을 위하여 아래와 같이 개인정보를 수집 및 이용합니다. \n1. 수집항목: 성명, 주민등록번호, 연락처 등\n2. 수집목적: 보험계약 체결, 심사, 유지, 보험금 지급 등...\n3. 보유 및 이용기간: 거래 종료일로부터 5년' },
                        { id: `${id}-t3`, title: '민감정보 수집 및 이용 동의', content: '당사는 보험계약과 관련하여 질병, 상해 등에 관한 민감정보를 질병 위험도 평가, 보험금 지급 심사 목적으로 수집합니다. \n이에 동의하셔야 보험 계약 체결이 가능합니다.' },
                    ]
                };
            }).filter(Boolean);

            resolve(terms);
        }, 300);
    });
};

/**
 * 내(구독 중인) 캡슐 보험 정보 조회
 */
export const getMyCapsuleInsurance = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            // Mock response: User has subscribed to some initial dummy data
            resolve({
                targetAmount: 5,
                selectedCells: [
                    { category: { id: 'disease', name: '질병 보험', color: 'bg-rose-100 border-rose-300 text-rose-700' }, name: '암 진단비 집중 (3만)', company: '한화생명', groupId: 'g1' },
                    { category: { id: 'disease', name: '질병 보험', color: 'bg-rose-100 border-rose-300 text-rose-700' }, name: '암 진단비 집중 (3만)', company: '한화생명', groupId: 'g1' },
                    { category: { id: 'disease', name: '질병 보험', color: 'bg-rose-100 border-rose-300 text-rose-700' }, name: '암 진단비 집중 (3만)', company: '한화생명', groupId: 'g1' },
                    { category: { id: 'liability', name: '생활 배상 보험', color: 'bg-blue-100 border-blue-300 text-blue-700' }, name: '자전거/킥보드 배상 (2만)', company: 'DB손해보험', groupId: 'g2' },
                    { category: { id: 'liability', name: '생활 배상 보험', color: 'bg-blue-100 border-blue-300 text-blue-700' }, name: '자전거/킥보드 배상 (2만)', company: 'DB손해보험', groupId: 'g2' },
                ]
            });
        }, 300);
    });
};
