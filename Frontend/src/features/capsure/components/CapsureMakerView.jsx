import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import CapsureProgress from './maker/CapsureProgress';
import ProductList from './maker/ProductList';
import { httpClient } from '@/common/api/httpClient';
import { normalizeProductSource } from '../utils/productSource';
import { CAPSURE_CATEGORY_CODE_BY_LABEL, CAPSURE_CATEGORY_OPTIONS } from '../constants/categories';
import AppButton from '@/common/components/ui/button/AppButton';
import PageTransitionLoading from '@/common/components/ui/loading/PageTransitionLoading';

const DEFAULT_PAGE_SIZE = 12;
const MAKER_PRODUCT_CACHE_KEY = 'capsure-maker-product-cache-v1';

const CapsureMakerView = ({ totalBudget, selectedProducts, onAddItem, onRemoveItem, onConfirm, onViewDetail }) => {
    const navigate = useNavigate();
    const location = useLocation();
    
    // Filter & Sort State
    const [activeCategories, setActiveCategories] = useState(['전체']);
    const [sortBy, setSortBy] = useState('popular');
    const [products, setProducts] = useState([]);
    const [isPreviewLoading, setIsPreviewLoading] = useState(false);
    const [isFetchingProducts, setIsFetchingProducts] = useState(false);
    const [page, setPage] = useState(0);
    const [size] = useState(DEFAULT_PAGE_SIZE);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [hasNext, setHasNext] = useState(false);
    const [hasPrevious, setHasPrevious] = useState(false);
    const cacheRef = React.useRef((() => {
        try {
            const raw = sessionStorage.getItem(MAKER_PRODUCT_CACHE_KEY);
            if (!raw) {
                return new Map();
            }
            const parsed = JSON.parse(raw);
            if (!Array.isArray(parsed)) {
                return new Map();
            }
            return new Map(parsed);
        } catch {
            return new Map();
        }
    })());
    const abortControllerRef = React.useRef(null);
    const requestIdRef = React.useRef(0);

    const persistCache = React.useCallback(() => {
        try {
            sessionStorage.setItem(
                MAKER_PRODUCT_CACHE_KEY,
                JSON.stringify(Array.from(cacheRef.current.entries()))
            );
        } catch {
            // ignore storage errors
        }
    }, []);

    const getActiveCategoryCode = () => {
        const filteredCat = activeCategories.find(c => c !== '전체');
        return filteredCat ? (CAPSURE_CATEGORY_CODE_BY_LABEL[filteredCat] || filteredCat) : '';
    };

    const buildQueryString = () => {
        const params = new URLSearchParams();
        if (totalBudget) params.append('budget', totalBudget);
        params.append('page', String(page));
        params.append('size', String(size));
        params.append('sortBy', sortBy === 'price' ? 'price_asc' : 'price_desc');

        const categoryCode = getActiveCategoryCode();
        if (categoryCode) {
            params.append('category', categoryCode);
        }
        return params.toString();
    };

    const applyPayload = (payload) => {
        const items = Array.isArray(payload) ? payload : (payload.items || []);
        const normalized = items.map(normalizeProductSource);
        setProducts(normalized);
        setTotalPages(Array.isArray(payload) ? 0 : (payload.totalPages || 0));
        setTotalElements(Array.isArray(payload) ? normalized.length : (payload.totalElements || 0));
        setHasNext(Array.isArray(payload) ? false : Boolean(payload.hasNext));
        setHasPrevious(Array.isArray(payload) ? false : Boolean(payload.hasPrevious));
    };

    // Fetch Products from Backend
    const fetchProducts = async () => {
        const queryString = buildQueryString();
        const cacheKey = queryString;
        const cachedEntry = cacheRef.current.get(cacheKey);
        const cached = cachedEntry?.payload || cachedEntry;
        const isFreshCache = Boolean(
            cachedEntry?.cachedAt && (Date.now() - cachedEntry.cachedAt < 45_000)
        );

        if (cached) {
            applyPayload(cached);
        }

        if (cached && isFreshCache) {
            setIsFetchingProducts(false);
            return;
        }

        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
        }
        const controller = new AbortController();
        abortControllerRef.current = controller;
        const requestId = ++requestIdRef.current;

        setIsFetchingProducts(!cached);

        try {
            const response = await httpClient.get(`/insurers/products?${queryString}`, {
                signal: controller.signal,
            });
            const data = response.data;

            if (data.success) {
                const payload = data.data || {};
                cacheRef.current.set(cacheKey, { payload, cachedAt: Date.now() });
                persistCache();

                if (requestId !== requestIdRef.current) {
                    return;
                }
                applyPayload(payload);
            }
        } catch (error) {
            if (error?.name === 'AbortError') {
                return;
            }
            console.error("Failed to fetch products:", error);
            setProducts([]); // Clear on error
            setTotalPages(0);
            setTotalElements(0);
            setHasNext(false);
            setHasPrevious(false);
        } finally {
            if (requestId === requestIdRef.current) {
                setIsFetchingProducts(false);
            }
        }
    };

    React.useEffect(() => {
        setPage(0);
    }, [totalBudget]);

    React.useEffect(() => {
        fetchProducts();
    }, [activeCategories, totalBudget, page, size, sortBy]);

    React.useEffect(() => {
        return () => {
            if (abortControllerRef.current) {
                abortControllerRef.current.abort();
            }
        };
    }, []);

    React.useEffect(() => {
        const searchParams = new URLSearchParams(location.search);
        const shouldPreview = searchParams.get('previewLoading') === '1';

        if (!shouldPreview) {
            setIsPreviewLoading(false);
            return;
        }

        setIsPreviewLoading(true);
        const previewTimer = window.setTimeout(() => {
            setIsPreviewLoading(false);
        }, 2800);

        return () => {
            window.clearTimeout(previewTimer);
        };
    }, [location.search]);

    const handleCategoryClick = (cat) => {
        setPage(0);
        if (cat === '전체') {
            setActiveCategories(['전체']);
        } else {
            setActiveCategories(prev => {
                const newCats = prev.filter(c => c !== '전체');
                if (newCats.includes(cat)) {
                    const removed = newCats.filter(c => c !== cat);
                    return removed.length === 0 ? ['전체'] : removed;
                } else {
                    return [cat];
                }
            });
        }
    };

    const currentAmount = selectedProducts.reduce((sum, p) => {
        return sum + p.monthlyPrice;
    }, 0);
    const remainingBudget = totalBudget - currentAmount;
    const progressPercent = Math.min((currentAmount / totalBudget) * 100, 100);

    const handleSortChange = (nextSortBy) => {
        setPage(0);
        setSortBy(nextSortBy);
    };

    if (isPreviewLoading) {
        return (
            <PageTransitionLoading
                message="보험 리스트를 불러오는 중이에요"
                backgroundClassName="bg-[#020715]"
                openDelayMs={0}
                textDelayMs={140}
                doneDelayMs={420}
            />
        );
    }

    return (
        <div className="flex flex-col min-h-screen pb-28">
            {/* Header */}
            <header className="sticky top-0 z-50 flex items-center justify-center p-4 bg-[#020715] border-b border-transparent min-h-[56px]">
                <button 
                    onClick={() => navigate(-1)} 
                    className="absolute left-4 p-2 text-white hover:bg-slate-800 rounded-full transition-colors"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
            </header>

            <div className="flex-1 overflow-y-auto hide-scrollbar">
                <CapsureProgress 
                    currentAmount={currentAmount}
                    totalBudget={totalBudget}
                    progressPercent={progressPercent}
                    remainingBudget={remainingBudget}
                />

                <ProductList 
                    categories={CAPSURE_CATEGORY_OPTIONS}
                    activeCategories={activeCategories}
                    handleCategoryClick={handleCategoryClick}
                    selectedProducts={selectedProducts}
                    onRemoveItem={onRemoveItem}
                    onAddItem={onAddItem}
                    sortBy={sortBy}
                    setSortBy={handleSortChange}
                    filteredProducts={products}
                    isFetchingProducts={isFetchingProducts}
                    onViewDetail={onViewDetail}
                    page={page}
                    totalPages={totalPages}
                    totalElements={totalElements}
                    hasNext={hasNext}
                    hasPrevious={hasPrevious}
                    onPrevPage={() => setPage(prev => Math.max(prev - 1, 0))}
                    onNextPage={() => setPage(prev => prev + 1)}
                />
            </div>

            {/* Sticky Bottom Action */}
            <div className="fixed app-fixed-cta left-0 right-0 max-w-[560px] mx-auto p-6 z-40">
                <AppButton
                    onClick={onConfirm}
                    className="shadow-[0_0_20px_rgba(130,216,252,0.2)]"
                >
                    캡슐 생성 완료하기
                </AppButton>
            </div>
        </div>
    );
};

export default CapsureMakerView;
