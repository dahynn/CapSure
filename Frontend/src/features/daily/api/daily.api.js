const TABS = ['원데이 자동차', '레저/스포츠', '산책/외출 펫', '단기 여행자', '미니 상해'];

const generateDummyData = () => {
    const data = [];
    let idCounter = 1;
    TABS.forEach(tab => {
        // Generate 15-25 items per tab
        const count = 15 + Math.floor(Math.random() * 10);
        for (let i = 0; i < count; i++) {
            const price = Math.floor(Math.random() * 45 + 5) * 1000; // 5000 ~ 50000
            data.push({
                id: idCounter++,
                tabName: tab,
                title: `[${tab}] 안전 보장 일일 보험 ${i + 1}`,
                description: `하루 동안 ${tab} 활동 시 발생할 수 있는 상해 및 손해를 보장합니다. 안전한 활동을 위해 필수입니다.`,
                price: price,
                badges: ['추천', '인기'].filter(() => Math.random() > 0.5),
            });
        }
    });
    return data;
};

const dummyInsurances = generateDummyData();

export const fetchDailyInsurances = async ({ tabName, minPrice = 0, maxPrice = 50000, page = 1, limit = 10 }) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            let filteredData = dummyInsurances;

            if (tabName) {
                filteredData = filteredData.filter(item => item.tabName === tabName);
            }

            filteredData = filteredData.filter(item => item.price >= minPrice && item.price <= maxPrice);

            const total = filteredData.length;
            const totalPages = Math.ceil(total / limit);
            const startIndex = (page - 1) * limit;
            const endIndex = startIndex + limit;
            const paginatedData = filteredData.slice(startIndex, endIndex);

            resolve({
                data: paginatedData,
                total,
                page,
                totalPages,
                limit
            });
        }, 500); // Simulate network delay
    });
};
