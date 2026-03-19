/**
 * 선택한 보험(셀)의 환불/위약금 정보 및 취소 약관을 불러옵니다.
 * @param {Array<string>} insuranceIds 취소하려는 보험의 ID 배열
 */
export const getCancelTermsAndRefund = async (insuranceIds) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            // Mock Calculation based on item counts
            // In a real app, this would be calculated by the server per actual item
            const cancelItemsCount = insuranceIds.length || 1;
            const originalAmount = cancelItemsCount * 10000; // 1만 원 * 개수
            const penaltyAmount = cancelItemsCount * 3000;   // 위약금
            const refundAmount = originalAmount - penaltyAmount;

            resolve({
                originalAmount,
                penaltyAmount,
                refundAmount,
                termsList: [
                    {
                        id: 'term-cancel-1',
                        title: '보험계약 해지 및 위약금 관련 동의',
                        content: '1. 고객님께서 선택하신 보험계약 해지에 동의하십니까?\n\n2. 본 계약을 중도에 해지할 경우, 관련 법령 및 약관에 따라 미경과 보험료에서 부가보험료 등을 공제한 금액이 환급되므로, 납입하신 보험료보다 적거나 없을 수 있습니다.\n\n3. 단기 해지로 인한 해지 공제액(위약금)이 발생하며, 이는 남은 보장 기간과 갱신 시점에 따라 차등 적용됩니다.\n\n4. 해지 신청이 완료된 이후에는 이를 철회하거나 기존 계약으로 복구할 수 없습니다.'
                    },
                    {
                        id: 'term-cancel-2',
                        title: '보장 혜택 종료 동의',
                        content: '1. 해지 처리가 완료되는 즉시 해당 보험의 모든 보장 혜택은 종료됩니다.\n\n2. 해지일 이후에 발생한 사고나 질병에 대해서는 어떠한 명목으로도 보험금을 청구하거나 지급받을 수 없습니다.\n\n3. 패키지로 가입된 연계 연가입 할인이 적용된 경우, 일부 상품 해지 시 남은 유지 상품의 할인율이 변동되거나 요금이 인상될 수 있습니다.'
                    },
                    {
                        id: 'term-cancel-3',
                        title: '개인정보 파기 및 보관 동의',
                        content: '1. 해지된 보험계약에 관한 개인정보 및 신용정보는 관련 법규(신용정보의 이용 및 보호에 관한 법률 등)에 따라 거래 종료일로부터 최대 5년간 분쟁 처리 및 법적 의무 이행을 위해서만 제한적으로 보관됩니다.\n\n2. 보관 기간이 경과한 정보는 재생 불가능한 방법으로 안전하게 파기됩니다.\n\n3. 마이데이터 연동을 통해 수집된 보조 건강/금융 데이터는 본 계약 해지와 무관하게 별도 철회 절차 전까지 유지됩니다.'
                    }
                ]
            });
        }, 500); // Network delay mock
    });
};

/**
 * 취소 승인 요청 실행
 * @param {Array<string>} insuranceIds 취소 대상 ID 
 */
export const executeCancel = async (insuranceIds) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({ success: true, message: '취소 처리가 완료되었습니다.' });
        }, 1200);
    });
};
