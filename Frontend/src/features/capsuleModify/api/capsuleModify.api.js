/**
 * 다음달 구독 변경 예약을 서버에 요청합니다.
 * @param {Object} payload 예약 정보 (선택된 셀 데이터와 목표 금액 등)
 */
export const submitCapsuleReservation = async (payload) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                success: true,
                message: '다음달 구독 변경 예약이 완료되었습니다.',
                reservationData: payload
            });
        }, 1500); // Network delay mock
    });
};

/**
 * 변경 예약된 다음달 구독 내역이 있는지 확인합니다.
 */
export const getNextMonthReservation = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            // Mock: 처음엔 예약된 내역이 없음(null)으로 반환
            // 프론트에서 예약 완료 후에는 이 mock 함수 대신 상태로 관리하거나 
            // localStorage 등을 써서 연동하는 방식을 택할 수 있음.
            resolve(null);
        }, 500);
    });
};
