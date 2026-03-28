import React, { useState, useEffect } from 'react';

const CategoryRecommend = ({ recommendations, onViewDetail }) => {
    const [currentIndex, setCurrentIndex] = useState(0);

    useEffect(() => {
        if (!recommendations || recommendations.length === 0) return;
        const interval = setInterval(() => {
            setCurrentIndex((prev) => (prev + 1) % recommendations.length);
        }, 3500); // 3.5초 주기
        return () => clearInterval(interval);
    }, [recommendations]);

    if (!recommendations || recommendations.length === 0) return null;

    return (
        <section className="animate-in slide-in-from-bottom-4 duration-700 delay-100 fill-mode-both pb-6">
            <div className="relative w-full grid">
                {recommendations.map((rec, index) => (
                    <div 
                        key={rec.id} 
                        style={{ gridArea: '1 / 1 / 2 / 2' }}
                        className={`w-full bg-[#161B26] rounded-3xl p-6 relative overflow-hidden shadow-xl border border-slate-800 transition-all duration-700 ease-in-out ${
                            index === currentIndex ? 'opacity-100 z-10' : 'opacity-0 scale-95 z-0 pointer-events-none'
                        }`}
                    >
                        <div 
                            className="absolute inset-0 opacity-10 pointer-events-none" 
                            style={{ background: `linear-gradient(to bottom right, ${rec.badgeColor}, transparent)` }}
                        />
                        <div className="relative z-10 flex flex-col h-full">
                            <div className="mb-4">
                                <span 
                                    className="inline-flex items-center px-3 py-1.5 bg-[#1F2736] text-[10px] font-black tracking-wider rounded-lg shadow-sm border"
                                    style={{ color: rec.badgeColor, borderColor: `${rec.badgeColor}33` }}
                                >
                                    CATEGORY RECOMMEND
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
                                    onClick={() => onViewDetail?.(rec.productSourceId ?? rec.id)}
                                    className="px-5 py-2.5 text-[#020715] text-sm font-bold rounded-xl active:scale-95 transition-all"
                                    style={{ 
                                        backgroundColor: rec.btnColor,
                                        boxShadow: `0 4px 12px ${rec.btnColor}4D`
                                    }}
                                >
                                    {rec.btnText}
                                </button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
};

export default CategoryRecommend;
