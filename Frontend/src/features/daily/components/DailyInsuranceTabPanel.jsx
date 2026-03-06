import React, { useState, useEffect, useCallback } from 'react';
import { fetchDailyInsurances } from '../api/daily.api';

const MIN_LIMIT = 0;
const MAX_LIMIT = 50000;
const STEP = 1000;

const DailyInsuranceTabPanel = ({ tabName }) => {
    const [minPrice, setMinPrice] = useState(MIN_LIMIT);
    const [maxPrice, setMaxPrice] = useState(MAX_LIMIT);

    const [displayMinPrice, setDisplayMinPrice] = useState(MIN_LIMIT);
    const [displayMaxPrice, setDisplayMaxPrice] = useState(MAX_LIMIT);

    const [items, setItems] = useState([]);
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [isLoading, setIsLoading] = useState(false);

    // Load data based on current state variables
    const loadData = useCallback(async (currentPage = 1, currentMin = minPrice, currentMax = maxPrice) => {
        setIsLoading(true);
        try {
            const response = await fetchDailyInsurances({
                tabName,
                minPrice: currentMin,
                maxPrice: currentMax,
                page: currentPage,
                limit: 5,
            });
            setItems(response.data);
            setTotalPages(response.totalPages || 1);
            setPage(currentPage);
        } catch (error) {
            console.error('Failed to fetch dummy insurances:', error);
        } finally {
            setIsLoading(false);
        }
    }, [tabName]);

    // Initial load or tab change
    useEffect(() => {
        setMinPrice(MIN_LIMIT);
        setMaxPrice(MAX_LIMIT);
        setDisplayMinPrice(MIN_LIMIT);
        setDisplayMaxPrice(MAX_LIMIT);
        loadData(1, MIN_LIMIT, MAX_LIMIT);
    }, [tabName, loadData]);

    // Handle Slider Change
    const handleMinSliderChange = (e) => {
        const value = Math.min(Number(e.target.value), maxPrice - STEP);
        setMinPrice(value);
        setDisplayMinPrice(value);
    };

    const handleMaxSliderChange = (e) => {
        const value = Math.max(Number(e.target.value), minPrice + STEP);
        setMaxPrice(value);
        setDisplayMaxPrice(value);
    };

    // Handle Input Change
    const handleMinInputChange = (e) => {
        setDisplayMinPrice(e.target.value);
    };

    const handleMaxInputChange = (e) => {
        setDisplayMaxPrice(e.target.value);
    };

    const handleMinInputBlur = () => {
        let value = Number(displayMinPrice);
        if (isNaN(value)) value = MIN_LIMIT;
        if (value < MIN_LIMIT) value = MIN_LIMIT;
        if (value > maxPrice - STEP) value = maxPrice - STEP;
        setMinPrice(value);
        setDisplayMinPrice(value);
    };

    const handleMaxInputBlur = () => {
        let value = Number(displayMaxPrice);
        if (isNaN(value)) value = MAX_LIMIT;
        if (value > MAX_LIMIT) value = MAX_LIMIT;
        if (value < minPrice + STEP) value = minPrice + STEP;
        setMaxPrice(value);
        setDisplayMaxPrice(value);
    };

    const applyFilters = () => {
        loadData(1, minPrice, maxPrice);
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 1 && newPage <= totalPages) {
            loadData(newPage, minPrice, maxPrice);
        }
    };

    const minPos = ((minPrice - MIN_LIMIT) / (MAX_LIMIT - MIN_LIMIT)) * 100;
    const maxPos = ((maxPrice - MIN_LIMIT) / (MAX_LIMIT - MIN_LIMIT)) * 100;

    return (
        <div className="bg-white p-6 rounded-2xl animate-in fade-in flex flex-col gap-6 w-full">

            {/* Header */}
            <div>
                <h3 className="text-xl font-bold text-slate-800 mb-2">{tabName} 보험</h3>
                <p className="text-slate-500 text-sm">
                    원하시는 금액대에 맞는 {tabName} 활동 추천 보험을 확인해보세요.
                </p>
            </div>

            {/* Filter Section */}
            <div className="bg-slate-50 p-6 rounded-xl flex flex-col gap-4">
                <div className="flex justify-between items-center mb-2">
                    <span className="font-semibold text-slate-700">금액대 설정</span>
                </div>

                {/* Dual Range Slider */}
                <div className="relative h-6 flex items-center mb-6">
                    <div className="absolute w-full h-2 bg-slate-200 rounded-full"></div>
                    <div
                        className="absolute h-2 bg-blue-500 rounded-full"
                        style={{ left: `${minPos}%`, right: `${100 - maxPos}%` }}
                    ></div>
                    <input
                        type="range"
                        min={MIN_LIMIT}
                        max={MAX_LIMIT}
                        step={STEP}
                        value={minPrice}
                        onChange={handleMinSliderChange}
                        className="custom-range absolute w-full appearance-none pointer-events-none bg-transparent"
                        style={{
                            zIndex: minPrice > MAX_LIMIT - 100 ? 5 : 3
                        }}
                    />
                    <input
                        type="range"
                        min={MIN_LIMIT}
                        max={MAX_LIMIT}
                        step={STEP}
                        value={maxPrice}
                        onChange={handleMaxSliderChange}
                        className="custom-range absolute w-full appearance-none pointer-events-none bg-transparent"
                        style={{ zIndex: 4 }}
                    />
                    <style>{`
              .custom-range::-webkit-slider-thumb {
                pointer-events: auto;
                appearance: none;
                width: 20px;
                height: 20px;
                background: white;
                border: 2px solid #3b82f6;
                border-radius: 50%;
                cursor: pointer;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
              }
              .custom-range::-moz-range-thumb {
                pointer-events: auto;
                width: 20px;
                height: 20px;
                background: white;
                border: 2px solid #3b82f6;
                border-radius: 50%;
                cursor: pointer;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
              }
            `}</style>
                </div>

                {/* Min / Max Inputs & Apply Button */}
                <div className="flex flex-col sm:flex-row justify-between items-end gap-4">
                    <div className="flex items-center gap-4 w-full sm:w-auto">
                        <div className="flex flex-col">
                            <label className="text-xs text-slate-500 mb-1">최소 금액 (원)</label>
                            <input
                                type="number"
                                value={displayMinPrice}
                                onChange={handleMinInputChange}
                                onBlur={handleMinInputBlur}
                                className="w-28 px-3 py-2 border border-slate-200 rounded-lg text-sm text-right focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                            />
                        </div>
                        <span className="text-slate-400 pb-2">~</span>
                        <div className="flex flex-col">
                            <label className="text-xs text-slate-500 mb-1">최대 금액 (원)</label>
                            <input
                                type="number"
                                value={displayMaxPrice}
                                onChange={handleMaxInputChange}
                                onBlur={handleMaxInputBlur}
                                className="w-28 px-3 py-2 border border-slate-200 rounded-lg text-sm text-right focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                            />
                        </div>
                    </div>
                    <button
                        onClick={applyFilters}
                        className="w-full sm:w-auto px-6 py-2.5 bg-slate-800 text-white text-sm font-semibold rounded-lg hover:bg-slate-700 transition"
                    >
                        적용
                    </button>
                </div>
            </div>

            {/* Insurance List */}
            <div className="flex flex-col gap-4 mt-2">
                {isLoading ? (
                    <div className="py-12 flex justify-center items-center">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
                    </div>
                ) : items.length > 0 ? (
                    items.map(item => (
                        <div key={item.id} className="border border-slate-200 p-5 rounded-xl flex justify-between items-center hover:shadow-md transition bg-white flex-col sm:flex-row gap-4 sm:gap-0">
                            <div className="flex flex-col gap-1 w-full sm:w-auto">
                                <div className="flex gap-2 mb-1">
                                    {item.badges?.map((badge, idx) => (
                                        <span key={idx} className="bg-blue-100 text-blue-700 text-xs font-semibold px-2 py-0.5 rounded">
                                            {badge}
                                        </span>
                                    ))}
                                    <span className="bg-slate-100 text-slate-600 text-xs font-semibold px-2 py-0.5 rounded">
                                        {item.tabName}
                                    </span>
                                </div>
                                <h4 className="text-lg font-bold text-slate-800">{item.title}</h4>
                                <p className="text-sm text-slate-500">{item.description}</p>
                            </div>
                            <div className="flex flex-col items-end gap-3 min-w-fit sm:pl-4 sm:border-l border-slate-100 w-full sm:w-auto pt-4 sm:pt-0 border-t sm:border-t-0">
                                <span className="text-xl font-extrabold text-blue-600">
                                    {item.price.toLocaleString()}원
                                </span>
                                <button className="px-5 py-2 bg-blue-50 text-blue-600 text-sm font-semibold rounded-lg hover:bg-blue-100 transition whitespace-nowrap w-full sm:w-auto">
                                    자세히 보기
                                </button>
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="py-12 text-center text-slate-500 bg-slate-50 rounded-xl">
                        해당 조건에 맞는 보험 상품이 없습니다.
                    </div>
                )}
            </div>

            {/* Pagination */}
            {!isLoading && totalPages > 1 && (
                <div className="flex justify-center items-center gap-2 mt-4">
                    <button
                        onClick={() => handlePageChange(page - 1)}
                        disabled={page === 1}
                        className={`w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition
              ${page === 1 ? 'text-slate-300 cursor-not-allowed' : 'text-slate-600 hover:bg-slate-100'}
            `}
                    >
                        &lt;
                    </button>

                    {Array.from({ length: totalPages }, (_, i) => i + 1).map(pageNum => (
                        <button
                            key={pageNum}
                            onClick={() => handlePageChange(pageNum)}
                            className={`w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition
                ${pageNum === page ? 'bg-blue-500 text-white' : 'text-slate-600 hover:bg-slate-100'}
              `}
                        >
                            {pageNum}
                        </button>
                    ))}

                    <button
                        onClick={() => handlePageChange(page + 1)}
                        disabled={page === totalPages}
                        className={`w-8 h-8 flex items-center justify-center rounded-lg text-sm font-medium transition
              ${page === totalPages ? 'text-slate-300 cursor-not-allowed' : 'text-slate-600 hover:bg-slate-100'}
            `}
                    >
                        &gt;
                    </button>
                </div>
            )}
        </div>
    );
};

export default DailyInsuranceTabPanel;
