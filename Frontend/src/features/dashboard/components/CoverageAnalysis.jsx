import React from 'react';
import { Info, Sparkles } from 'lucide-react';

const CoverageAnalysis = () => {
    return (
        <div className="mb-8">
            <div className="flex justify-between items-center mb-4 px-1">
                <h2 className="text-[20px] font-bold text-white tracking-tight">보장 분석</h2>
                <button className="w-6 h-6 rounded-full bg-[#161B26] border border-slate-700 text-[#82D8FC] flex items-center justify-center transition-colors hover:bg-slate-800">
                    <Info className="w-3.5 h-3.5" strokeWidth={3} />
                </button>
            </div>

            <div className="bg-[#10141D] rounded-3xl p-6 shadow-lg border border-slate-800/80">
                <div className="flex justify-between items-end mb-4">
                    <div>
                        <p className="text-[13px] text-[#9D9DA4] font-medium mb-1">나의 보장 수준 백분위</p>
                        <div className="flex items-baseline gap-1">
                            <h3 className="text-[26px] font-black text-white tracking-tight">상위 </h3>
                            <span className="text-[26px] font-black text-[#F2BEF7]">15%</span>
                        </div>
                    </div>
                    <span className="text-[11px] font-black tracking-widest text-[#F6CD3C] pb-1 uppercase">Premium Grade</span>
                </div>

                {/* Custom Gradient Progress Bar */}
                <div className="w-full h-3.5 bg-[#1C212E] rounded-full mb-6 relative overflow-hidden">
                    <div 
                        className="absolute top-0 left-0 h-full rounded-full"
                        style={{ 
                            width: '85%', // 상위 15% means they beat 85%
                            background: 'linear-gradient(90deg, #82D8FC 0%, #F2BEF7 50%, #F6CD3C 100%)' 
                        }}
                    />
                </div>

                <div className="bg-[#191523] border border-[#2B233A] rounded-2xl p-4 flex gap-3 text-white">
                    <Sparkles className="w-5 h-5 text-[#F2BEF7] mt-0.5 flex-shrink-0" />
                    <p className="text-[13px] leading-snug text-[#BBBBCA]">
                        현재 적용된 <span className="font-bold text-[#F2BEF7]">보험 캡슐</span>은 한 달 동안 유지되며, 보장 범위가 최적화된 상태입니다.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default CoverageAnalysis;
