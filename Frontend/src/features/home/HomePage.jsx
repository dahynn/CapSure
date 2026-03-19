import React from "react";
import { useNavigate } from "react-router-dom";
import {
    Flame,
    HeartPulse,
    Dog,
    Car,
    ArrowRight,
    ShieldCheck,
    MoreVertical,
} from "lucide-react";

const HomePage = () => {
    const navigate = useNavigate();

    // Mock user data (백엔드 연동 시 이 state를 API 응답으로 대체)
    const [user] = React.useState({
        name: '정정교',
    });

    // Mock AI Recommendations
    const [aiRecommendations] = React.useState([
        {
            id: 1,
            badgeColor: "#82D8FC",
            title: "운전자 보험\n업그레이드 제안",
            desc: "현재 보장 대비 24% 효율 증가",
            btnColor: "#82D8FC",
            btnText: "분석하기"
        },
        {
            id: 2,
            badgeColor: "#F6CD3C",
            title: "암 보험\n맞춤형 플랜",
            desc: "내 가족력 기반 최적의 보장",
            btnColor: "#F6CD3C",
            btnText: "알아보기"
        }
    ]);

    const [currentAiIndex, setCurrentAiIndex] = React.useState(0);

    React.useEffect(() => {
        const interval = setInterval(() => {
            setCurrentAiIndex((prev) => (prev + 1) % aiRecommendations.length);
        }, 3500); // 3.5초 주기
        return () => clearInterval(interval);
    }, [aiRecommendations.length]);

    // Mock subscribed capsures (백엔드 연동 시 API 응답으로 대체)
    const [subscribedCapsures] = React.useState([
        {
            id: 1,
            title: "나의 든든한 일상",
            date: "2023.10.12",
            themeColor: "#82D8FC", // 브랜드 블루
            coverages: [
                { name: "실손", isActive: true },
                { name: "상해", isActive: true },
                { name: "배상", isActive: true },
                { name: "사망", isActive: false },
                { name: "암", isActive: true },
                { name: "수술", isActive: true },
                { name: "뇌/심장", isActive: false }
            ]
        },
        {
            id: 2,
            title: "우리 가족 건강 지키미",
            date: "2024.01.05",
            themeColor: "#F6CD3C", // 옐로우
            coverages: [
                { name: "사망", isActive: true },
                { name: "암", isActive: true },
                { name: "뇌/심장", isActive: true },
                { name: "실손", isActive: true },
                { name: "수술", isActive: true },
                { name: "상해", isActive: false },
                { name: "배상", isActive: false }
            ]
        }
    ]);

    // Mock active insurances (백엔드 연동 시 대체)
    const [activeInsurances] = React.useState([
        {
            id: 1,
            status: "정상 유지",
            statusColor: "#82D8FC", // Blue
            productName: "카카오 정기 보험",
            paymentDay: 15,
            monthlyPremium: 45000
        },
        {
            id: 2,
            status: "납입 대기",
            statusColor: "#F6CD3C", // Yellow
            productName: "현대해상 실손의료비",
            paymentDay: 25,
            monthlyPremium: 12000
        }
    ]);

    return (
        <div className="px-8 py-8 md:px-12 md:py-10 space-y-12 max-w-[560px] mx-auto w-full transition-all">
            {/* Section 1: Welcome Header */}
            <section className="animate-in slide-in-from-top-4 duration-500">
                <h1 className="text-[28px] md:text-3xl font-bold text-white leading-tight tracking-tight">
                    {user.name} 님,
                </h1>
                <p className="text-[15px] mt-2" style={{ color: 'var(--color-brand-gray)' }}>
                    오늘도 당신의 일상을 안전하게 보관 중입니다.
                </p>
            </section>

            {/* Section 2: AI Recommendation Auto-slider */}
            <section className="animate-in slide-in-from-bottom-4 duration-700 delay-100 fill-mode-both pb-6">
                <div className="relative w-full grid">
                    {aiRecommendations.map((rec, index) => (
                        <div 
                            key={rec.id} 
                            style={{ gridArea: '1 / 1 / 2 / 2' }}
                            className={`w-full bg-[#161B26] rounded-3xl p-6 relative overflow-hidden shadow-xl border border-slate-800 transition-all duration-700 ease-in-out ${
                                index === currentAiIndex ? 'opacity-100 z-10' : 'opacity-0 scale-95 z-0 pointer-events-none'
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
                                        PREMIUM AI
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

            {/* Section 3: Subscribed Capsures */}
            <section className="animate-in slide-in-from-bottom-4 duration-700 delay-200 fill-mode-both pb-10">
                <div className="flex justify-between items-end mb-6 px-1">
                    <h2 className="text-[22px] font-bold text-white tracking-tight">나의 가입 캡슐</h2>
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
                                        filter: 'blur(2px)' // 부드러운 빛 효과
                                    }}
                                />
                            </div>
                            
                            {/* Inner Mask Wrapper to cover the exact capsule shape */}
                            <div className="relative z-10 w-full h-full flex flex-col gap-1.5 rounded-[32.5px] bg-[#020715] overflow-hidden">
                                {/* Top part (Outer Top Rounded, Inner Flat) */}
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
                                            <h3 className="text-xl md:text-[22px] font-bold text-white mb-2 tracking-tight">{capsure.title}</h3>
                                            <p 
                                                className="text-[12px] font-bold tracking-widest uppercase font-mono" 
                                                style={{ color: capsure.themeColor }}
                                            >
                                                EST. {capsure.date}
                                            </p>
                                        </div>
                                        <button 
                                            onClick={() => navigate('/my-capsure')}
                                            className="px-4 py-2 rounded-[14px] bg-slate-700/50 hover:bg-slate-700 text-white text-[13px] font-medium transition-colors border border-slate-600/50 active:scale-95"
                                        >
                                            상세보기
                                        </button>
                                    </div>
                                </div>

                                {/* Bottom part (Tags: Outer Bottom Rounded, Inner Flat) */}
                                <div className="rounded-b-[32px] rounded-t-[8px] bg-[#0A0E17] p-6 flex flex-wrap gap-2.5 border border-slate-800/80 border-t-0 z-10">
                                    {capsure.coverages.map(cov => {
                                        // Make background transparent per user request ("색 없이")
                                        const style = cov.isActive ? {
                                            borderColor: '#82D8FC66',
                                            color: '#82D8FC',
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

            {/* Section 4: Active Insurances Carousel */}
            <section className="animate-in slide-in-from-bottom-4 duration-700 delay-300 fill-mode-both">
                <div className="mb-6 px-1">
                    <h2 className="text-[22px] font-bold text-white tracking-tight">현재 진행 중인 보험</h2>
                </div>

                <div className="flex gap-4 overflow-x-auto hide-scrollbar pb-6 snap-x snap-mandatory w-full">
                    {activeInsurances.map(ins => (
                        <div key={ins.id} className="flex-shrink-0 w-[260px] md:w-[300px] bg-[#161B26] rounded-3xl p-6 relative overflow-hidden snap-start shadow-xl border border-slate-800 flex flex-col hover:border-slate-700 transition-colors">
                            <div className="flex justify-between items-start mb-6">
                                <span 
                                    className="px-3 py-1.5 rounded-lg text-xs font-bold tracking-wide"
                                    style={{ 
                                        color: ins.statusColor,
                                        backgroundColor: ins.statusColor + '1A' // 10% opacity
                                    }}
                                >
                                    {ins.status}
                                </span>
                                <button className="text-slate-500 hover:text-white transition-colors">
                                    <MoreVertical className="w-5 h-5" />
                                </button>
                            </div>

                            <div className="mb-8">
                                <h3 className="text-[20px] font-bold text-white mb-2">{ins.productName}</h3>
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
        </div>
    );
};

export default HomePage;
