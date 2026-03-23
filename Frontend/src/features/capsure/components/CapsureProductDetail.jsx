import React, { useEffect } from 'react';
import { ChevronLeft, Share2, ShieldCheck, ChevronRight, Info, Zap, Receipt, Pill, PlusSquare } from 'lucide-react';

const CapsureProductDetail = ({ product, onBack, onAdd, isAdded }) => {
    // Scroll to top on mount
    useEffect(() => {
        window.scrollTo(0, 0);
    }, []);

    // Create some fake coverages based on the category
    const coverages = [
        { title: "입원 의료비", desc: "최대 5,000만원", icon: <PlusSquare className="w-5 h-5 text-brand-blue" />, bg: "bg-[#182F48]" },
        { title: "통원 의료비", desc: "회당 20만원", icon: <ShieldCheck className="w-5 h-5 text-brand-light-purple" />, bg: "bg-[#3D2C42]" },
        { title: "처방 조제비", desc: "회당 10만원", icon: <Pill className="w-5 h-5 text-brand-yellow" />, bg: "bg-[#3A331C]" }
    ];

    return (
        <div className="flex flex-col min-h-screen bg-[#020715] animate-in fade-in slide-in-from-bottom-4 duration-300 pb-28">
            {/* Header */}
            <header className="sticky top-0 z-50 flex items-center justify-between p-4 bg-[#020715]">
                <button 
                    onClick={onBack} 
                    className="p-2 text-white hover:bg-slate-800 rounded-full transition-colors flex items-center gap-1 -ml-2 font-bold"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-base font-bold text-white absolute left-1/2 -translate-x-1/2">보험 상세 정보</h1>
                <button className="p-2 text-white hover:bg-slate-800 rounded-full transition-colors">
                    <Share2 className="w-5 h-5" />
                </button>
            </header>

            <div className="px-6 pt-6 flex-1 overflow-y-auto hide-scrollbar">
                {/* Title & Badges */}
                <div className="flex items-center gap-2 mb-4">
                    <span className="bg-[#182F48] text-brand-blue text-capsure-sm font-bold px-3 py-1.5 rounded-lg border border-brand-blue/20">
                        {product.category}
                    </span>
                    <span className="bg-[#3D2C42] text-brand-light-purple text-capsure-sm font-bold px-3 py-1.5 rounded-lg border border-brand-light-purple/20">
                        베스트
                    </span>
                </div>
                
                <h2 className="text-white text-3xl font-black mb-2 leading-tight tracking-tight">
                    {product.name}
                </h2>
                
                <div className="flex items-baseline gap-1.5 mb-10">
                    <span className="text-brand-blue text-2xl font-black tracking-tight">{product.price.toLocaleString()}</span>
                    <span className="text-slate-400 text-base font-medium">원/월</span>
                </div>

                {/* Coverage Details */}
                <div className="mb-10">
                    <div className="flex items-center gap-2 mb-4">
                        <ShieldCheck className="w-5 h-5 text-brand-purple" />
                        <h3 className="text-white text-lg font-bold">보장 내역</h3>
                    </div>
                    <div className="space-y-3">
                        {coverages.map((item, idx) => (
                            <div key={idx} className="bg-capsure-card border border-slate-700/50 rounded-[20px] p-5 flex items-center justify-between shadow-sm active:scale-[0.98] transition-transform cursor-pointer">
                                <div className="flex items-center gap-4">
                                    <div className={`w-12 h-12 rounded-full flex items-center justify-center ${item.bg}`}>
                                        {item.icon}
                                    </div>
                                    <div className="flex flex-col gap-0.5">
                                        <span className="text-slate-400 text-capsure-base font-medium">{item.title}</span>
                                        <span className="text-white text-base font-bold">{item.desc}</span>
                                    </div>
                                </div>
                                <ChevronRight className="w-5 h-5 text-slate-500" />
                            </div>
                        ))}
                    </div>
                </div>

                {/* Benefits */}
                <div className="mb-8">
                    <h3 className="text-white text-lg font-bold mb-4 uppercase tracking-wider text-capsure-lg">CAPSURE 멤버십 혜택</h3>
                    <div className="bg-capsure-card border border-slate-700/50 rounded-[20px] p-6 flex flex-col gap-6">
                        <div className="flex gap-4">
                            <div className="w-10 h-10 rounded-full bg-[#182F48] flex items-center justify-center flex-shrink-0 mt-0.5">
                                <Receipt className="w-5 h-5 text-brand-blue" />
                            </div>
                            <div>
                                <h4 className="text-white text-capsure-lg font-bold mb-1.5">Toss Point 적립</h4>
                                <p className="text-slate-400 text-capsure-base leading-relaxed">구독 유지 시 매월 결제 금액의 3%를 토스 포인트로 환급해드려요.</p>
                            </div>
                        </div>
                        <div className="w-full h-[1px] bg-slate-800/80" />
                        <div className="flex gap-4">
                            <div className="w-10 h-10 rounded-full bg-[#3D2C42] flex items-center justify-center flex-shrink-0 mt-0.5">
                                <Zap className="w-5 h-5 text-brand-light-purple" />
                            </div>
                            <div>
                                <h4 className="text-white text-capsure-lg font-bold mb-1.5">초스피드 청구 서비스</h4>
                                <p className="text-slate-400 text-capsure-base leading-relaxed">서류 사진만 찍으세요. AI가 3분 안에 청구를 완료합니다.</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Info Note */}
                <div className="bg-[#121622] rounded-[16px] p-4 flex items-center justify-between mb-8 cursor-pointer active:scale-[0.98] transition-transform">
                    <span className="text-slate-400 text-sm font-medium pl-2">상품 고시 사항</span>
                    <Info className="w-5 h-5 text-slate-500" />
                </div>
            </div>

            {/* Sticky Bottom Action */}
            <div className="fixed bottom-0 left-0 right-0 max-w-[560px] mx-auto bg-gradient-to-t from-[#020715] via-[#020715] to-transparent pt-12 px-6 pb-8 z-50">
                <button 
                    onClick={onAdd}
                    className={`w-full py-4 rounded-[16px] font-bold text-lg shadow-lg transition-all flex items-center justify-center gap-2 ${
                        isAdded 
                            ? 'bg-slate-700 text-white' 
                            : 'bg-brand-blue text-[#020715] hover:bg-[#6BC1E6]'
                    }`}
                >
                    {isAdded ? '이미 캡슐에 담겨있어요' : '캡슐에 담기'}
                </button>
            </div>
        </div>
    );
};

export default CapsureProductDetail;
