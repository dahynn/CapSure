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
