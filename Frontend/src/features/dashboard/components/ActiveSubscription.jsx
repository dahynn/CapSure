import React from 'react';
import { Receipt } from 'lucide-react';

const ActiveSubscription = () => {
  return (
    <div className="group relative mb-8 overflow-hidden rounded-3xl border border-slate-800 bg-[#161B26] p-6 shadow-xl">
      {/* Subtle Gradient Glow Background */}
      <div className="pointer-events-none absolute right-0 top-0 h-64 w-64 -translate-y-1/3 translate-x-1/3 transform rounded-full bg-blue-500/5 opacity-50 mix-blend-screen blur-3xl transition-opacity duration-700 group-hover:opacity-100" />

      <div className="relative z-10">
        {/* Header removed as per user request */}

        <div className="mb-6">
          <h3 className="mb-1 text-[14px] font-medium text-[#9D9DA4]">이번 달 구독료</h3>
          <div className="flex items-baseline gap-1">
            <span className="text-[32px] font-black leading-none tracking-tight text-white md:text-[36px]">
              45,000
            </span>
            <span className="text-[18px] font-bold text-white">원</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button className="flex-1 rounded-xl bg-[#82D8FC] px-4 py-3.5 text-[15px] font-bold text-[#020715] transition-colors hover:bg-[#6CCDF2] active:scale-95">
            청구 상세 보기
          </button>
        </div>
      </div>
    </div>
  );
};

export default ActiveSubscription;
