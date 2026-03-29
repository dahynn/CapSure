import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, Pill, ShieldCheck, FileText, ChevronRight, CheckCircle2, Shield, Activity, CalendarClock, Loader2 } from 'lucide-react';
import { getCapsuleDetail, getProductDetail } from '@/features/mypage/api/mypage.api';
import DashboardProductDetailModal from '@/features/dashboard/components/DashboardProductDetailModal';

const CapsuleDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [capsule, setCapsule] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [isProductDetailOpen, setIsProductDetailOpen] = useState(false);
    const [isProductDetailLoading, setIsProductDetailLoading] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [productDetailError, setProductDetailError] = useState('');

    useEffect(() => {
        const fetchDetail = async () => {
            try {
                setLoading(true);
                const data = await getCapsuleDetail(id);
                setCapsule(data);
            } catch (err) {
                console.error(err);
                setError('캡슐 정보를 불러오는데 실패했습니다.');
            } finally {
                setLoading(false);
            }
        };

        fetchDetail();
    }, [id]);

    const handleProductClick = async (productSourceId) => {
        if (!productSourceId) {
            return;
        }

        setIsProductDetailOpen(true);
        setIsProductDetailLoading(true);
        setSelectedProduct(null);
        setProductDetailError('');

        try {
            const detail = await getProductDetail(productSourceId);
            setSelectedProduct(detail);
        } catch (detailError) {
            console.error(detailError);
            setProductDetailError('보험 상품 상세 정보를 불러오지 못했습니다.');
        } finally {
            setIsProductDetailLoading(false);
        }
    };

    const handleCloseProductDetail = () => {
        setIsProductDetailOpen(false);
        setIsProductDetailLoading(false);
        setSelectedProduct(null);
        setProductDetailError('');
    };

    if (loading) {
        return (
            <div className="w-full h-screen bg-[#0B0E14] flex flex-col items-center justify-center">
                <Loader2 className="w-8 h-8 text-[#82D8FC] animate-spin mb-4" />
                <p className="text-[#9D9DA4] text-sm">상세 정보를 불러오는 중입니다...</p>
            </div>
        );
    }

    if (error || !capsule) {
        return (
            <div className="w-full h-screen bg-[#0B0E14] flex flex-col items-center justify-center">
                <p className="text-red-400 mb-4">{error || '데이터를 찾을 수 없습니다.'}</p>
                <button 
                    onClick={() => navigate(-1)} 
                    className="px-6 py-2 bg-[#1C212E] rounded-xl text-white hover:bg-[#1E2535] transition-colors"
                >
                    돌아가기
                </button>
            </div>
        );
    }

    return (
        <div className="w-full flex flex-col flex-1">
            <div className="px-8 py-8 md:px-12 md:py-10 space-y-10 max-w-[560px] mx-auto w-full transition-all animate-in fade-in slide-in-from-bottom-4 duration-500 pb-32">
                
                {/* Header */}
                <div className="flex items-center justify-between mb-4">
                    <button 
                        onClick={() => navigate(-1)} 
                        className="p-2 hover:bg-[#1E2535] rounded-full transition-colors text-white -ml-2"
                    >
                        <ChevronLeft className="w-6 h-6" />
                    </button>
                    <button
                        onClick={() => navigate(`/mypage/capsule/${id}/edit`)}
                        className="flex items-center gap-1.5 px-3.5 py-2 bg-[#82D8FC]/10 border border-[#82D8FC]/30 rounded-full text-[#82D8FC] text-[12px] font-bold hover:bg-[#82D8FC]/20 transition-all active:scale-95"
                    >
                        <CalendarClock className="w-3.5 h-3.5" />
                        익월 변경
                    </button>
                </div>

                {/* Capsule Hero Card */}
                <div className="relative p-8 rounded-[32px] overflow-hidden group shadow-[0_10px_40px_rgba(0,0,0,0.5)] border border-slate-800/80 hover:border-[#82D8FC]/30 transition-colors">
                    <div className="absolute inset-0 bg-gradient-to-br from-[#1C212E] via-[#141925] to-[#0A0E17] -z-10"></div>
                    <div className="absolute top-0 right-0 w-48 h-48 bg-[#82D8FC]/10 rounded-full blur-[50px] -mr-16 -mt-16 pointer-events-none transition-transform duration-1000 group-hover:scale-125 group-hover:bg-[#82D8FC]/20"></div>
                    
                    <div className="flex flex-col mb-8 relative z-10">
                        <div className="flex justify-between items-start mb-6">
                            <div className="w-14 h-14 bg-[#0B0E14] rounded-full flex items-center justify-center border border-slate-700/80 shadow-[0_0_15px_rgba(130,216,252,0.1)] relative">
                                <Pill className="w-6 h-6 text-[#82D8FC]" />
                                <div className="absolute top-0 right-0 w-3.5 h-3.5 rounded-full bg-emerald-400 border-[3px] border-[#0B0E14]"></div>
                            </div>
                            <span className="text-[#82D8FC] text-[11px] font-extrabold px-3 py-1.5 bg-[#82D8FC]/10 border border-[#82D8FC]/20 rounded-full backdrop-blur-sm shadow-[0_0_10px_rgba(130,216,252,0.1)]">
                                {capsule.status}
                            </span>
                        </div>
                        <h2 className="text-white text-2xl font-bold tracking-tight mt-1 drop-shadow-md">{capsule.name}</h2>
                        <p className="text-[#9D9DA4] text-[13px] font-medium mt-1.5 flex items-center gap-1.5 opacity-90">
                            가입 기간: <span className="text-[#82D8FC] font-semibold">{capsule.date}</span>
                        </p>
                    </div>
                    
                    <div className="px-5 py-4 rounded-[20px] bg-[#0B0E14]/60 backdrop-blur-md border border-slate-800/80 relative z-10 flex justify-between items-center group-hover:bg-[#0B0E14]/80 transition-colors">
                        <span className="text-[#9D9DA4] font-medium text-sm">총 월 납입액</span>
                        <div className="text-right">
                            <span className="text-white font-black text-2xl tracking-tighter drop-shadow-md">{capsule.totalPremium.toLocaleString()}</span>
                            <span className="text-[#82D8FC] text-sm ml-1.5 font-bold">원</span>
                        </div>
                    </div>
                </div>

                {/* Product List */}
                <div className="pt-2">
                    <h3 className="text-white text-[15px] font-bold mb-4 flex items-center gap-2 px-1 opacity-90">
                        <Shield className="w-4 h-4 text-[#82D8FC]" /> 속해있는 보험 상품
                    </h3>
                    <div className="space-y-3 relative before:absolute before:inset-y-0 before:left-6 before:w-[2px] before:bg-gradient-to-b before:from-[#82D8FC]/20 before:via-[#82D8FC]/10 before:to-transparent before:-z-10">
                        {capsule.products.map((product, idx) => (
                            <button
                                type="button"
                                key={product.id} 
                                onClick={() => handleProductClick(product.id)}
                                className="p-4 bg-[#141925] rounded-[24px] border border-slate-800/60 hover:bg-[#1C212E] hover:border-[#82D8FC]/30 transition-all cursor-pointer shadow-lg animate-in slide-in-from-bottom-4 fade-in fill-mode-both flex gap-4 items-center group relative overflow-hidden"
                                style={{ animationDelay: `${100 + idx * 100}ms` }}
                            >
                                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-[#82D8FC]/5 to-transparent -translate-x-full group-hover:animate-[shimmer_1.5s_infinite]"></div>
                                <div className="w-10 h-10 rounded-full bg-[#0B0E14] border border-slate-800/80 flex items-center justify-center text-[#9D9DA4] group-hover:text-[#82D8FC] group-hover:bg-[#82D8FC]/5 transition-colors shrink-0 shadow-inner z-10">
                                    <FileText className="w-4 h-4" />
                                </div>
                                <div className="flex-1 space-y-1.5 z-10">
                                    <div className="flex items-center gap-2">
                                        <span className="text-[10px] font-extrabold text-[#141925] bg-[#82D8FC] px-2 py-[2px] rounded-full">{product.type}</span>
                                    </div>
                                    <h4 className="text-white text-sm font-bold tracking-tight">{product.productName}</h4>
                                    <p className="text-[#9D9DA4] text-[11px] font-medium">{product.companyName}</p>
                                </div>
                                <div className="w-8 h-8 rounded-full bg-[#0B0E14] border border-slate-800/50 flex items-center justify-center text-[#4E5669] group-hover:border-[#82D8FC]/30 group-hover:text-[#82D8FC] transition-all shrink-0 z-10 group-hover:translate-x-1">
                                    <ChevronRight className="w-4 h-4" />
                                </div>
                            </button>
                        ))}
                    </div>
                </div>

                {/* Coverage Details */}
                <div className="pt-2 pb-6">
                    <h3 className="text-white text-[15px] font-bold mb-4 flex items-center gap-2 px-1 opacity-90">
                        <ShieldCheck className="w-4 h-4 text-[#82D8FC]" /> 핵심 통합 보장 내역
                    </h3>
                    {capsule.coverages && capsule.coverages.length > 0 ? (
                        <div className="bg-[#141925] rounded-[28px] border border-slate-800/60 p-5 shadow-lg relative overflow-hidden">
                            <div className="absolute top-0 right-0 w-full h-[2px] bg-gradient-to-r from-transparent via-[#82D8FC]/30 to-transparent"></div>
                            <div className="space-y-0 relative z-10">
                                {capsule.coverages.map((coverage, idx) => (
                                    <div 
                                        key={idx} 
                                        className="flex justify-between items-center py-4 border-b border-slate-800/50 last:border-0 hover:bg-[#1C212E]/60 px-3 -mx-3 rounded-xl transition-colors animate-in slide-in-from-bottom-4 fade-in fill-mode-both group"
                                        style={{ animationDelay: `${300 + idx * 100}ms` }}
                                    >
                                        <div className="flex items-start gap-3 w-[60%]">
                                            <div className="w-6 h-6 rounded-full bg-[#0B0E14] border border-slate-800/80 flex items-center justify-center shrink-0 group-hover:bg-[#82D8FC]/20 group-hover:border-[#82D8FC]/30 transition-colors mt-0.5">
                                                <CheckCircle2 className="w-3.5 h-3.5 text-[#82D8FC]" />
                                            </div>
                                            <span className="text-slate-300 text-[13px] font-medium leading-relaxed break-keep group-hover:text-white transition-colors">
                                                {coverage.label}
                                            </span>
                                        </div>
                                        <span className="text-white text-[13px] font-bold text-right shrink-0 max-w-[40%] pl-3 group-hover:text-[#82D8FC] transition-colors">
                                            {coverage.amount}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <div className="flex flex-col items-center justify-center py-10 text-center gap-2 bg-[#141925]/50 rounded-[28px] border border-slate-800/40">
                            <ShieldCheck className="w-8 h-8 text-slate-700 mb-1" />
                            <p className="text-slate-500 text-[13px] font-medium">현재 등록된 보장 내역이 없습니다.</p>
                        </div>
                    )}
                </div>
            </div>

            <DashboardProductDetailModal
                isOpen={isProductDetailOpen}
                onClose={handleCloseProductDetail}
                product={selectedProduct}
                isLoading={isProductDetailLoading}
                errorMessage={productDetailError}
            />
        </div>
    );
};

export default CapsuleDetailPage;
