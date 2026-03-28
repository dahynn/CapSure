export const CAPSURE_CATEGORY_OPTIONS = ['전체', '사망', '암', '뇌/심장', '실손', '수술', '기타'];

export const CAPSURE_CATEGORY_CODE_BY_LABEL = {
    사망: 'DEATH',
    암: 'CANCER',
    '뇌/심장': 'BRAIN_HEART',
    실손: 'ACTUAL_LOSS',
    수술: 'SURGERY',
    기타: 'ETC',
};

export const CAPSURE_CATEGORY_LABEL_BY_CODE = {
    DEATH: '사망',
    CANCER: '암',
    BRAIN_HEART: '뇌/심장',
    ACTUAL_LOSS: '실손',
    SURGERY: '수술',
    ETC: '기타',
    ACCIDENT: '상해',
    LIABILITY: '배상',
};

export const getCapsureCategoryLabel = (coverageCategoryCode) =>
    CAPSURE_CATEGORY_LABEL_BY_CODE[coverageCategoryCode] ?? '기타';
