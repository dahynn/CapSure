import React, { useState } from 'react';
import {
    Droplets,
    HeartPulse,
    Scissors,
    Shield,
    ShieldAlert,
    TriangleAlert,
    Zap,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';

const STACK_CARDS = [
    { key: 'brain-heart', label: '뇌/심장', icon: HeartPulse, tone: 'purple', position: 'left-0 top-0' },
    { key: 'injury', label: '상해', icon: ShieldAlert, tone: 'blue', position: 'right-0 top-0' },
    { key: 'cancer', label: '암', icon: Zap, tone: 'yellow', position: 'left-0 top-[96px]' },
    { key: 'death', label: '사망', icon: TriangleAlert, tone: 'blue', position: 'left-0 top-[192px]' },
    { key: 'actual-loss', label: '실손', icon: Droplets, tone: 'purple', position: 'left-[100px] top-[192px]' },
    { key: 'surgery', label: '수술', icon: Scissors, tone: 'yellow', position: 'right-0 top-[192px]' },
];

const STACK_TONE_STYLES = {
    blue: {
        card: 'border-[#3a86c6] bg-transparent',
        iconWrap: 'bg-[#142740]',
        icon: 'text-[#82D8FC]',
    },
    purple: {
        card: 'border-[#6e5a9c] bg-transparent',
        iconWrap: 'bg-[#2c2442]',
        icon: 'text-[#F2BEF7]',
    },
    yellow: {
        card: 'border-[#8f7a27] bg-transparent',
        iconWrap: 'bg-[#272417]',
        icon: 'text-[#F6CD3C]',
    },
};

const StackCard = ({ label, icon: Icon, tone = 'blue', position, motionClass = '' }) => {
    const style = STACK_TONE_STYLES[tone] ?? STACK_TONE_STYLES.blue;

    return (
        <article
            className={`absolute z-20 ${position} w-[90px] h-[90px] rounded-[28px] border ${style.card} shadow-[0_8px_20px_rgba(0,0,0,0.28)] flex flex-col items-center justify-center ${motionClass}`}
        >
            <div className={`w-10 h-10 rounded-[11px] ${style.iconWrap} flex items-center justify-center`}>
                <Icon className={`w-[18px] h-[18px] ${style.icon}`} />
            </div>
            <p className="mt-1 text-white text-[12px] font-bold">{label}</p>
        </article>
    );
};

const CapsureBudgetView = ({ onProceed }) => {
    const [budgetInput, setBudgetInput] = useState('10000');

    return (
        <div
            className="flex flex-col min-h-screen"
            style={{ paddingBottom: 'calc(var(--app-bottom-nav-height) + env(safe-area-inset-bottom) + 122px)' }}
        >
            <div className="px-6 pt-6 animate-in slide-in-from-right-8 duration-300">
                <h2 className="text-2xl font-black text-white leading-snug mb-2">
                    이번 달 보험료로
                    <br />
                    얼마가 적당할까요?
                </h2>
                <p className="text-slate-400 text-sm mb-8">맞춤형 캡슐 설계를 위해 목표 예산을 알려주세요.</p>

                <p className="text-slate-400 text-capsure-sm mb-2">목표 예산</p>
                <div className="flex items-center justify-between border-2 rounded-xl px-5 py-4 transition-colors border-slate-700 bg-[#0A0D14] focus-within:border-brand-blue focus-within:bg-capsure-card">
                    <input
                        type="number"
                        value={budgetInput}
                        onChange={(e) => setBudgetInput(e.target.value)}
                        className="w-full bg-transparent text-3xl font-bold tracking-tight text-white outline-none no-spinner"
                        placeholder="0"
                    />
                    <span className="text-white font-bold ml-2">원</span>
                </div>

                <div className="flex gap-2.5 mt-4 overflow-x-auto hide-scrollbar">
                    {[10000, 30000, 50000, 70000].map((amt) => (
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

            <div className="flex flex-col items-center mt-7 mb-2 animate-in fade-in duration-500">
                <div className="relative w-[290px] h-[290px] mb-6">
                    {STACK_CARDS.map((card) => (
                        <StackCard
                            key={card.key}
                            label={card.label}
                            icon={card.icon}
                            tone={card.tone}
                            position={card.position}
                            motionClass={card.motionClass}
                        />
                    ))}

                    <div className="absolute z-0 left-[100px] top-[96px] w-[90px] h-[90px] rounded-[28px] border border-dashed border-slate-300/80 flex items-center justify-center bg-[#0b1322] shadow-[0_8px_20px_rgba(0,0,0,0.26)]">
                        <span className="text-[40px] leading-none font-black text-white/90">?</span>
                    </div>
                </div>

                <p className="text-center text-slate-400 text-capsure-base leading-relaxed">
                    설정하신 예산 내에서
                    <br />
                    최적의 보장 항목을 캡슐에 담아드릴게요.
                </p>
            </div>

            <div
                className="fixed left-0 right-0 max-w-[560px] mx-auto px-6 pb-4 pt-6 bg-gradient-to-t from-[#020715] via-[#020715] to-transparent z-40"
                style={{ bottom: 'calc(var(--app-bottom-nav-height) + env(safe-area-inset-bottom) + 2px)' }}
            >
                <div className="flex justify-center flex-wrap gap-x-6 gap-y-2 mb-3 text-slate-500 text-capsure-sm font-bold">
                    <span className="flex items-center gap-1.5">
                        <Shield className="w-3.5 h-3.5" /> 안전한 보안 진단
                    </span>
                    <span className="flex items-center gap-1.5">⚡ 3초 빠른 설계</span>
                </div>
                <AppButton
                    onClick={() => {
                        onProceed(Number(budgetInput) || 10000);
                    }}
                    className="shadow-[0_0_20px_rgba(130,216,252,0.2)]"
                >
                    캡슐 설계 시작하기
                </AppButton>
            </div>
        </div>
    );
};

export default CapsureBudgetView;
