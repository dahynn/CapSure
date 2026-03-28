import React, { useState, useEffect } from 'react';

const TONE_STYLES = {
    blue: {
        overlay: 'bg-gradient-to-br from-brand-blue/20 to-transparent',
        badgeText: 'text-brand-blue',
        badgeBorder: 'border-brand-blue/30',
        buttonBg: 'bg-brand-blue',
        buttonShadow: 'shadow-[0_4px_12px_rgba(130,216,252,0.3)]',
    },
    yellow: {
        overlay: 'bg-gradient-to-br from-brand-yellow/20 to-transparent',
        badgeText: 'text-brand-yellow',
        badgeBorder: 'border-brand-yellow/30',
        buttonBg: 'bg-brand-yellow',
        buttonShadow: 'shadow-[0_4px_12px_rgba(246,205,60,0.3)]',
    },
    purple: {
        overlay: 'bg-gradient-to-br from-brand-purple/20 to-transparent',
        badgeText: 'text-brand-purple',
        badgeBorder: 'border-brand-purple/30',
        buttonBg: 'bg-brand-purple',
        buttonShadow: 'shadow-[0_4px_12px_rgba(242,190,247,0.3)]',
    },
    gray: {
        overlay: 'bg-gradient-to-br from-brand-gray/20 to-transparent',
        badgeText: 'text-brand-gray',
        badgeBorder: 'border-brand-gray/30',
        buttonBg: 'bg-brand-gray',
        buttonShadow: 'shadow-[0_4px_12px_rgba(157,157,164,0.3)]',
    },
};

const CategoryRecommend = ({ recommendations, onViewDetail }) => {
    const [currentIndex, setCurrentIndex] = useState(0);
    const safeRecommendations = recommendations && recommendations.length > 0
        ? recommendations
        : [
            {
                id: 'fallback-recommend',
                productSourceId: null,
                title: '카테고리 추천 준비 중',
                desc: '추천 데이터를 불러오는 중입니다. 잠시 후 다시 확인해 주세요.',
                tone: 'blue',
                btnText: '새로고침',
                isFallback: true,
            },
        ];

    useEffect(() => {
        if (!recommendations || recommendations.length === 0) return;
        const interval = setInterval(() => {
            setCurrentIndex((prev) => (prev + 1) % safeRecommendations.length);
        }, 6500); // 6.5초 주기
        return () => clearInterval(interval);
    }, [recommendations, safeRecommendations.length]);

    return (
        <section className="animate-in slide-in-from-bottom-4 duration-700 delay-100 fill-mode-both pb-6">
            <div className="relative w-full grid">
                {safeRecommendations.map((rec, index) => {
                    const tone = TONE_STYLES[rec.tone] ?? TONE_STYLES.blue;
                    return (
                        <div
                            key={rec.id}
                            style={{ gridArea: '1 / 1 / 2 / 2' }}
                            className={`w-full bg-[#161B26] rounded-3xl p-6 relative overflow-hidden shadow-xl border border-slate-800 transition-all duration-900 ease-in-out ${
                                index === currentIndex ? 'opacity-100 z-10' : 'opacity-0 scale-95 z-0 pointer-events-none'
                            }`}
                        >
                            <div className={`absolute inset-0 opacity-10 pointer-events-none ${tone.overlay}`} />
                            <div className="relative z-10 flex flex-col h-full">
                                <div className="mb-4">
                                    <span
                                        className={`inline-flex items-center px-3 py-1.5 bg-[#1F2736] text-[10px] font-black tracking-wider rounded-lg shadow-sm border ${tone.badgeText} ${tone.badgeBorder}`}
                                    >
                                        AI 추천
                                    </span>
                                </div>
                                <h3 className="text-[20px] md:text-[22px] font-bold text-white leading-snug tracking-tight mb-2 whitespace-pre-line">
                                    {rec.title}
                                </h3>
                                <p className="text-[13px] text-[#9D9DA4] mb-8">
                                    {rec.desc}
                                </p>
                                <div className="flex justify-end mt-auto">
                                    <button
                                        onClick={() => {
                                            if (rec.isFallback) {
                                                window.location.reload();
                                                return;
                                            }
                                            onViewDetail?.(rec.productSourceId ?? rec.id);
                                        }}
                                        className={`px-5 py-2.5 text-[#020715] text-sm font-bold rounded-xl transition-all ${tone.buttonBg} ${tone.buttonShadow} ${rec.isFallback ? '' : 'active:scale-95'}`}
                                    >
                                        {rec.btnText}
                                    </button>
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>
        </section>
    );
};

export default CategoryRecommend;
