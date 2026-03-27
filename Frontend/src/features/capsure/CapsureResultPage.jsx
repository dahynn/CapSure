import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useCapsure } from './context/CapsureContext';
import { CheckCircle2, Calendar, Info, Home } from 'lucide-react';

const CapsureResultPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { selectedProducts } = useCapsure();

    // Calculate total price from previous state or context
    // In a real app, this would come from a backend response after subscription
    const totalPremium = selectedProducts.reduce((sum, p) => sum + Number(p.monthlyPrice || p.price || 0), 0);
    
    // Formatting currency
    const formattedPrice = new Intl.NumberFormat('ko-KR').format(totalPremium || 45000);

    // Coverage dates (Today ~ 1 month later)
    const today = new Date();
    const nextMonth = new Date();
    nextMonth.setMonth(today.getMonth() + 1);

    const formatDate = (date) => {
        return date.toISOString().split('T')[0].replace(/-/g, '.');
    };

    return (
        <div className="flex flex-col min-h-[calc(100vh-80px)] bg-[#020715] px-6 pt-12 pb-10">
            {/* Header / Icon */}
            <div className="flex flex-col items-center text-center mb-10">
                <div className="relative mb-6">
                    <div className="absolute inset-0 bg-brand-blue/20 blur-2xl rounded-full scale-150 animate-pulse" />
                    <CheckCircle2 className="w-20 h-20 text-brand-blue relative z-10" strokeWidth={1.5} />
                </div>
                <h1 className="text-2xl font-black text-white mb-3">캡슐 생성 및 결제 완료!</h1>
                <p className="text-slate-400 text-sm leading-relaxed max-w-[280px]">
                    새로운 보험 캡슐이 성공적으로 활성화되었습니다.
                </p>
            </div>

            {/* Subscription Details Container */}
            <div className="flex flex-col gap-4">
                {/* Total Fee Card */}
                <div className="bg-[#0D1526]/80 border border-slate-700/40 rounded-3xl p-6">
                    <span className="text-slate-500 font-bold text-[10px] tracking-widest uppercase mb-1 block">
                        TOTAL SUBSCRIPTION FEE
                    </span>
                    <div className="flex items-baseline gap-1">
                        <span className="text-brand-blue font-black text-2xl">{formattedPrice}</span>
                        <span className="text-white/60 font-bold text-lg">원</span>
                    </div>
                </div>

                {/* Period Card */}
                <div className="bg-[#0D1526]/80 border border-slate-700/40 rounded-3xl p-6">
                    <div className="flex items-center gap-3 mb-1">
                        <div className="w-10 h-10 bg-slate-800/60 rounded-xl flex items-center justify-center">
                            <Calendar className="w-5 h-5 text-brand-light-purple" />
                        </div>
                        <div className="flex flex-col">
                            <span className="text-slate-500 font-bold text-[10px] tracking-widest uppercase">
                                COVERAGE PERIOD
                            </span>
                            <span className="text-white font-bold text-base">
                                {formatDate(today)} ~ {formatDate(nextMonth)}
                            </span>
                        </div>
                    </div>
                    <div className="flex justify-between items-center mt-3 pt-3 border-t border-slate-800/50">
                        <span className="text-slate-500 text-xs font-medium">구독 기간</span>
                        <span className="text-brand-light-purple text-xs font-bold">한 달 간</span>
                    </div>
                </div>

                {/* Info Note */}
                <div className="bg-slate-800/20 border border-slate-800/60 rounded-2xl p-4 flex gap-3">
                    <Info className="w-5 h-5 text-brand-yellow flex-shrink-0 mt-0.5" />
                    <p className="text-slate-400 text-xs leading-relaxed">
                        구독은 한 달 뒤 자동 만료되며,<br />
                        <span className="text-white/80 font-bold">설정에 따라 갱신될 수 있습니다.</span>
                    </p>
                </div>
            </div>

            {/* Bottom Button */}
            <div className="mt-auto pt-10">
                <button
                    onClick={() => navigate('/home')}
                    className="w-full py-4 bg-brand-blue text-[#020715] rounded-2xl font-black text-lg flex items-center justify-center gap-2 hover:opacity-90 active:scale-[0.98] transition-all shadow-lg shadow-brand-blue/10"
                >
                    홈으로 가기
                </button>
            </div>
        </div>
    );
};

export default CapsureResultPage;
