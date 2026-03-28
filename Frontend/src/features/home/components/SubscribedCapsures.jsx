import React from 'react';
import { useNavigate } from 'react-router-dom';

const SubscribedCapsures = ({ subscribedCapsures }) => {
    const navigate = useNavigate();

    if (!subscribedCapsures || subscribedCapsures.length === 0) return null;

    return (
        <section className="animate-in slide-in-from-bottom-4 duration-700 delay-200 fill-mode-both pb-10">
            <div className="flex justify-between items-end mb-6 px-1">
                <h2 className="text-[22px] font-semibold text-white tracking-tight">나의 가입 캡슐</h2>
                <button 
                    onClick={() => navigate('/my-capsure')}
                    className="text-[14px] text-[#82D8FC] font-medium transition-opacity hover:opacity-80"
                >
                    전체보기
                </button>
            </div>

            <div className="space-y-5">
                {subscribedCapsures.map(capsure => (
                    <div key={capsure.id} className="relative rounded-[34px] p-[1.5px] overflow-hidden group shadow-lg">
                        {/* Animated Glowing Border Container */}
                        <div className="absolute top-1/2 left-1/2 w-[300%] h-[300%] -translate-x-1/2 -translate-y-1/2 pointer-events-none opacity-80">
                            <div 
                                className="w-full h-full animate-[spin_5s_linear_infinite]"
                                style={{
                                    background: `conic-gradient(from 0deg, transparent 0%, transparent 35%, ${capsure.themeColor} 50%, transparent 65%, transparent 100%)`,
                                    filter: 'blur(2px)'
                                }}
                            />
                        </div>
                        
                        {/* Inner Mask Wrapper */}
                        <div className="relative z-10 w-full h-full flex flex-col gap-1.5 rounded-[32.5px] bg-[#020715] overflow-hidden">
                            {/* Top part */}
                            <div className="rounded-t-[32px] rounded-b-[8px] bg-[#161B26] p-6 pb-7 relative overflow-hidden transition-colors border border-slate-700/50 border-b-0 z-10">
                                {/* Mixed Color Gradient Glow */}
                                <div 
                                    className="absolute inset-0 pointer-events-none opacity-50 mix-blend-screen"
                                    style={{
                                        background: 'radial-gradient(120% 150% at 0% 0%, rgba(246,205,60,0.12) 0%, rgba(130,216,252,0.15) 25%, rgba(242,190,247,0.1) 45%, transparent 80%)'
                                    }}
                                />
                                
                                <div className="flex justify-between items-start relative z-10">
                                    <div>
                                        <h3 className="text-xl md:text-[22px] font-semibold text-white mb-1 tracking-tight">{capsure.title}</h3>
                                        <p className="text-[#9D9DA4] text-[12px] font-medium flex items-center gap-1">
                                            가입일: <span style={{ color: capsure.themeColor }} className="font-bold">{capsure.date}</span>
                                        </p>
                                    </div>
                                    <button 
                                        onClick={() => navigate(`/mypage/capsule/${capsure.subscriptionId ?? capsure.id}`)}
                                        className="px-4 py-2 rounded-[14px] bg-slate-700/50 hover:bg-slate-700 text-white text-[13px] font-medium transition-colors border border-slate-600/50 active:scale-95"
                                    >
                                        상세보기
                                    </button>
                                </div>
                            </div>

                            {/* Bottom part (Tags) */}
                            <div className="rounded-b-[32px] rounded-t-[8px] bg-[#0A0E17] p-6 flex flex-wrap gap-2.5 border border-slate-800/80 border-t-0 z-10">
                                {capsure.coverages.map(cov => {
                                    const style = cov.isActive ? {
                                        borderColor: `${capsure.themeColor}66`,
                                        color: capsure.themeColor,
                                        backgroundColor: 'transparent'
                                    } : {};

                                    return (
                                        <span 
                                            key={cov.name} 
                                            style={style}
                                            className={`px-3 py-[5px] rounded-lg text-[13px] font-medium border transition-colors ${
                                                !cov.isActive ? 'border-slate-800/80 text-slate-500 bg-transparent' : ''
                                            }`}
                                        >
                                            {cov.name}
                                        </span>
                                    );
                                })}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
};

export default SubscribedCapsures;
