// features/searchInsurance/api/searchInsurance.api.js

const categories = [
    {
        id: 'car',
        name: '자동차보험',
        icon: 'Car',
        badge: '할인최대\n3만원',
        bgColor: 'bg-orange-500',
        iconColor: 'text-white'
    },
    {
        id: 'overseas',
        name: '해외여행...',
        icon: 'Luggage',
        bgColor: 'bg-green-400',
        iconColor: 'text-white',
        iconBadge: 'Plane' // small plane icon at the bottom right
    },
    {
        id: 'search',
        name: '상품찾기',
        icon: 'Search',
        bgColor: 'bg-purple-100',
        iconColor: 'text-purple-600'
    },
    {
        id: 'savings',
        name: '저축보험',
        icon: 'PiggyBank',
        bgColor: 'bg-yellow-100',
        iconColor: 'text-yellow-600'
    },
    {
        id: 'pet',
        name: '펫보험비교',
        icon: 'PawPrint',
        bgColor: 'bg-orange-100',
        iconColor: 'text-yellow-500'
    },
    {
        id: 'autocare',
        name: '오토케어',
        icon: 'CarFront',
        bgColor: 'bg-blue-100',
        iconColor: 'text-blue-500',
        iconBadge: 'Heart'
    },
    {
        id: 'travelcare',
        name: '여행케어',
        icon: 'SuitcaseRoll',
        bgColor: 'bg-green-100',
        iconColor: 'text-green-500',
        iconBadge: 'Heart'
    },
    {
        id: 'driving',
        name: '내 운전성향',
        icon: 'Smile',
        badge2: '밸런스게임',
        bgColor: 'bg-yellow-100',
        iconColor: 'text-yellow-600'
    }
];

const products = [
    {
        id: 1,
        categoryId: 'car',
        title: '운전자보험',
        description: '예기치 못한 피해에도 든든하게 대비하기',
        icon: 'SteeringWheel',
        iconBg: 'bg-blue-50',
        iconColor: 'text-blue-400'
    },
    {
        id: 2,
        categoryId: 'health',
        title: '암보험',
        description: '오르지 않는 보험료, 100세까지 든든하게',
        icon: 'BriefcaseMedical',
        iconBg: 'bg-red-50',
        iconColor: 'text-red-400'
    },
    {
        id: 3,
        categoryId: 'health',
        title: '치아보험',
        description: '충치·임플란트 치료비 보장 받아요',
        icon: 'Tooth',
        iconBg: 'bg-purple-50',
        iconColor: 'text-purple-400'
    },
    {
        id: 4,
        categoryId: 'life',
        title: '주택화재보험',
        description: '누수 피해까지 보장가능한',
        icon: 'Flame',
        iconBg: 'bg-blue-50',
        iconColor: 'text-blue-400'
    },
    {
        id: 5,
        categoryId: 'savings',
        title: '연금저축보험',
        description: '노후대비 똑똑하게 미리 준비하기',
        icon: 'CreditCard',
        iconBg: 'bg-yellow-50',
        iconColor: 'text-yellow-500'
    }
];

export const getInsuranceCategories = async () => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve(categories);
        }, 300);
    });
};

export const getInsuranceProductsByCategory = async (categoryId) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            if (!categoryId || categoryId === 'all') {
                resolve(products);
            } else {
                resolve(products.filter(p => p.categoryId === categoryId));
            }
        }, 300);
    });
};

export const getInsuranceDetail = async (productId) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                id: productId,
                title: '3대 질병 (암/ 뇌/ 심장)',
                subtitle: '에 대해 보장받을 수 있습니다.',
                subscript: '(해당 특약 가입 시)',
                aiSummary: [
                    '유방/자궁/전립선암 등의 소액암도 일반암으로 보상',
                    '암진단비에서 보장받고, 한번 더 추가 보장받을 수 있는 부위별 암진단비 운영 (폐/위/비뇨기관/대장 부위의 암진단비)',
                    '뇌출혈/뇌경색/뇌졸중을 포함한 뇌혈관질환을 보장',
                    '급성심근경색증은 기본이고 협심증까지 보장'
                ],
                aiNote: '※ 유사암 : 기타피부암, 갑상선암, 제자리암, 경계성종양',
                coverages: [
                    {
                        icon: 'Stethoscope',
                        iconBg: 'bg-green-100',
                        iconColor: 'text-green-600',
                        title: '암관련보장',
                        mainText: '암진단비/유사암진단비',
                        subText: '다양한 부위별 암 진단비\n(추가 선택가입 가능)',
                        detailText: '갑상선암, 폐암, 위암 등'
                    },
                    {
                        icon: 'Brain',
                        iconBg: 'bg-blue-100',
                        iconColor: 'text-blue-600',
                        title: '뇌관련보장',
                        mainText: '뇌혈관질환진단비',
                        detailText: '뇌혈관질환, 뇌졸중, 뇌출혈'
                    },
                    {
                        icon: 'HeartPulse',
                        iconBg: 'bg-orange-100',
                        iconColor: 'text-orange-600',
                        title: '심장관련보장',
                        mainText: '허혈심장질환진단비',
                        detailText: '허혈심장질환, 급성심근경색'
                    }
                ]
            });
        }, 300);
    });
};
