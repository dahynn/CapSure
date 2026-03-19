import React from 'react';
import { Receipt } from 'lucide-react';

const ActiveSubscription = () => {
    return (
        <div className="bg-[#161B26] rounded-3xl p-6 shadow-xl border border-slate-800 mb-8 relative overflow-hidden group">
            {/* Subtle Gradient Glow Background */}
            <div className="absolute top-0 right-0 w-64 h-64 bg-blue-500/5 rounded-full blur-3xl pointer-events-none transform translate-x-1/3 -translate-y-1/3 mix-blend-screen opacity-50 group-hover:opacity-100 transition-opacity duration-700" />
            
            <div className="relative z-10">
                <div className="flex justify-between items-center mb-6">
                    <span className="px-3 py-1 bg-[#1F2F4C] text-[#82D8FC] rounded-full text-xs font-bold tracking-wide border border-[#82D8FC] border-opacity-20 shadow-[0_0_10px_rgba(130,216,252,0.1)]">
                        Active Subscription
                    </span>
                    <span className="text-[13px] text-[#71717A] tracking-tight">결제 예정일: 2026.03.15</span>
                </div>

                <div className="mb-6">
                    <h3 className="text-[14px] text-[#9D9DA4] font-medium mb-1">이번 달 구독료</h3>
                    <div className="flex items-baseline gap-1">
                        <span className="text-[32px] md:text-[36px] font-black text-white tracking-tight leading-none">45,000</span>
                        <span className="text-[18px] font-bold text-white">원</span>
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <button className="flex-1 bg-[#82D8FC] hover:bg-[#6CCDF2] text-[#020715] font-bold py-3.5 px-4 rounded-xl transition-colors active:scale-95 text-[15px]">
                        청구 상세 보기
                    </button>
                    <button className="w-[50px] h-[50px] rounded-xl bg-[#1F2736] border border-slate-700 hover:border-slate-500 flex items-center justify-center transition-all group/btn">
                        <Receipt className="w-[22px] h-[22px] text-white flex-shrink-0 group-hover/btn:scale-110 transition-transform" />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ActiveSubscription;
