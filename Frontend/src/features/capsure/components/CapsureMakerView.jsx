import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import CapsureProgress from './maker/CapsureProgress';
import ProductList from './maker/ProductList';
import { httpClient } from '@/common/api/httpClient';
import { getProductSourceId, normalizeProductSource } from '../utils/productSource';
import { CAPSURE_CATEGORY_CODE_BY_LABEL, CAPSURE_CATEGORY_OPTIONS } from '../constants/categories';
import AppButton from '@/common/components/ui/button/AppButton';
import PageTransitionLoading from '@/common/components/ui/loading/PageTransitionLoading';

const DEFAULT_PAGE_SIZE = 12;

const CapsureMakerView = ({ totalBudget, selectedProducts, onAddItem, onRemoveItem, onConfirm, onViewDetail }) => {
    const MIN_TRANSITION_VISIBLE_MS = 760;
    const navigate = useNavigate();
    const location = useLocation();
    
    // Filter & Sort State
    const [activeCategories, setActiveCategories] = useState(['전체']);
    const [sortBy, setSortBy] = useState('popular');
    const [products, setProducts] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [showTransitionLoading, setShowTransitionLoading] = useState(false);
    const [isPreviewLoading, setIsPreviewLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [size] = useState(DEFAULT_PAGE_SIZE);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [hasNext, setHasNext] = useState(false);
    const [hasPrevious, setHasPrevious] = useState(false);
    const transitionShownAtRef = React.useRef(0);

    // Fetch Products from Backend
    const fetchProducts = async () => {
        setIsLoading(true);
        try {
            // Build query params
            const params = new URLSearchParams();
            if (totalBudget) params.append('budget', totalBudget);
            params.append('page', String(page));
            params.append('size', String(size));

            // If multiple categories are supported by backend, handle here.
            // Currently backend getProducts takes a single String category.
            // We'll use the first active category that isn't '전체'
            const filteredCat = activeCategories.find(c => c !== '전체');
            if (filteredCat) {
                params.append('category', CAPSURE_CATEGORY_CODE_BY_LABEL[filteredCat] || filteredCat);
            }

            const queryString = params.toString();
            const response = await httpClient.get(`/insurers/products?${queryString}`);
            const data = response.data;

            if (data.success) {
                const payload = data.data || {};
                const items = Array.isArray(payload) ? payload : (payload.items || []);
                const normalized = items.map(normalizeProductSource);
                setProducts(normalized);
                setTotalPages(Array.isArray(payload) ? 0 : (payload.totalPages || 0));
                setTotalElements(Array.isArray(payload) ? normalized.length : (payload.totalElements || 0));
                setHasNext(Array.isArray(payload) ? false : Boolean(payload.hasNext));
                setHasPrevious(Array.isArray(payload) ? false : Boolean(payload.hasPrevious));
            }
        } catch (error) {
            console.error("Failed to fetch products:", error);
            setProducts([]); // Clear on error
            setTotalPages(0);
            setTotalElements(0);
            setHasNext(false);
            setHasPrevious(false);
        } finally {
            setIsLoading(false);
        }
    };

    React.useEffect(() => {
        setPage(0);
    }, [totalBudget]);

    React.useEffect(() => {
        fetchProducts();
    }, [activeCategories, totalBudget, page, size]);

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

    React.useEffect(() => {
        let openTimer;
        let closeTimer;

        if (isLoading) {
            openTimer = window.setTimeout(() => {
                transitionShownAtRef.current = Date.now();
                setShowTransitionLoading(true);
            }, 40);
        } else if (showTransitionLoading) {
            const elapsed = Date.now() - transitionShownAtRef.current;
            const remaining = Math.max(0, MIN_TRANSITION_VISIBLE_MS - elapsed);
            closeTimer = window.setTimeout(() => {
                setShowTransitionLoading(false);
            }, remaining);
        }

        return () => {
            if (openTimer) {
                window.clearTimeout(openTimer);
            }
            if (closeTimer) {
                window.clearTimeout(closeTimer);
            }
        };
    }, [isLoading, showTransitionLoading]);

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

    const filteredProducts = [...products].sort((a, b) => {
        if (sortBy === 'price') {
            return a.monthlyPrice - b.monthlyPrice;
        }
        if (sortBy === 'popular') {
            // stable fallback for popular
            return getProductSourceId(b) - getProductSourceId(a);
        }
        return 0;
    });

    if (isPreviewLoading || showTransitionLoading) {
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
                    setSortBy={setSortBy}
                    filteredProducts={filteredProducts}
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
