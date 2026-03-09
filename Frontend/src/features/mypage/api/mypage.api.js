/**
 * 가상(더미) 보험 가입 이력 반환
 */
export const getInsuranceHistory = async () => {
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
