import React from 'react';
import { SlidersHorizontal, Loader2, Plus } from 'lucide-react';

const CapsuleItemExplorer = ({
    categories,
    activeCategory,
    setActiveCategory,
    activeFilter,
    setActiveFilter,
    setIsFilterModalOpen,
    isItemsLoading,
    categoryItems,
    expandedItemId,
    setExpandedItemId,
    setSelectedTermsText,
    setIsTermsModalOpen,
    handleAddItem
}) => {
    return (
        <div className="bg-white p-6 md:p-8 rounded-3xl shadow-sm border border-slate-200 flex flex-col h-full">
            <div className="flex items-center justify-between mb-6">
                <h3 className="font-bold text-slate-800 text-xl flex items-center gap-2">
                    보험 탐색
                </h3>
            </div>

            {/* Horizontal Category Scroll */}
            <div className="flex overflow-x-auto gap-3 pb-4 scrollbar-none border-b border-slate-100">
                {categories.map((cat) => (
                    <button
                        key={cat.id}
                        onClick={() => setActiveCategory(cat.id)}
                        className={`flex-shrink-0 px-4 py-2.5 text-sm font-bold rounded-xl whitespace-nowrap transition-colors border ${activeCategory === cat.id
                            ? 'bg-primary-50 text-primary-700 border-primary-200'
                            : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                            }`}
                    >
                        {cat.name}
                    </button>
                ))}
            </div>

            {/* Filter Bar */}
            <div className="flex items-center gap-2 overflow-x-auto py-4 scrollbar-none">
                <button
                    onClick={() => setIsFilterModalOpen(true)}
                    className="flex-shrink-0 flex items-center justify-center w-10 h-10 bg-slate-100 text-slate-600 rounded-xl hover:bg-slate-200 transition-colors"
                >
                    <SlidersHorizontal className="w-5 h-5" />
                </button>

                <div className="h-6 w-px bg-slate-200 mx-2"></div>

                {[1, 3, 5].map(price => (
                    <button
                        key={price}
                        onClick={() => setActiveFilter(activeFilter === price ? null : price)}
                        className={`flex-shrink-0 px-4 py-2 text-sm font-bold rounded-xl whitespace-nowrap transition-colors border ${activeFilter === price
                            ? 'bg-slate-800 text-white border-slate-800'
                            : 'bg-white text-slate-600 border-slate-200 hover:border-slate-300 hover:bg-slate-50'
                            }`}
                    >
                        {price}만원 이하
                    </button>
                ))}
            </div>

            {/* Items List */}
            <div className="flex-1 overflow-y-auto pr-2 min-h-[400px]">
                {activeCategory ? (
                    <div className="flex flex-col gap-3">
                        {isItemsLoading ? (
                            <div className="flex justify-center py-10">
                                <Loader2 className="w-8 h-8 text-slate-400 animate-spin" />
                            </div>
                        ) : categoryItems.length > 0 ? (
                            categoryItems.map((item) => {
                                const cat = categories.find(c => c.id === activeCategory);
                                const isExpanded = expandedItemId === item.id;
                                return (
                                    <div key={item.id} className={`border-2 rounded-xl overflow-hidden bg-white ${cat.color}`}>
                                        <div
                                            onClick={() => setExpandedItemId(isExpanded ? null : item.id)}
                                            className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50 transition-colors"
                                        >
                                            <div className="flex flex-col gap-1.5">
                                                <span className="font-bold text-slate-800 text-lg leading-tight">{item.name}</span>
                                                <span className="text-sm font-medium text-slate-500">{item.company}</span>
                                            </div>
                                            <div className="flex flex-col items-end gap-1.5">
                                                <span className="font-bold text-lg text-slate-900">{item.price}만원</span>
                                                <span className="text-[11px] font-bold bg-white px-2.5 py-1 rounded-md shadow-sm">{item.price}칸 차지</span>
                                            </div>
                                        </div>

                                        {isExpanded && (
                                            <div className="px-5 pb-5 pt-2 bg-white border-t border-slate-100">
                                                <table className="w-full mb-4">
                                                    <thead>
                                                        <tr className="border-b border-slate-200">
                                                            <th className="py-2.5 text-left text-sm font-bold text-slate-500">보장 내용</th>
                                                            <th className="py-2.5 text-right text-sm font-bold text-slate-500">보장 금액</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {item.coverages?.map((cov, idx) => (
                                                            <tr key={idx} className="border-b border-slate-100 last:border-0">
                                                                <td className="py-3 text-sm text-slate-700">{cov.label}</td>
                                                                <td className="py-3 text-sm text-slate-900 font-bold text-right">{cov.amount}</td>
                                                            </tr>
                                                        ))}
                                                    </tbody>
                                                </table>
                                                <div className="flex justify-end items-center mb-5">
                                                    <button
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            setSelectedTermsText(item.termsText);
                                                            setIsTermsModalOpen(true);
                                                        }}
                                                        className="text-sm text-primary-600 underline hover:text-primary-800 font-medium"
                                                    >
                                                        약관 상세보기
                                                    </button>
                                                </div>
                                                <button
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleAddItem(cat, item);
                                                    }}
                                                    className="w-full py-3.5 bg-primary-600 text-white font-bold rounded-xl hover:bg-primary-700 transition-colors flex items-center justify-center gap-2 shadow-sm"
                                                >
                                                    <Plus className="w-5 h-5" /> 담기
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                );
                            })
                        ) : (
                            <div className="py-12 text-center text-slate-500 flex flex-col items-center gap-3">
                                <span className="font-medium text-lg">조회된 보험 상품이 없습니다.</span>
                                <span className="text-sm">다른 필터를 선택해주세요.</span>
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="py-12 text-center text-slate-500 flex flex-col items-center gap-3">
                        <span className="font-medium text-lg">카테고리를 선택해주세요.</span>
                    </div>
                )}
            </div>
        </div>
    );
};

export default CapsuleItemExplorer;
