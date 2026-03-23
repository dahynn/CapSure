import React from 'react';
import { X, Plus, Check } from 'lucide-react';

const ProductList = ({ 
    categories, 
    activeCategories, 
    handleCategoryClick, 
    selectedProducts, 
    onRemoveItem, 
    onAddItem, 
    sortBy, 
    setSortBy, 
    filteredProducts, 
    onViewDetail 
}) => {
    return (
        <>
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
        </>
    );
};

export default ProductList;
