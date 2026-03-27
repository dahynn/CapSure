import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import CapsureProgress from './maker/CapsureProgress';
import ProductList from './maker/ProductList';
import { httpClient } from '@/common/api/httpClient';

const categories = ['전체', '사망', '암', '뇌/심장', '실손', '수술', '기타'];

// Category to Backend Enum Mapping
const CATEGORY_MAP = {
    '사망': 'DEATH',
    '암': 'CANCER',
    '뇌/심장': 'BRAIN_HEART',
    '실손': 'ACTUAL_LOSS',
    '수술': 'SURGERY',
    '기타': 'ETC'
};

const CapsureMakerView = ({ totalBudget, selectedProducts, onAddItem, onRemoveItem, onConfirm, onViewDetail }) => {
    const navigate = useNavigate();
    
    // Filter & Sort State
    const [activeCategories, setActiveCategories] = useState(['전체']);
    const [sortBy, setSortBy] = useState('popular');
    const [products, setProducts] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    // Fetch Products from Backend
    const fetchProducts = async () => {
        setIsLoading(true);
        try {
            // Build query params
            const params = new URLSearchParams();
            if (totalBudget) params.append('budget', totalBudget);
            
            // If multiple categories are supported by backend, handle here. 
            // Currently backend getProducts takes a single String category.
            // We'll use the first active category that isn't '전체'
            const filteredCat = activeCategories.find(c => c !== '전체');
            if (filteredCat) {
                params.append('category', CATEGORY_MAP[filteredCat] || filteredCat);
            }

            const response = await httpClient.get(`/insurers/products?${params.toString()}`);
            const data = response.data;
            
            if (data.success) {
                setProducts(data.data || []);
            }
        } catch (error) {
            console.error("Failed to fetch products:", error);
            setProducts([]); // Clear on error
        } finally {
            setIsLoading(false);
        }
    };

    React.useEffect(() => {
        fetchProducts();
    }, [activeCategories, totalBudget]);

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
                    return [cat];
                }
            });
        }
    };

    const currentAmount = selectedProducts.reduce((sum, p) => {
        const pPrice = Number(p.monthlyPrice || p.price || 0);
        return sum + pPrice;
    }, 0);
    const remainingBudget = totalBudget - currentAmount;
    const progressPercent = Math.min((currentAmount / totalBudget) * 100, 100);

    const filteredProducts = [...products].sort((a, b) => {
        if (sortBy === 'price') {
            const priceA = Number(a.monthlyPrice || a.price || 0);
            const priceB = Number(b.monthlyPrice || b.price || 0);
            return priceA - priceB;
        }
        if (sortBy === 'popular') {
            // stable fallback for popular
            return (b.productSourceId || b.id) - (a.productSourceId || a.id);
        }
        return 0;
    });

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
            <div className="fixed bottom-[72px] left-0 right-0 max-w-[560px] mx-auto p-6 z-50">
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
