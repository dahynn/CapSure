import React, { useState } from 'react';

// 보험 카테고리 목록 데이터
const CATEGORIES = [
    { id: 'pet', label: '🐶 펫보험' },
    { id: 'injury', label: '🤕 상해보험' },
    { id: 'silson', label: '🏥 실손보험' },
    { id: 'car', label: '🚗 자동차보험' },
    { id: 'cancer', label: '🧬 암보험' },
    { id: 'travel', label: '✈️ 여행자보험' },
    { id: 'dental', label: '🦷 치아보험' },
    { id: 'life', label: '☂️ 종신보험' },
    { id: 'pension', label: '👴 연금보험' },
    { id: 'fire', label: '🔥 화재보험' },
    { id: 'child', label: '👶 어린이보험' }
];

const CategorySelectionStep = ({ onComplete }) => {
    const [selectedCategories, setSelectedCategories] = useState([]);

    // 카테고리 선택/해제 토글
    const toggleCategory = (categoryId) => {
        setSelectedCategories(prev => {
            if (prev.includes(categoryId)) {
                return prev.filter(id => id !== categoryId);
            } else {
                return [...prev, categoryId];
            }
        });
    };

    // 제출 핸들러
    const handleSubmit = () => {
        // (선택사항) 선택한 카테고리를 저장하는 로직
        // 예: api.saveCategories(selectedCategories)
        
        // 부모 컴포넌트로 완료 이벤트 전달
        onComplete(selectedCategories);
    };

    return (
        <div className="flex flex-col h-full animate-in fade-in slide-in-from-right-8 duration-300">
            <h2 className="text-2xl font-bold text-slate-800 mb-2">
                어떤 보험을 찾으시나요?
            </h2>
            <p className="text-slate-500 mb-6">
                관심 있는 보험 카테고리를 모두 선택해주세요. (중복 선택 가능)
            </p>

            <div className="flex-1 overflow-y-auto">
                <div className="flex flex-wrap gap-3">
                    {CATEGORIES.map(category => {
                        const isSelected = selectedCategories.includes(category.id);
                        return (
                            <button
                                key={category.id}
                                onClick={() => toggleCategory(category.id)}
                                className={`px-4 py-3 rounded-full text-sm font-medium transition-all ${
                                    isSelected 
                                    ? 'bg-primary-500 text-white shadow-md shadow-primary-200 border-transparent' 
                                    : 'bg-white border text-slate-600 border-slate-200 hover:border-primary-300 hover:bg-primary-50'
                                }`}
                            >
                                {category.label}
                            </button>
                        );
                    })}
                </div>
            </div>

            <div className="pt-6 mt-auto">
                <button
                    onClick={handleSubmit}
                    // 최소 1개 이상 선택해야 활성화되도록 할 수 있음 (옵션)
                    // disabled={selectedCategories.length === 0}
                    className="w-full py-4 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 shadow-lg shadow-primary-200 transition-all flex justify-center items-center"
                >
                    선택 완료
                </button>
            </div>
        </div>
    );
};

export default CategorySelectionStep;
