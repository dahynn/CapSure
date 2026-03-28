import React, { useEffect } from 'react';
import { ChevronLeft, Share2, ShieldCheck, Info, Zap, Receipt, Building2, Phone } from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';

const CapsureProductDetail = ({ product, onBack, onAdd, isAdded }) => {
    // Scroll to top on mount
    useEffect(() => {
        window.scrollTo(0, 0);
    }, []);

    const productName = product.productName;
    const price = product.monthlyPrice;
    const categoryLabel = product.categoryLabel;
    const companyName = product.companyName;

    const handleCopyLink = async () => {
        const currentUrl = window.location.href;
        try {
            await navigator.clipboard.writeText(currentUrl);
            alert('링크가 복사되었습니다.');
        } catch (error) {
            try {
                const textArea = document.createElement('textarea');
                textArea.value = currentUrl;
                textArea.style.position = 'fixed';
                textArea.style.opacity = '0';
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                alert('링크가 복사되었습니다.');
            } catch (fallbackError) {
                alert('링크 복사에 실패했습니다.');
            }
        }
    };

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
                <button
                    onClick={handleCopyLink}
                    className="p-2 text-white hover:bg-slate-800 rounded-full transition-colors"
                    aria-label="링크 복사"
                >
                    <Share2 className="w-5 h-5" />
                </button>
            </header>

            <div className="px-6 pt-6 flex-1 overflow-y-auto hide-scrollbar">
                {/* Title & Badges */}
                <div className="flex items-center gap-2 mb-4 flex-wrap">
                    <span className="bg-[#182F48] text-brand-blue text-capsure-sm font-bold px-3 py-1.5 rounded-lg border border-brand-blue/20 min-w-fit whitespace-nowrap">
                        {categoryLabel}
                    </span>
                    <span className="bg-slate-800 text-slate-400 text-capsure-sm font-bold px-3 py-1.5 rounded-lg border border-slate-700/50 min-w-fit whitespace-nowrap">
                        {product.insurerSector === 'LIFE' ? '생명보험' : '손해보험'}
                    </span>
                    <span className="bg-[#1C2C28] text-[#4ADE80] text-capsure-sm font-bold px-3 py-1.5 rounded-lg border border-[#4ADE80]/20 min-w-fit whitespace-nowrap">
                        {product.saleChannel}
                    </span>
                </div>
                
                <div className="flex items-center gap-1.5 mb-2 text-slate-400 font-bold text-capsure-base">
                    <Building2 className="w-4 h-4" />
                    {companyName}
                </div>
                <h2 className="text-white text-3xl font-black mb-2 leading-tight tracking-tight">
                    {productName}
                </h2>
                
                <div className="flex items-baseline gap-1.5 mb-10">
                    <span className="text-brand-blue text-2xl font-black tracking-tight">{price.toLocaleString()}</span>
                    <span className="text-slate-400 text-base font-medium">원/월</span>
                </div>

                {/* Core Coverage Box */}
                <div className="mb-10">
                    <div className="flex items-center gap-2 mb-4">
                        <ShieldCheck className="w-5 h-5 text-brand-purple" />
                        <h3 className="text-white text-lg font-bold">핵심 보장 정보</h3>
                    </div>
                    <div className="bg-capsure-card border border-slate-700/50 rounded-[20px] p-6 shadow-sm">
                        <div className="flex flex-col gap-6">
                            <div className="flex flex-col gap-1.5">
                                <span className="text-slate-500 text-capsure-sm font-bold">보담 항목</span>
                                <span className="text-white text-capsure-lg font-black">{product.coverageName || '기본 보장'}</span>
                            </div>
                            <div className="w-full h-[1px] bg-slate-800/80" />
                            <div className="flex flex-col gap-1.5">
                                <span className="text-slate-500 text-capsure-sm font-bold">지급 사유</span>
                                <span className="text-white text-capsure-lg font-bold leading-relaxed">{product.claimReason || '보험 약관에 정한 지급 사유 발생 시'}</span>
                            </div>
                            <div className="w-full h-[1px] bg-slate-800/80" />
                            <div className="flex flex-col gap-1.5">
                                <span className="text-slate-500 text-capsure-sm font-bold">지급 금액</span>
                                <span className="text-brand-blue text-2xl font-black">
                                    {product.payoutAmount || product.joinAmount || '상세 약관 참조'}
                                </span>
                                {(product.payoutAmount && product.joinAmount && product.payoutAmount !== product.joinAmount) && (
                                    <span className="text-slate-500 text-xs mt-1">가입금액: {product.joinAmount}</span>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Product Summary / Features */}
                {(product.productSummary || product.productFeature) && (
                    <div className="mb-10">
                        <div className="flex items-center gap-2 mb-4">
                            <Info className="w-5 h-5 text-brand-yellow" />
                            <h3 className="text-white text-lg font-bold">상품 특징</h3>
                        </div>
                        <div className="bg-[#121622] rounded-[20px] p-6 border border-slate-800/50">
                            <p className="text-slate-300 text-capsure-base leading-relaxed whitespace-pre-wrap">
                                {product.productSummary || product.productFeature}
                            </p>
                        </div>
                    </div>
                )}

                {/* Contact */}
                {product.contactPhone && (
                    <div className="bg-slate-900/50 rounded-[16px] p-4 flex items-center justify-between mb-8">
                        <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-slate-800 flex items-center justify-center">
                                <Phone className="w-4 h-4 text-slate-300" />
                            </div>
                            <span className="text-slate-300 text-capsure-base font-bold">가입 문의</span>
                        </div>
                        <span className="text-brand-blue font-black">{product.contactPhone}</span>
                    </div>
                )}

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
            </div>

            {/* Sticky Bottom Action */}
            <div className="fixed app-fixed-cta left-0 right-0 max-w-[560px] mx-auto bg-gradient-to-t from-[#020715] via-[#020715] to-transparent pt-12 px-6 pb-8 z-40">
                <AppButton
                    onClick={onAdd}
                    tone={isAdded ? 'subtle' : 'primary'}
                    className="rounded-[16px] text-lg shadow-lg"
                >
                    {isAdded ? '이미 캡슐에 담겨있어요' : '캡슐에 담기'}
                </AppButton>
            </div>
        </div>
    );
};

export default CapsureProductDetail;
