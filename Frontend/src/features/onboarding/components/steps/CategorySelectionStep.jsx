import React, { useState } from 'react';

// 와이어프레임 기반 카테고리 목록
const CATEGORIES = [
    { id: 'death', label: '사망', subLabel: 'DEATH', accentColor: '#82D8FC', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>, bgColor: '#1A2333' },
    { id: 'cancer', label: '암', subLabel: 'CANCER', accentColor: '#F2BEF7', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10 2v7.527a2 2 0 0 1-.211.896L4.72 20.55a2 2 0 0 0 1.789 2.895h11.982a2 2 0 0 0 1.789-2.895l-5.068-10.127A2 2 0 0 1 15 9.527V2"></path><path d="M8.5 2h7"></path></svg>, bgColor: '#251D2A' },
    { id: 'vital', label: '뇌/심장', subLabel: 'BRAIN_HEART', accentColor: '#F6CD3C', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>, bgColor: '#2A2514' },
    { id: 'indemnity', label: '실손', subLabel: 'ACTUAL_LOSS', accentColor: '#67E8C8', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path><polyline points="9 12 11 14 15 10"></polyline></svg>, bgColor: '#132A29' },
    { id: 'surgery', label: '수술', subLabel: 'SURGERY', accentColor: '#F2BEF7', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="6" cy="6" r="3"></circle><circle cx="6" cy="18" r="3"></circle><line x1="20" y1="4" x2="8.12" y2="15.88"></line><line x1="14.47" y1="14.48" x2="20" y2="20"></line><line x1="8.12" y1="8.12" x2="12" y2="12"></line></svg>, bgColor: '#231D28' },
    { id: 'injury', label: '상해', subLabel: 'INJURY', accentColor: '#FCA5A5', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>, bgColor: '#2B1B22' },
    { id: 'liability', label: '일상배상책임', subLabel: 'LIABILITY', accentColor: '#F6CD3C', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3v18"></path><path d="M4 8h16"></path><path d="M4 8l3 6M20 8l-3 6"></path><path d="M4 14h6"></path><path d="M14 14h6"></path></svg>, bgColor: '#2A2514' },
    { id: 'etc', label: '기타', subLabel: 'ETC', accentColor: '#C4B5FD', icon: <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle></svg>, bgColor: '#231F33' }
];

const shuffle = (list) => {
    const result = [...list];
    for (let i = result.length - 1; i > 0; i -= 1) {
        const j = Math.floor(Math.random() * (i + 1));
        [result[i], result[j]] = [result[j], result[i]];
    }
    return result;
};

const buildColorBalancedRandomOrder = (categories) => {
    const colorBuckets = categories.reduce((acc, category) => {
        const current = acc.get(category.accentColor) ?? [];
        current.push(category);
        acc.set(category.accentColor, current);
        return acc;
    }, new Map());

    const bucketEntries = shuffle(
        Array.from(colorBuckets.entries()).map(([color, items]) => [color, shuffle(items)])
    );
    const ordered = [];
    let hasItems = true;

    while (hasItems) {
        hasItems = false;
        for (const [, bucket] of bucketEntries) {
            if (bucket.length > 0) {
                ordered.push(bucket.shift());
                hasItems = true;
            }
        }
    }

    return ordered;
};

const CategorySelectionStep = ({ onComplete }) => {
    const [selectedCategories, setSelectedCategories] = useState([]);
    const [displayCategories] = useState(() => buildColorBalancedRandomOrder(CATEGORIES));
    const MIN_SELECTION = 3;

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
        if (selectedCategories.length < MIN_SELECTION) return;
        onComplete(selectedCategories);
    };

    const hasMinSelection = selectedCategories.length >= MIN_SELECTION;

    return (
        <div className="flex h-full min-h-0 w-full flex-col animate-in fade-in duration-500 pt-2 pb-4">
            
            {/* 상단 텍스트 */}
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-white mb-3 leading-snug">
                    어떤 종류의 보장을<br/>찾고 계신가요?
                </h1>
                <p className="text-sm leading-relaxed" style={{ color: 'var(--color-brand-gray)' }}>
                    맞춤 플랜 설정을 위해 해당하는 항목을 모두 선택해 주세요.
                </p>
                <div className="mt-2 flex items-center gap-2" style={{ color: 'var(--color-brand-yellow)' }}>
                    <span
                        className="inline-flex h-4 w-4 items-center justify-center rounded-full text-[10px] font-bold"
                        style={{ border: '1px solid var(--color-brand-yellow)' }}
                    >
                        !
                    </span>
                    <p className="text-xs font-semibold">카테고리를 최소 3개 이상 선택해 주세요.</p>
                </div>
            </div>

            {/* 그리드 선택 영역 */}
            <div className="flex-1 overflow-y-auto pb-2 scrollbar-hide">
                <div className="grid grid-cols-2 gap-x-2 gap-y-3 pb-4">
                    {displayCategories.map(category => {
                        const isSelected = selectedCategories.includes(category.id);
                        return (
                            <button
                                key={category.id}
                                onClick={() => toggleCategory(category.id)}
                                className="aspect-square w-full flex flex-col items-center justify-center rounded-[1.25rem] px-2 transition-all duration-300"
                                style={{
                                    backgroundColor: '#0B1120',
                                    border: isSelected ? `2px solid ${category.accentColor}` : '2px solid #1E2A40',
                                    boxShadow: isSelected ? `0 0 0 1px ${category.accentColor}33` : 'none',
                                    transform: isSelected ? 'scale(0.98)' : 'scale(1)'
                                }}
                            >
                                {/* 아이콘 배지 영역 */}
                                <div 
                                    className="w-12 h-12 rounded-[0.95rem] flex items-center justify-center mb-2 transition-colors"
                                    style={{ backgroundColor: category.bgColor, color: category.accentColor }}
                                >
                                    {category.icon}
                                </div>

                                {/* 텍스트 영역 */}
                                <div className="text-center">
                                    <p className="text-white font-bold text-sm mb-1">{category.label}</p>
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
                    disabled={!hasMinSelection}
                    className="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95"
                    style={{ 
                        backgroundColor: hasMinSelection ? 'var(--color-brand-yellow)' : '#5C4E1A',
                        color: hasMinSelection ? '#020715' : '#131B2E',
                        opacity: hasMinSelection ? 1 : 0.8
                    }}
                >
                    확인
                </button>
            </div>
            
        </div>
    );
};

export default CategorySelectionStep;
