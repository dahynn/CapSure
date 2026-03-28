import React from 'react';
import { ArrowRight, CreditCard, Loader2, ShieldCheck } from 'lucide-react';

const Payment = ({ totalPremium, isSubmitting, onNext, onPrev }) => {
    return (
        <div className="max-w-md mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4">
            <div className="text-center py-6">
                <h2 className="text-3xl font-black text-slate-800 tracking-tight">마지막 단계입니다</h2>
                <p className="text-slate-500 mt-3 text-lg">결제 수단을 선택하고 구독을 시작하세요.</p>
            </div>

            <div className="bg-white p-6 rounded-3xl shadow-xl border border-slate-100">
                <p className="text-sm font-semibold text-slate-500">이번 달 총 결제 금액</p>
                <p className="text-3xl font-black text-slate-800 mt-2">{totalPremium.toLocaleString()}원</p>
            </div>

            <div className="bg-white p-8 rounded-3xl shadow-xl border border-slate-100 flex flex-col items-center gap-8">
                <div className="w-20 h-20 bg-blue-50 text-blue-500 rounded-full flex items-center justify-center">
                    <CreditCard className="w-10 h-10" />
                </div>

                <div className="space-y-4 w-full">
                    {/* Only Toss Payment Button is available for now */}
                    <button
                        onClick={onNext}
                        disabled={isSubmitting}
                        className="w-full flex items-center justify-between p-5 rounded-2xl border-2 border-[#0064FF] bg-[#F2F6FF] hover:bg-[#E5EDFF] transition-colors"
                    >
                        <div className="flex items-center gap-3">
                            {/* Toss Logo Mock */}
                            <div className="w-8 h-8 rounded-full bg-[#0064FF] flex items-center justify-center text-white font-black text-xs">
                                toss
                            </div>
                            <span className="font-bold text-[#0064FF] text-lg">
                                {isSubmitting ? '결제 처리 중...' : '토스 결제하기'}
                            </span>
                        </div>
                        {isSubmitting ? (
                            <Loader2 className="w-5 h-5 text-[#0064FF] animate-spin" />
                        ) : (
                            <ArrowRight className="w-5 h-5 text-[#0064FF]" />
                        )}
                    </button>

                    <button disabled className="w-full flex items-center justify-start p-5 rounded-2xl border border-slate-200 bg-slate-50 text-slate-400 cursor-not-allowed">
                        <span className="font-bold">신용/체크카드 (준비 중)</span>
                    </button>

                    <button disabled className="w-full flex items-center justify-start p-5 rounded-2xl border border-slate-200 bg-slate-50 text-slate-400 cursor-not-allowed">
                        <span className="font-bold">카카오페이 (준비 중)</span>
                    </button>
                </div>

                <div className="flex items-center gap-2 text-sm text-slate-400 font-medium mt-4">
                    <ShieldCheck className="w-4 h-4" />
                    안전하게 결제가 진행됩니다.
                </div>
            </div>

            <div className="flex justify-center pt-4">
                <button
                    onClick={onPrev}
                    className="px-6 py-3 text-slate-500 hover:text-slate-700 font-medium transition-colors"
                >
                    이전으로
                </button>
            </div>
        </div>
    );
};

export default Payment;
