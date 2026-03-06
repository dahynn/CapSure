import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Flame, HeartPulse, Dog, Car, ArrowRight, ShieldCheck, Sparkles } from 'lucide-react';

const HomePage = () => {
    const navigate = useNavigate();

    const insuranceMenus = [
        { title: '화재보험', icon: Flame, color: 'text-orange-500', bg: 'bg-orange-50' },
        { title: '생명보험', icon: HeartPulse, color: 'text-rose-500', bg: 'bg-rose-50' },
        { title: '펫 보험', icon: Dog, color: 'text-amber-500', bg: 'bg-amber-50' },
        { title: '자동차 보험', icon: Car, color: 'text-blue-500', bg: 'bg-blue-50' },
    ];

    const quickLinks = [
        { title: '내 맘대로 조립하는', subtitle: '캡슐 보험', path: '/capsule-insurance', color: 'from-primary-500 to-primary-600' },
        { title: '단 하루만 필요할 땐', subtitle: '일일 보험', path: '/daily-insurance', color: 'from-emerald-500 to-teal-600' },
    ];

    return (
        <div className="p-6 md:p-8 space-y-8 pb-24">

            {/* Welcome Section */}
            <div className="flex items-center justify-between animate-in slide-in-from-top-4 duration-500">
                <div>
                    <h1 className="text-2xl md:text-3xl font-bold text-slate-800">
                        반갑습니다 김블록님 👋
                    </h1>
                    <p className="text-slate-500 mt-1">오늘도 안전한 하루 되세요</p>
                </div>
            </div>

            {/* Main Banner - Product Recommendation */}
            <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-indigo-900 via-indigo-800 to-primary-900 p-8 text-white shadow-2xl group cursor-pointer animate-in fade-in duration-700">
                <div className="absolute top-0 right-0 p-8 opacity-20 group-hover:opacity-40 transition-opacity duration-500 group-hover:scale-110 transform">
                    <ShieldCheck className="w-48 h-48" />
                </div>

                <div className="relative z-10">
                    <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/20 backdrop-blur-md text-sm font-semibold mb-4">
                        <Sparkles className="w-4 h-4 text-yellow-300" />
                        <span>AI 맞춤 추천</span>
                    </div>
                    <h2 className="text-3xl font-bold mb-2">나에게 딱 맞는<br />맞춤형 보험 찾기</h2>
                    <p className="text-indigo-200 mb-6 max-w-sm">
                        복잡한 보험, 이제 AI가 분석해서 꼭 필요한 보장만 추천해드려요.
                    </p>
                    <button className="bg-white text-indigo-900 px-6 py-3 rounded-xl font-bold hover:bg-indigo-50 transition-colors flex items-center gap-2 group-hover:gap-4">
                        추천받기
                        <ArrowRight className="w-5 h-5" />
                    </button>
                </div>
            </div>

            {/* Grid Layout - Insurance Menus */}
            <div>
                <h3 className="text-lg font-bold text-slate-800 mb-4 px-1">보험 카테고리</h3>
                <div className="grid grid-cols-4 gap-4">
                    {insuranceMenus.map((menu, idx) => (
                        <div
                            key={idx}
                            className="flex flex-col items-center gap-3 p-4 rounded-2xl bg-white shadow-sm border border-slate-100 hover:shadow-md transition-shadow cursor-pointer hover:-translate-y-1 transform duration-200"
                        >
                            <div className={`p-4 rounded-2xl ${menu.bg}`}>
                                <menu.icon className={`w-8 h-8 ${menu.color}`} />
                            </div>
                            <span className="text-sm font-bold text-slate-700">{menu.title}</span>
                        </div>
                    ))}
                </div>
            </div>

            {/* Main Feature Links */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4">
                {quickLinks.map((link, idx) => (
                    <div
                        key={idx}
                        onClick={() => navigate(link.path)}
                        className={`cursor-pointer rounded-3xl p-6 bg-gradient-to-br ${link.color} text-white shadow-lg hover:shadow-xl hover:-translate-y-1 transition-all duration-300 relative overflow-hidden group`}
                    >
                        <div className="absolute right-0 bottom-0 opacity-10 group-hover:scale-110 group-hover:opacity-20 transition-all duration-500 transform translate-x-4 translate-y-4">
                            <ShieldCheck className="w-32 h-32" />
                        </div>
                        <div className="relative z-10">
                            <p className="text-white/80 font-medium text-sm mb-1">{link.title}</p>
                            <div className="flex items-center justify-between">
                                <h3 className="text-2xl font-bold">{link.subtitle}</h3>
                                <div className="w-10 h-10 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center group-hover:bg-white/30 transition-colors">
                                    <ArrowRight className="w-5 h-5 text-white" />
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>

        </div>
    );
};

export default HomePage;
