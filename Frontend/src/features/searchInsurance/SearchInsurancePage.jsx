import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import * as Icons from "lucide-react";
import { getInsuranceCategories, getInsuranceProductsByCategory } from "./api/searchInsurance.api";

// Helper component to dynamically render Lucide icons
const DynamicIcon = ({ name, className }) => {
    const IconComponent = Icons[name] || Icons.HelpCircle;
    return <IconComponent className={className} />;
};

// ProductCard component for displaying individual insurance products
const ProductCard = ({ product }) => {
    const navigate = useNavigate();

    return (
        <div
            key={product.id}
            className="flex items-center justify-between cursor-pointer group"
            onClick={() => navigate(`/search-insurance/${product.id}`)}
        >
            <div className="flex items-center space-x-4">
                <div className={`w-[48px] h-[48px] rounded-[18px] ${product.iconBg} flex items-center justify-center border border-gray-50`}>
                    <DynamicIcon name={product.icon} className={`w-6 h-6 ${product.iconColor}`} />
                    {/* In case there's an inner plus icon or something, simulating typical insurance UI */}
                    {product.categoryId === 'life' && (
                        <div className="absolute ml-5 mt-5 bg-white rounded-full">
                            <Icons.PlusCircle className="w-3 h-3 text-blue-400 fill-white" />
                        </div>
                    )}
                </div>
                <div className="flex flex-col justify-center">
                    <h3 className="text-[16px] font-bold text-gray-900 mb-0.5">{product.title}</h3>
                    <p className="text-[13px] text-gray-500 leading-snug">{product.description}</p>
                </div>
            </div>
            <Icons.ChevronRight className="w-5 h-5 text-gray-300" />
        </div>
    );
};

const SearchInsurancePage = () => {
    const [categories, setCategories] = useState([]);
    const [products, setProducts] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState("all");
    const [filterType, setFilterType] = useState('popular'); // 'popular' | 'category'

    // Fetch categories on mount
    useEffect(() => {
        const fetchCategories = async () => {
            const data = await getInsuranceCategories();
            setCategories(data);
        };
        fetchCategories();
    }, []);

    // Fetch products based on filter and selected category
    useEffect(() => {
        const fetchProducts = async () => {
            const data = await getInsuranceProductsByCategory(
                filterType === 'popular' ? 'all' : selectedCategory
            );
            setProducts(data);
        };
        fetchProducts();
    }, [selectedCategory, filterType]);

    return (
        <div className="min-h-screen bg-gray-100 font-sans pb-20">
            {/* Top Categories Scroll Area */}
            <div className="bg-white pt-6 pb-4 mb-3 border-b border-gray-100">
                <div className="flex overflow-x-auto hide-scrollbar px-4 space-x-5">
                    {categories.map((cat) => (
                        <div
                            key={cat.id}
                            className="flex flex-col items-center justify-start min-w-[64px] cursor-pointer"
                            onClick={() => {
                                setSelectedCategory(cat.id);
                                setFilterType('category');
                            }}
                        >
                            <div className="relative mb-2">
                                <div className={`w-[52px] h-[52px] rounded-2xl ${cat.bgColor} flex items-center justify-center relative shadow-sm border border-gray-50`}>
                                    <DynamicIcon name={cat.icon} className={`w-6 h-6 ${cat.iconColor} ${cat.iconColor.includes('white') ? 'fill-current' : ''}`} />

                                    {/* Small badge icon in the bottom right of the container */}
                                    {cat.iconBadge && (
                                        <div className="absolute -bottom-1 -right-1 bg-white rounded-full p-0.5 shadow-sm border border-gray-100">
                                            <DynamicIcon name={cat.iconBadge} className="w-3.5 h-3.5 text-red-500 fill-red-500" />
                                        </div>
                                    )}
                                </div>

                                {/* Top left floating badge (할인최대) */}
                                {cat.badge && (
                                    <div className="absolute -top-3 -left-2 bg-[#ff6b35] text-white text-[9px] font-bold px-1.5 py-0.5 rounded-[6px] shadow-sm whitespace-pre-line text-center leading-tight z-10 w-max">
                                        {cat.badge}
                                    </div>
                                )}

                                {/* Top center floating badge (밸런스게임) */}
                                {cat.badge2 && (
                                    <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-white text-green-600 border border-green-500 text-[9px] font-bold px-1.5 py-0.5 rounded-full shadow-sm whitespace-nowrap z-10">
                                        {cat.badge2}
                                    </div>
                                )}
                            </div>
                            <span
                                className={`text-[13px] font-medium whitespace-nowrap text-center ${selectedCategory === cat.id && filterType === 'category' ? 'text-gray-900 font-bold' : 'text-gray-600'}`}
                            >
                                {cat.name}
                            </span>
                        </div>
                    ))}
                </div>

                <div className="px-4 mt-2 mb-2">
                    <div className="h-1 bg-gray-200 rounded-full w-full relative">
                        <div className="h-full bg-gray-400 rounded-full absolute left-0" style={{ width: '40%' }}></div>
                    </div>
                </div>

                {/* Consultation Banner */}
                <div className="px-4 mt-5">
                    <div className="bg-[#f9f9fb] border border-gray-100 rounded-2xl py-3.5 px-4 flex justify-center items-center cursor-pointer">
                        <span className="text-[14px] text-gray-700 font-medium">내 보험 충분한지</span>
                        <span className="text-[14px] text-green-600 font-bold ml-1">전문가에게 상담받기</span>
                        <Icons.ChevronRight className="w-4 h-4 text-green-600 ml-1" />
                    </div>
                </div>
            </div>

            {/* List Section */}
            <div className="bg-white px-5 pt-8 pb-10 border-b border-gray-100 min-h-[400px]">
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-xl font-bold text-gray-900">보험 모아보기</h2>
                    <span className="text-[13px] text-gray-500 flex items-center cursor-pointer">
                        전체보기 <Icons.ChevronRight className="w-3.5 h-3.5 ml-0.5" />
                    </span>
                </div>

                {/* Filter Tabs */}
                <div className="flex space-x-2 mb-6">
                    <button
                        className={`px-4 py-1.5 rounded-full text-[14px] font-bold transition-colors ${filterType === 'popular' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-500'}`}
                        onClick={() => {
                            setFilterType('popular');
                            setSelectedCategory('all');
                        }}
                    >
                        인기
                    </button>
                    <button
                        className={`px-4 py-1.5 rounded-full text-[14px] font-bold transition-colors ${filterType === 'category' ? 'bg-gray-800 text-white' : 'bg-gray-100 text-gray-500'}`}
                        onClick={() => {
                            setFilterType('category');
                            // If no category selected yet, pick first one visually
                            if (selectedCategory === 'all' && categories.length > 0) {
                                setSelectedCategory(categories[0].id);
                            }
                        }}
                    >
                        카테고리
                    </button>
                </div>

                {/* Products List */}
                <div className="space-y-6">
                    {products.length > 0 ? (
                        products.map(product => (
                            <ProductCard key={product.id} product={product} />
                        ))
                    ) : (
                        <div className="text-center py-10 text-gray-400 text-[14px]">
                            해당 카테고리의 상품이 없습니다.
                        </div>
                    )}
                </div>
            </div>

            {/* Personal Recommendation Card */}
            <div className="px-4 mt-5 mb-8">
                <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
                    <div className="flex items-center mb-4">
                        <div className="w-10 h-10 rounded-full bg-white flex overflow-hidden items-center justify-center mr-3 relative shadow-[0_2px_8px_rgba(0,0,0,0.08)]">
                            {/* DB Logo mockup */}
                            <div className="flex space-x-0.5 items-end h-5">
                                <div className="w-1.5 h-3 bg-blue-500 rounded-t-full rounded-b-sm"></div>
                                <div className="w-1.5 h-4 bg-orange-400 rounded-t-full rounded-b-sm"></div>
                                <div className="w-1.5 h-5 bg-green-500 rounded-t-full rounded-b-sm"></div>
                            </div>
                        </div>
                        <div>
                            <div className="text-[12px] text-gray-500 mb-0.5">DB손해보험</div>
                            <div className="text-[16px] font-bold text-gray-900">재형님이 원하는 보험은?</div>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-2 mt-4">
                        <div className="bg-[#fff7f4] rounded-[14px] p-4 flex flex-col justify-between h-[100px] hover:opacity-90 cursor-pointer">
                            <div className="text-[14px] font-bold text-[#f05c2b] leading-snug">
                                직장인에게 필요한<br />건강 보장 더 받기
                            </div>
                            <div className="self-end text-[12px] text-[#f05c2b] mt-1">
                                <span className="font-bold mr-0.5">{'>'}</span>
                            </div>
                        </div>
                        <div className="bg-[#f4f8ff] rounded-[14px] p-4 flex flex-col justify-between h-[100px] hover:opacity-90 cursor-pointer">
                            <div className="text-[14px] font-bold text-[#4a72d4] leading-snug">
                                운전자보험 실속있게<br />가입하기
                            </div>
                            <div className="self-end text-[12px] text-[#4a72d4] mt-1">
                                <span className="font-bold mr-0.5">{'>'}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    );
};

export default SearchInsurancePage;
