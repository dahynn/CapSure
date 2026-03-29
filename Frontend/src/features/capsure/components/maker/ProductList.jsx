import React from 'react';
import { X, Plus, Check, Building2 } from 'lucide-react';
import { getProductSourceId } from '../../utils/productSource';

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
    isFetchingProducts,
    onViewDetail,
    page,
    totalPages,
    totalElements,
    hasNext,
    hasPrevious,
    onPrevPage,
    onNextPage
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
                                className={`min-w-[88px] h-10 px-4 rounded-full outline-none text-capsure-base font-bold transition-all border text-center ${activeCategories.includes(cat) ? 'bg-brand-blue text-[#020715] border-brand-blue' : 'bg-capsure-card text-slate-400 border-slate-800 hover:border-slate-600'}`}
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
                        selectedProducts.map(p => {
                            const pId = getProductSourceId(p);
                            const pName = p.productName;
                            const pPrice = Math.floor(p.monthlyPrice);
                            const pCategory = p.categoryLabel;

                            return (
                                <div key={pId} className="flex-shrink-0 flex flex-col bg-capsure-card border border-slate-700/50 rounded-xl px-3.5 py-3 w-[160px] h-[92px]">
                                    <div className="flex justify-between items-start mb-2">
                                        <span className={`text-micro font-bold px-2 py-0.5 rounded ${pCategory === '실손' ? 'bg-brand-purple/20 text-brand-purple' : pCategory === '암' ? 'bg-brand-yellow/20 text-brand-yellow' : 'bg-slate-700/50 text-brand-blue'}`}>
                                            {pCategory}
                                        </span>
                                        <button onClick={() => onRemoveItem(pId)} className="w-[20px] h-[20px] flex items-center justify-center rounded-full bg-[#2A3142] text-slate-400 hover:text-white transition-colors">
                                            <X className="w-3.5 h-3.5" />
                                        </button>
                                    </div>
                                    <p className="text-white text-capsure-base font-bold leading-tight truncate">{pName}</p>
                                    <p className="text-brand-blue text-capsure-base font-bold mt-1">{pPrice?.toLocaleString()}원</p>
                                </div>
                            );
                        })
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
                {isFetchingProducts ? (
                    Array.from({ length: 3 }).map((_, idx) => (
                        <div
                            key={`product-skeleton-${idx}`}
                            className="bg-capsure-card border border-slate-800/70 rounded-[20px] p-4 animate-skeleton-soft"
                        >
                            <div className="flex justify-between items-center mb-3">
                                <div className="h-4 w-24 rounded-md bg-slate-800/70" />
                                <div className="h-3 w-16 rounded bg-slate-800/70" />
                            </div>
                            <div className="space-y-1.5 mb-4">
                                <div className="h-3.5 w-full rounded bg-slate-800/70" />
                                <div className="h-3.5 w-3/4 rounded bg-slate-800/70" />
                            </div>
                            <div className="flex items-end justify-between">
                                <div className="h-5 w-24 rounded bg-slate-800/70" />
                                <div className="h-10 w-10 rounded-full bg-slate-800/70" />
                            </div>
                        </div>
                    ))
                ) : filteredProducts?.length === 0 ? (
                    <div className="py-20 text-center text-slate-500 font-bold">검색 결과가 없습니다.</div>
                ) : (
                    filteredProducts?.map(product => {
                        const productId = getProductSourceId(product);
                        const productName = product.productName;
                        const productPrice = Math.floor(product.monthlyPrice);
                        const productCategoryLabel = product.categoryLabel;
                        const isSelected = selectedProducts.some(p => getProductSourceId(p) === productId);
                        const sectorLabel = product.insurerSector === 'LIFE' ? '생명보험' : '손해보험';

                        return (
                            <div key={productId} className="bg-capsure-card border border-slate-800/80 rounded-[20px] p-5 flex flex-col shadow-sm relative overflow-hidden">
                                {/* Top Row: Badge & Sector */}
                                <div className="flex justify-between items-center mb-4">
                                    <div className="flex gap-2">
                                        <span className={`text-micro font-extrabold px-2 py-1 rounded ${productCategoryLabel === '실손' ? 'bg-[#3D2C42] text-brand-light-purple' : productCategoryLabel === '암' ? 'bg-[#3A331C] text-brand-yellow' : 'bg-[#182F48] text-brand-blue'}`}>
                                            {productCategoryLabel}
                                        </span>
                                        <span className="text-micro font-bold px-2 py-1 rounded bg-slate-800 text-slate-400 border border-slate-700/50">
                                            {sectorLabel}
                                        </span>
                                    </div>
                                    <div className="flex items-center gap-1.5 text-slate-500 text-micro font-bold">
                                        <Building2 className="w-3 h-3" />
                                        {product.companyName}
                                    </div>
                                </div>

                                <h3 className="text-white font-bold text-capsure-lg leading-[1.3] mb-4 truncate w-full">{productName}</h3>

                                {/* Bottom Row: Price */}
                                <div className="flex items-end justify-between">
                                    <div className="flex flex-col">
                                        <div className="flex items-baseline gap-1 mb-3">
                                            <span className="text-brand-blue font-black text-capsure-title tracking-tight">{productPrice?.toLocaleString()}</span>
                                            <span className="text-slate-400 font-bold text-sm">원/월 가입가격</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="mt-1 flex items-end justify-between gap-3">
                                    <button 
                                        onClick={() => onViewDetail && onViewDetail(product)} 
                                        className="w-[34%] min-w-[118px] h-11 rounded-xl border border-slate-700 text-slate-200 text-sm font-bold hover:border-slate-500 hover:bg-slate-800/60 transition-colors"
                                    >
                                        상품 상세
                                    </button>
                                    <button
                                        onClick={() => {
                                            if (isSelected) {
                                                onRemoveItem(productId);
                                            } else {
                                                onAddItem(product);
                                            }
                                        }}
                                        className={`w-[52px] h-[52px] rounded-full flex items-center justify-center transition-all ${isSelected ? 'bg-slate-700 text-white border border-slate-600' : 'bg-brand-blue text-[#020715] hover:opacity-80'}`}
                                    >
                                        {isSelected ? <Check className="w-6 h-6" strokeWidth={3}/> : <Plus className="w-7 h-7" strokeWidth={3} />}
                                    </button>
                                </div>
                            </div>
                        );
                    })
                )}

                <div className="mt-2 mb-6 border border-slate-800/70 rounded-2xl bg-capsure-card px-4 py-3 flex items-center justify-between">
                    <div className="text-slate-400 text-xs font-semibold">
                        총 {totalElements?.toLocaleString?.() ?? totalElements ?? 0}개
                        {totalPages > 0 ? ` · ${page + 1}/${totalPages} 페이지` : ''}
                    </div>
                    <div className="flex items-center gap-2">
                        <button
                            onClick={onPrevPage}
                            disabled={!hasPrevious}
                            className={`h-9 px-3 rounded-lg text-sm font-bold border transition-colors ${
                                hasPrevious
                                    ? 'border-slate-600 text-slate-200 hover:bg-slate-800/70'
                                    : 'border-slate-800 text-slate-600 cursor-not-allowed'
                            }`}
                        >
                            이전
                        </button>
                        <button
                            onClick={onNextPage}
                            disabled={!hasNext}
                            className={`h-9 px-3 rounded-lg text-sm font-bold border transition-colors ${
                                hasNext
                                    ? 'border-slate-600 text-slate-200 hover:bg-slate-800/70'
                                    : 'border-slate-800 text-slate-600 cursor-not-allowed'
                            }`}
                        >
                            다음
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
};

export default ProductList;
