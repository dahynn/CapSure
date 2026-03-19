/**
 * 캡슐 수정(변경 예약) Mock API
 */
export const submitCapsureReservation = async (reservationData) => {
    return new Promise((resolve) => {
        setTimeout(() => resolve({ success: true }), 1000);
    });
};
