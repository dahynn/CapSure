import React, { useState } from 'react';

// 와이어프레임 기반 카테고리 목록
const CATEGORIES = [
    { id: 'death', label: '사망', subLabel: 'DEATH', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#82D8FC" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>, bgColor: '#1A2333' },
    { id: 'cancer', label: '암', subLabel: 'CANCER', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#F2BEF7" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10 2v7.527a2 2 0 0 1-.211.896L4.72 20.55a2 2 0 0 0 1.789 2.895h11.982a2 2 0 0 0 1.789-2.895l-5.068-10.127A2 2 0 0 1 15 9.527V2"></path><path d="M8.5 2h7"></path></svg>, bgColor: '#251D2A' },
    { id: 'vital', label: '뇌/심장', subLabel: 'VITAL', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#F6CD3C" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>, bgColor: '#2A2514' },
    { id: 'indemnity', label: '실손', subLabel: 'INDEMNITY', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#82D8FC" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path><polyline points="9 12 11 14 15 10"></polyline></svg>, bgColor: '#162432' },
    { id: 'surgery', label: '수술', subLabel: 'SURGERY', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#F2BEF7" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="6" cy="6" r="3"></circle><circle cx="6" cy="18" r="3"></circle><line x1="20" y1="4" x2="8.12" y2="15.88"></line><line x1="14.47" y1="14.48" x2="20" y2="20"></line><line x1="8.12" y1="8.12" x2="12" y2="12"></line></svg>, bgColor: '#231D28' },
    { id: 'injury', label: '상해', subLabel: 'INJURY', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#82D8FC" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>, bgColor: '#17242E' },
    { id: 'liability', label: '일상배상책임', subLabel: 'LIABILITY', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#F6CD3C" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3v18"></path><path d="M4 8h16"></path><path d="M4 8l3 6M20 8l-3 6"></path><path d="M4 14h6"></path><path d="M14 14h6"></path></svg>, bgColor: '#2A2514' },
    { id: 'etc', label: '기타', subLabel: 'ETC', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#82D8FC" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle></svg>, bgColor: '#162432' }
];

const CategorySelectionStep = ({ onComplete }) => {
    const [selectedCategories, setSelectedCategories] = useState([]);

    const toggleCategory = (categoryId) => {
        setSelectedCategories(prev => {
            if (prev.includes(categoryId)) {
                return prev.filter(id => id !== categoryId);
            } else {
                return [...prev, categoryId];
            }
        });
    };

    const handleSubmit = () => {
        if (selectedCategories.length === 0) return;
        onComplete(selectedCategories);
    };

    const hasSelection = selectedCategories.length > 0;

    return (
        <div className="flex flex-col h-full animate-in fade-in duration-500 pt-2 pb-4 w-full">
            
            {/* 상단 텍스트 */}
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-white mb-3 leading-snug">
                    어떤 종류의 보장을<br/>찾고 계신가요?
                </h1>
                <p className="text-sm leading-relaxed" style={{ color: 'var(--color-brand-gray)' }}>
                    맞춤 플랜 설정을 위해 해당하는 항목을 모두 선택해 주세요.
                </p>
            </div>

            {/* 그리드 선택 영역 */}
            <div className="flex-1 overflow-y-auto pb-4 scrollbar-hide">
                <div className="grid grid-cols-2 gap-3 pb-8">
                    {CATEGORIES.map(category => {
                        const isSelected = selectedCategories.includes(category.id);
                        return (
                            <button
                                key={category.id}
                                onClick={() => toggleCategory(category.id)}
                                className={`flex flex-col items-center justify-center py-6 px-2 rounded-[2rem] transition-all duration-300 ${
                                    category.fullWidth ? 'col-span-2 flex-row gap-6 py-5' : ''
                                }`}
                                style={{
                                    backgroundColor: '#0B1120',
                                    border: isSelected ? '2px solid var(--color-brand-blue)' : '2px solid #1E2A40',
                                    transform: isSelected ? 'scale(0.98)' : 'scale(1)'
                                }}
                            >
                                {/* 아이콘 배지 영역 */}
                                <div 
                                    className="w-16 h-16 rounded-[1.2rem] flex items-center justify-center mb-3 transition-colors"
                                    style={{ backgroundColor: category.bgColor }}
                                >
                                    {category.icon}
                                </div>

                                {/* 텍스트 영역 */}
                                <div className={category.fullWidth ? 'text-left' : 'text-center'}>
                                    <p className="text-white font-bold text-base mb-1">{category.label}</p>
                                    <p className="text-[10px] tracking-widest uppercase font-medium" style={{ color: '#475569' }}>
                                        {category.subLabel}
                                    </p>
                                </div>
                            </button>
                        );
                    })}
                </div>
            </div>

            {/* 하단 완료 버튼 */}
            <div className="mt-auto pt-2 pb-2">
                <button
                    onClick={handleSubmit}
                    disabled={!hasSelection}
                    className="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95"
                    style={{ 
                        backgroundColor: hasSelection ? 'var(--color-brand-yellow)' : '#5C4E1A',
                        color: hasSelection ? '#020715' : '#131B2E',
                        opacity: hasSelection ? 1 : 0.8
                    }}
                >
                    {hasSelection ? '완료' : '최소 하나를 선택해 주세요'}
                </button>
            </div>
            
        </div>
    );
};

export default CategorySelectionStep;
