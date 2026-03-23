import React from 'react';
import { Pill, AlertOctagon, Car, Timer } from 'lucide-react';

const ScheduleSummary = () => {
    return (
        <div className="mb-10">
            <div className="flex justify-between items-center mb-6 px-1">
                <h2 className="text-[20px] font-bold text-white tracking-tight">일정 요약</h2>
                <button className="text-[13px] text-[#82D8FC] font-medium transition-opacity hover:opacity-80">
                    전체보기
                </button>
            </div>

            <div className="space-y-4">
                {/* Health Capsule */}
                <div className="bg-[#10141D] rounded-3xl p-5 shadow-sm border border-slate-800 flex items-center justify-between group cursor-pointer hover:border-slate-700 transition-colors">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-[#1C212E] rounded-full flex items-center justify-center text-[#82D8FC] flex-shrink-0">
                            <Pill className="w-[22px] h-[22px]" />
                        </div>
                        <div className="flex flex-col">
                            <h4 className="text-[15px] font-bold text-white tracking-tight mb-0.5">종합 건강 캡슐 A-12</h4>
                            <p className="text-[12px] text-[#9D9DA4]">만기일: 2026.04.10</p>
                        </div>
                    </div>
                    <span className="text-[13px] font-bold text-slate-300">D-24</span>
                </div>

                {/* Fire Insurance */}
                <div className="bg-[#10141D] rounded-3xl p-5 shadow-sm border border-slate-800 flex items-center justify-between group cursor-pointer hover:border-slate-700 transition-colors">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-[#2A1D2B] rounded-full flex items-center justify-center text-[#F2BEF7] flex-shrink-0">
                            <AlertOctagon className="w-[22px] h-[22px]" />
                        </div>
                        <div className="flex flex-col">
                            <h4 className="text-[15px] font-bold text-white tracking-tight mb-0.5">주택 화재 안심 캡슐</h4>
                            <p className="text-[12px] text-[#9D9DA4]">자동 갱신일: 2026.03.15</p>
                        </div>
                    </div>
                    <span className="text-[13px] font-bold text-[#82D8FC]">갱신예정</span>
                </div>

                {/* Daily Auto Insurance */}
                <div className="bg-[#10141D] rounded-3xl p-5 shadow-sm border border-slate-800 flex items-center justify-between group cursor-pointer hover:border-slate-700 transition-colors">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-[#2B2516] rounded-full flex items-center justify-center text-[#F6CD3C] flex-shrink-0">
                            <Car className="w-[22px] h-[22px]" />
                        </div>
                        <div className="flex flex-col">
                            <h4 className="text-[15px] font-bold text-white tracking-tight mb-0.5">단기 운전자 보호 (24h)</h4>
                            <p className="text-[12px] text-[#9D9DA4]">종료일: 2026.03.02</p>
                        </div>
                    </div>
                    <Timer className="w-5 h-5 text-[#F6CD3C]" />
                </div>
            </div>
        </div>
    );
};

export default ScheduleSummary;
