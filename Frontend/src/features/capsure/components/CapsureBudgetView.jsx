import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Wallet, Shield, ArrowRight } from 'lucide-react';

const CapsureBudgetView = ({ onProceed }) => {
    const navigate = useNavigate();
    const [budgetInput, setBudgetInput] = useState("100000");

    return (
        <div className="flex flex-col min-h-screen">
            <header className="flex items-center p-4 min-h-[56px] relative">
                <button onClick={() => navigate(-1)} className="p-2 text-white"><ChevronLeft className="w-6 h-6"/></button>
            </header>

            <div className="px-6 pt-6 animate-in slide-in-from-right-8 duration-300">
                <h2 className="text-2xl font-black text-white leading-snug mb-2">이번 달 보험료로<br/>얼마가 적당할까요?</h2>
                <p className="text-slate-400 text-sm mb-8">맞춤형 캡슐 설계를 위해 목표 예산을 알려주세요.</p>

                <p className="text-slate-400 text-capsure-sm mb-2">목표 예산</p>
                <div className="flex items-center justify-between border-2 rounded-xl px-5 py-4 transition-colors border-slate-700 bg-[#0A0D14] focus-within:border-brand-blue focus-within:bg-capsure-card">
                    <input
                        type="number"
                        value={budgetInput}
                        onChange={(e) => setBudgetInput(e.target.value)}
                        className="w-full bg-transparent text-3xl font-bold tracking-tight text-white outline-none"
                        placeholder="0"
                    />
                    <span className="text-white font-bold ml-2">원</span>
                </div>

                {/* Quick Amounts */}
                <div className="flex gap-2.5 mt-4 overflow-x-auto hide-scrollbar">
                    {[30000, 50000, 100000, 150000].map(amt => (
                        <button
                            key={amt}
                            onClick={() => setBudgetInput(amt.toString())}
                            className={`px-5 py-2 whitespace-nowrap rounded-full text-capsure-base font-bold border transition-colors outline-none ${budgetInput === amt.toString() ? 'border-brand-blue text-brand-blue bg-brand-blue/10' : 'border-slate-700 text-slate-400 bg-capsure-card hover:border-slate-500 hover:text-slate-300'}`}
                        >
                            {amt / 10000}만원
                        </button>
                    ))}
                </div>
            </div>

            {/* Static Spinning Graphic Area */}
            <div className="flex-1 flex flex-col items-center justify-center mt-4 animate-in fade-in duration-500 pb-20">
                {/* Spinning Graphic Container */}
                <div className="relative w-[84px] h-[168px] mb-8 mt-4">
                    {/* The Capsule Outline */}
                    <div className="w-full h-full rounded-full border-[3px] border-slate-600 bg-transparent flex flex-col items-center justify-center relative shadow-[0_0_40px_rgba(130,216,252,0.05)] z-10">
                        {/* Static Inner content */}
                        <div className="absolute w-[86%] h-[92%] left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2">
                            <div className="w-full h-full flex flex-col items-center justify-between py-1">
                                {/* Top Icon */}
                                <div className="w-[60px] h-[60px] rounded-full bg-[#3D2C42] flex items-center justify-center">
                                    <Wallet className="w-6 h-6 text-brand-light-purple" strokeWidth={2.5} />
                                </div>
                                {/* Divider Dashes */}
                                <div className="flex items-center justify-center gap-[5px] py-1">
                                    {[1,2,3,4,5].map(i => <div key={i} className="w-[5px] h-[2px] bg-slate-600 rounded-full" />)}
                                </div>
                                {/* Bottom Icon */}
                                <div className="w-[60px] h-[60px] rounded-full bg-[#182F48] flex items-center justify-center">
                                    <Shield className="w-6 h-6 text-brand-blue" strokeWidth={2.5} />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Floating spinning dots decoration */}
                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[180px] h-[180px] pointer-events-none z-20">
                        <div className="w-full h-full animate-spin-slow relative">
                            <div className="absolute top-4 right-6 w-2.5 h-2.5 rounded-full bg-[#8c7426] shadow-[0_0_10px_rgba(212,174,56,0.6)]" />
                            <div className="absolute bottom-6 left-6 w-2 h-2 rounded-full bg-brand-purple shadow-[0_0_10px_rgba(242,190,247,0.6)]" />
                        </div>
                    </div>
                </div>
                
                <p className="text-center text-slate-400 text-capsure-base leading-relaxed">
                    설정하신 예산 내에서<br/>최적의 보장 항목을 캡슐에 담아드릴게요.
                </p>
            </div>

            {/* Sticky Bottom Button */}
            <div className="fixed bottom-[72px] left-0 right-0 max-w-[560px] mx-auto pt-16 px-6 pb-8 z-40">
                <button 
                    onClick={() => {
                        onProceed(Number(budgetInput) || 100000);
                    }}
                    className="w-full py-4 rounded-xl font-bold text-[#020715] text-base bg-brand-blue shadow-[0_0_20px_rgba(130,216,252,0.2)] hover:bg-[#6BC1E6] active:scale-[0.98] transition-all flex justify-center items-center gap-2"
                >
                    캡슐 설계 시작하기
                    <ArrowRight className="w-5 h-5" strokeWidth={3} />
                </button>
                <div className="flex justify-center flex-wrap gap-x-6 gap-y-2 mt-5 text-slate-500 text-capsure-sm font-bold">
                    <span className="flex items-center gap-1.5"><Shield className="w-3.5 h-3.5"/> 안전한 보안 진단</span>
                    <span className="flex items-center gap-1.5">⚡ 3초 빠른 설계</span>
                </div>
            </div>
        </div>
    );
};

export default CapsureBudgetView;
