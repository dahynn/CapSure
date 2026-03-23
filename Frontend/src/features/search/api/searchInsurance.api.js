/**
 * 보험 검색에서 사용하는 Mock API
 */

const categories = [
    { id: 'life', name: '생명보험', icon: 'User', bgColor: 'bg-blue-50', iconColor: 'text-blue-500', iconBadge: 'Heart', badge: '최대 할인가' },
    { id: 'nonlife', name: '손해보험', icon: 'Shield', bgColor: 'bg-green-50', iconColor: 'text-green-500' },
    { id: 'driver', name: '운전자보험', icon: 'Car', bgColor: 'bg-orange-50', iconColor: 'text-orange-500', badge2: '밸런스게임' },
    { id: 'pet', name: '펫보험', icon: 'Dog', bgColor: 'bg-purple-50', iconColor: 'text-purple-500' },
    { id: 'travel', name: '해외여행보험', icon: 'Plane', bgColor: 'bg-indigo-50', iconColor: 'text-indigo-500' },
    { id: 'shilson', name: '실손보험', icon: 'Activity', bgColor: 'bg-red-50', iconColor: 'text-red-500' },
];

const products = [
    { id: '1', title: '삼성화재 다이렉트 자동차보험', description: '가장 많이 가입하는 자동차보험', icon: 'Car', iconBg: 'bg-blue-50', iconColor: 'text-blue-500', categoryId: 'driver' },
    { id: '2', title: '현대해상 굿앤굿어린이보험', description: '엄마들이 가장 선호하는 보험', icon: 'Baby', iconBg: 'bg-pink-50', iconColor: 'text-pink-500', categoryId: 'life' },
    { id: '3', title: 'DB손해보험 참좋은운전자보험', description: '실속 있는 보장이 필요할 때', icon: 'ShieldCheck', iconBg: 'bg-green-50', iconColor: 'text-green-500', categoryId: 'driver' },
    { id: '4', title: 'KB손해보험 다이렉트 펫보험', description: '소중한 반려동물을 위한 보험', icon: 'Dog', iconBg: 'bg-yellow-50', iconColor: 'text-yellow-600', categoryId: 'pet' },
];

export const getInsuranceCategories = async () => {
    return new Promise((resolve) => {
        setTimeout(() => resolve(categories), 200);
    });
};

export const getInsuranceProductsByCategory = async (categoryId) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            if (categoryId === 'all') resolve(products);
            else resolve(products.filter(p => p.categoryId === categoryId));
        }, 200);
    });
};

export const getInsuranceDetail = async (id) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            const product = products.find(p => p.id === id) || products[0];
            resolve({
                id: product.id,
                title: product.title,
                subtitle: '당신의 일상을 지켜주는 든든한 파트너',
                subscript: '가장 합리적인 비용으로 최대의 보장을 누리세요. 삼성화재가 함께합니다.',
                coverages: [
                    { title: '사고 처리', mainText: '대인/대물 무한 보장\n(기본)', icon: 'Car', iconBg: 'bg-blue-100', iconColor: 'text-blue-600', detailText: '최대 10억원 보장' },
                    { title: '상해 보장', mainText: '본인 상해 치료비\n지원', icon: 'UserPlus', iconBg: 'bg-green-100', iconColor: 'text-green-600', subText: '특약 가입 시\n추가 보장', detailText: '최대 5천만원 보장' }
                ],
                aiSummary: [
                    '현재 고객님의 연령대에서 가장 가성비가 좋은 상품입니다.',
                    '필수 특약 위주로 구성되어 중복 보장 걱정이 없습니다.',
                    '온라인 가입 시 오프라인 대비 평균 15.3% 저렴합니다.'
                ],
                aiNote: '※ 이 분석은 고객님의 기존 보험 가입 데이터를 바탕으로 생성되었습니다.'
            });
        }, 300);
    });
};
