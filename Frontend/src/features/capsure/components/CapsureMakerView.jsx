import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, X, Plus, Check } from 'lucide-react';

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
                {/* Visual Capsule Graphic */}
                <div className="flex flex-col items-center pt-8 pb-6 bg-gradient-to-b from-[#020715] to-[#0A0D14]">
                    <div className="relative w-[84px] h-[168px] rounded-full border-4 border-slate-700/40 bg-capsure-card overflow-hidden shadow-[0_0_40px_rgba(130,216,252,0.1)] mb-6">
                        {/* Glass reflection */}
                        <div className="absolute inset-x-1 top-1 bottom-1 rounded-full border border-white/5 pointer-events-none z-20" />
                        
                        {/* Empty pill icon embedded in glass */}
                        <div className="absolute top-6 left-1/2 -translate-x-1/2 opacity-20 rotate-45 z-10">
                            <div className="w-5 h-8 rounded-full border-2 border-white flex flex-col items-center justify-center" />
                            <div className="w-full h-0 border-t-2 border-white absolute top-1/2" />
                        </div>

                        {/* Fill Progress Liquid */}
                        <div 
                            className="absolute bottom-0 left-0 right-0 bg-gradient-to-b from-[#4F687F] to-[#3A4D60] transition-all duration-700 ease-out z-10"
                            style={{ height: `${progressPercent}%` }}
                        >
                            {/* Crisp liquid surface line */}
                            <div className="absolute top-0 left-0 right-0 h-[2px] bg-[#7A95AD] shadow-[0_1px_5px_rgba(255,255,255,0.1)]" />
                        </div>
                    </div>

                    {/* Budget Info */}
                    <div className="text-center w-full px-8">
                        <p className="text-slate-400 text-capsure-base font-medium mb-1">현재 담은 금액</p>
                        <h2 className="text-4xl font-black text-white mb-6 tracking-tight">
                            {currentAmount.toLocaleString()}원
                        </h2>

                        {/* Progress Bar Container */}
                        <div className="w-full space-y-2">
                            <div className="flex justify-between text-micro font-bold">
                                <span className="text-brand-blue">진행률</span>
                                <span className="text-brand-blue">{Math.round(progressPercent)}%</span>
                            </div>
                            <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                                <div 
                                    className="h-full bg-gradient-to-r from-[#82D8FC] to-[#4A90E2] transition-all duration-500 rounded-full"
                                    style={{ width: `${progressPercent}%` }}
                                />
                            </div>
                            <div className="text-center pt-2">
                                <span className="text-slate-400 text-capsure-sm">남은 예산: <span className="font-bold text-white">{remainingBudget.toLocaleString()}원</span></span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Category Filters */}
                <div className="pt-2 pb-2">
                    <div className="overflow-x-auto hide-scrollbar">
                        <div className="flex gap-2 min-w-max px-6">
                            {categories.map((cat, idx) => (
                                <button
                                    key={idx}
                                    onClick={() => handleCategoryClick(cat)}
                                    className={`px-4 py-2 rounded-full outline-none text-capsure-base font-bold transition-all border ${activeCategories.includes(cat) ? 'bg-brand-blue text-[#020715] border-brand-blue' : 'bg-capsure-card text-slate-400 border-slate-800 hover:border-slate-600'}`}
                                >
                                    {cat}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Selected Chips */}
                <div className="px-6 mt-6">
                    <div className="flex items-center gap-2 mb-3">
                        <h3 className="text-white font-bold text-sm">담은 상품</h3>
                        <span className="bg-slate-700 text-brand-blue text-micro font-bold px-1.5 py-0.5 rounded-md">{selectedProducts.length}</span>
                    </div>
                    <div className="flex gap-2.5 overflow-x-auto hide-scrollbar pb-2 min-h-[92px]">
                        {selectedProducts.length === 0 ? (
                            <div className="w-full min-h-[82px] flex items-center border border-dashed border-slate-700/50 rounded-xl px-4 text-slate-500 text-capsure-base">
                                아래 리스트에서 원하시는 상품을 담아보세요
                            </div>
                        ) : (
                            selectedProducts.map(p => (
                                <div key={p.id} className="flex-shrink-0 flex flex-col bg-capsure-card border border-slate-700/50 rounded-xl px-3.5 py-3 w-[160px] h-[92px]">
                                    <div className="flex justify-between items-start mb-2">
                                        <span className={`text-micro font-bold px-2 py-0.5 rounded ${p.category === '실손' ? 'bg-brand-purple/20 text-brand-purple' : p.category === '암' ? 'bg-brand-yellow/20 text-brand-yellow' : 'bg-slate-700/50 text-brand-blue'}`}>
                                            {p.category}
                                        </span>
                                        <button onClick={() => onRemoveItem(p.id)} className="w-[20px] h-[20px] flex items-center justify-center rounded-full bg-[#2A3142] text-slate-400 hover:text-white transition-colors">
                                            <X className="w-3.5 h-3.5" />
                                        </button>
                                    </div>
                                    <p className="text-white text-capsure-base font-bold leading-tight truncate">{p.name}</p>
                                    <p className="text-brand-blue text-capsure-base font-bold mt-1">{p.price.toLocaleString()}원</p>
                                </div>
                            ))
                        )}
                    </div>
                </div>

                {/* List Divider with Sort */}
                <div className="px-6 mt-8 mb-4 flex justify-end items-center gap-3">
                    <button 
                        onClick={() => setSortBy('price')}
                        className={`text-capsure-sm outline-none font-bold transition-colors ${sortBy === 'price' ? 'text-white' : 'text-slate-500 hover:text-slate-300'}`}
                    >
                        가격 낮은 순
                    </button>
                    <div className="w-[1px] h-2.5 bg-slate-700" />
                    <button 
                        onClick={() => setSortBy('popular')}
                        className={`text-capsure-sm outline-none font-medium transition-colors ${sortBy === 'popular' ? 'text-white' : 'text-slate-500 hover:text-slate-300'}`}
                    >
                        인기순
                    </button>
                </div>

                {/* Product List */}
                <div className="px-6 flex flex-col gap-4 pb-8">
                    {filteredProducts.map(product => {
                        const isSelected = selectedProducts.find(p => p.id === product.id);
                        return (
                            <div key={product.id} className="bg-capsure-card border border-slate-800/80 rounded-[20px] p-5 flex flex-col shadow-sm relative">
                                {/* Top Row: Badge & Title */}
                                <div className="flex items-center gap-2.5 mb-5">
                                    <span className={`text-capsure-sm font-bold px-2 py-1 rounded break-keep whitespace-nowrap ${product.category === '실손' ? 'bg-[#3D2C42] text-brand-light-purple' : product.category === '암' ? 'bg-[#3A331C] text-brand-yellow' : 'bg-[#182F48] text-brand-blue'}`}>
                                        {product.category}
                                    </span>
                                    <h3 className="text-white font-bold text-capsure-title leading-[1.3] truncate w-full">{product.name}</h3>
                                </div>

                                {/* Bottom Row: Price & Buttons */}
                                <div className="flex items-end justify-between">
                                    {/* Price & Link */}
                                    <div className="flex flex-col">
                                        <div className="flex items-baseline gap-1 mb-2">
                                            <span className="text-brand-blue font-black text-capsure-price tracking-tight">{product.price.toLocaleString()}</span>
                                            <span className="text-slate-400 font-bold text-sm">원/월</span>
                                        </div>
                                        <button 
                                            onClick={() => onViewDetail && onViewDetail(product)} 
                                            className="text-slate-400 text-capsure-base underline self-start hover:text-slate-300 transition-colors"
                                        >
                                            상세보기
                                        </button>
                                    </div>
                                    {/* Add Button */}
                                    <button 
                                        onClick={() => {
                                            if (isSelected) {
                                                onRemoveItem(product.id);
                                            } else {
                                                onAddItem(product);
                                            }
                                        }}
                                        className={`w-[52px] h-[52px] rounded-full flex items-center justify-center transition-all ${isSelected ? 'bg-[#2A3142] text-white border border-slate-600' : 'bg-brand-blue text-[#020715] hover:bg-[#6BC1E6]'}`}
                                    >
                                        {isSelected ? <Check className="w-6 h-6" strokeWidth={3}/> : <Plus className="w-7 h-7" strokeWidth={3} />}
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
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
