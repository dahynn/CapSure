import React from 'react';

const CapsureProgress = ({ currentAmount, totalBudget, progressPercent, remainingBudget }) => {
    return (
        <div className="flex flex-col items-center pt-8 pb-6 bg-gradient-to-b from-[#020715] to-[#0A0D14]">
            {/* Visual Capsule Graphic */}
            <div className="relative w-[84px] h-[168px] rounded-full border-4 border-slate-700/40 bg-capsure-card overflow-hidden shadow-[0_0_40px_rgba(130,216,252,0.1)] mb-6">
                {/* Glass reflection */}
                <div className="absolute inset-x-1 top-1 bottom-1 rounded-full border border-white/5 pointer-events-none z-20" />
                
                {/* Empty pill icon embedded in glass */}
                <div className="absolute top-6 left-1/2 -translate-x-1/2 opacity-20 rotate-45 z-10">
                    <div className="w-5 h-8 rounded-full border-2 border-white flex flex-col items-center justify-center" />
                    <div className="w-full h-0 border-t-2 border-white absolute top-1/2" />
                </div>

                {/* Fill Progress Liquid */}
                <div 
                    className="absolute bottom-0 left-0 right-0 bg-gradient-to-b from-[#4F687F] to-[#3A4D60] transition-all duration-700 ease-out z-10"
                    style={{ height: `${progressPercent}%` }}
                >
                    {/* Crisp liquid surface line */}
                    <div className="absolute top-0 left-0 right-0 h-[2px] bg-[#7A95AD] shadow-[0_1px_5px_rgba(255,255,255,0.1)]" />
                </div>
            </div>

            {/* Budget Info */}
            <div className="text-center w-full px-8">
                <p className="text-slate-400 text-capsure-base font-medium mb-1">현재 담은 금액</p>
                <h2 className="text-4xl font-black text-white mb-6 tracking-tight">
                    {currentAmount.toLocaleString()}원
                </h2>

                {/* Progress Bar Container */}
                <div className="w-full space-y-2">
                    <div className="flex justify-between text-micro font-bold">
                        <span className="text-brand-blue">진행률</span>
                        <span className="text-brand-blue">{Math.round(progressPercent)}%</span>
                    </div>
                    <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden">
                        <div 
                            className="h-full bg-gradient-to-r from-[#82D8FC] to-[#4A90E2] transition-all duration-500 rounded-full"
                            style={{ width: `${progressPercent}%` }}
                        />
                    </div>
                    <div className="text-center pt-2">
                        <span className="text-slate-400 text-capsure-sm">남은 예산: <span className="font-bold text-white">{remainingBudget.toLocaleString()}원</span></span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CapsureProgress;
