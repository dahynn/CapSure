import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import CapsureProgress from './maker/CapsureProgress';
import ProductList from './maker/ProductList';

const mockProducts = [
    { id: 1, category: '실손', name: 'KB 4세대 실손보험', price: 12500, company: 'KB', isRecommended: true },
    { id: 2, category: '암', name: '삼성화재 다이렉트 암보험', price: 18200, company: '삼성화재', isRecommended: true },
    { id: 3, category: '뇌/심장', name: 'DB손보 뇌심장 집중보장', price: 24000, company: 'DB손보' },
    { id: 4, category: '수술', name: '현대해상 수술비 플랜', price: 9800, company: '현대해상' },
    { id: 5, category: '치아', name: '라이나생명 치아보험', price: 15000, company: '라이나생명' },
];

const categories = ['전체', '사망', '암', '뇌/심장', '실손', '수술', '치아'];

const CapsureMakerView = ({ totalBudget, selectedProducts, onAddItem, onRemoveItem, onConfirm, onViewDetail }) => {
    const navigate = useNavigate();
    
    // Filter & Sort State
    const [activeCategories, setActiveCategories] = useState(['전체']);
    const [sortBy, setSortBy] = useState('price');

    const handleCategoryClick = (cat) => {
        if (cat === '전체') {
            setActiveCategories(['전체']);
        } else {
            setActiveCategories(prev => {
                const newCats = prev.filter(c => c !== '전체');
                if (newCats.includes(cat)) {
                    const removed = newCats.filter(c => c !== cat);
                    return removed.length === 0 ? ['전체'] : removed;
                } else {
                    return [...newCats, cat];
                }
            });
        }
    };

    const currentAmount = selectedProducts.reduce((sum, p) => sum + p.price, 0);
    const remainingBudget = totalBudget - currentAmount;
    const progressPercent = Math.min((currentAmount / totalBudget) * 100, 100);

    const filteredProducts = activeCategories.includes('전체')
        ? mockProducts
        : mockProducts.filter(p => activeCategories.includes(p.category));

    return (
        <div className="flex flex-col min-h-screen pb-28">
            {/* Header */}
            <header className="sticky top-0 z-50 flex items-center justify-center p-4 bg-[#020715] border-b border-transparent">
                <button 
                    onClick={() => navigate(-1)} 
                    className="absolute left-4 p-2 text-white hover:bg-slate-800 rounded-full transition-colors"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-base font-bold text-white">나만의 캡슐 만들기</h1>
            </header>

            <div className="flex-1 overflow-y-auto hide-scrollbar">
                <CapsureProgress 
                    currentAmount={currentAmount}
                    totalBudget={totalBudget}
                    progressPercent={progressPercent}
                    remainingBudget={remainingBudget}
                />

                <ProductList 
                    categories={categories}
                    activeCategories={activeCategories}
                    handleCategoryClick={handleCategoryClick}
                    selectedProducts={selectedProducts}
                    onRemoveItem={onRemoveItem}
                    onAddItem={onAddItem}
                    sortBy={sortBy}
                    setSortBy={setSortBy}
                    filteredProducts={filteredProducts}
                    onViewDetail={onViewDetail}
                />
            </div>

            {/* Sticky Bottom Action */}
            <div className="fixed bottom-0 left-0 right-0 max-w-[560px] mx-auto bg-[#0A0E17] p-6 border-t border-slate-800/80 z-50">
                <button 
                    onClick={onConfirm}
                    className="w-full py-4 rounded-xl font-bold text-[#020715] text-base bg-brand-blue shadow-[0_0_20px_rgba(130,216,252,0.2)] hover:bg-[#6BC1E6] active:scale-[0.98] transition-all"
                >
                    캡슐 생성 완료하기
                </button>
            </div>
        </div>
    );
};

export default CapsureMakerView;
