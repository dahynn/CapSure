import React from 'react';
import { useNavigate } from 'react-router-dom';
import { MoreVertical } from 'lucide-react';

const ActiveInsurances = ({ activeInsurances }) => {
    const navigate = useNavigate();
    if (!activeInsurances || activeInsurances.length === 0) return null;

    return (
        <section className="animate-in slide-in-from-bottom-4 duration-700 delay-300 fill-mode-both">
            <div className="mb-6 px-1">
                <h2 className="text-[22px] font-bold text-white tracking-tight">현재 진행 중인 보험</h2>
            </div>

            <div className="flex gap-4 overflow-x-auto hide-scrollbar pb-6 snap-x snap-mandatory w-full">
                {activeInsurances.map((ins) => (
                    <div key={ins.id} className="flex-shrink-0 w-[260px] md:w-[300px] bg-[#161B26] rounded-3xl p-6 relative overflow-hidden snap-start shadow-xl border border-slate-800 flex flex-col hover:border-slate-700 transition-colors">
                        <div className="flex justify-between items-start mb-6">
                            <span
                                className="px-3 py-1.5 rounded-lg text-xs font-bold tracking-wide"
                                style={{
                                    color: ins.statusColor,
                                    backgroundColor: ins.statusColor + '1A'
                                }}
                            >
                                {ins.status}
                            </span>
                            <button
                                onClick={() => navigate(`/capsure-insurance/detail/${ins.productSourceId}`)}
                                className="text-slate-500 hover:text-white transition-colors"
                                aria-label="보험 상세 보기"
                            >
                                <MoreVertical className="w-5 h-5" />
                            </button>
                        </div>

                        <div className="mb-8">
                            <h3 className="text-[17px] font-bold text-white mb-2">{ins.productName}</h3>
                            <p className="text-[13px] text-[#9D9DA4]">월 납입일: {ins.paymentDay}일</p>
                        </div>

                        <div className="flex justify-between items-end mt-auto pt-4">
                            <span className="text-[13px] text-[#9D9DA4] mb-1">월 보험료</span>
                            <span className="text-[24px] font-bold text-white">
                                {ins.monthlyPremium.toLocaleString()}원
                            </span>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
};

export default ActiveInsurances;
