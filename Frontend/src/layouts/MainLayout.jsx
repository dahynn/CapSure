import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { UserCircle, ShieldAlert, LogOut, History, User, Package } from 'lucide-react';
import { useState } from 'react';

import SessionTimer from '@/features/auth/components/SessionTimer';

const MainLayout = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [showProfileMenu, setShowProfileMenu] = useState(false);

    const navItems = [
        { name: '캡슐 보험', path: '/capsule-insurance' },
        { name: '보험 탐색', path: '/search-insurance' },
    ];

    return (
        <div className="min-h-screen bg-white flex flex-col">
            {/* Full-width Container */}
            <div className="w-full flex flex-col min-h-screen relative overflow-hidden">

                {/* Header Navigation */}
                <header className="sticky top-0 z-50 w-full bg-white/80 backdrop-blur-lg border-b border-slate-200 flex justify-center">
                    <div className="w-full max-w-5xl px-6 py-4 flex items-center justify-between">
                        <div
                            className="flex items-center gap-2 cursor-pointer"
                            onClick={() => navigate('/home')}
                        >
                            <ShieldAlert className="w-8 h-8 text-primary-600" />
                            <span className="text-xl font-black tracking-tight text-slate-800">
                            CapsuleCover
                        </span>
                    </div>

                    <nav className="hidden md:flex items-center gap-8">
                        {navItems.map((item) => (
                            <button
                                key={item.path}
                                onClick={() => navigate(item.path)}
                                className={`text-sm font-semibold transition-colors duration-200 relative ${location.pathname.startsWith(item.path)
                                    ? 'text-primary-600'
                                    : 'text-slate-500 hover:text-slate-900'
                                    }`}
                            >
                                {item.name}
                                {location.pathname.startsWith(item.path) && (
                                    <span className="absolute -bottom-5 left-0 w-full h-0.5 bg-primary-600 rounded-t-full" />
                                )}
                            </button>
                        ))}
                    </nav>

                    <div className="flex items-center gap-4">
                        <SessionTimer />
                        <div className="relative">
                            <button
                                onClick={() => setShowProfileMenu(!showProfileMenu)}
                                className="p-2 rounded-full hover:bg-slate-100 transition-colors"
                            >
                                <UserCircle className="w-7 h-7 text-slate-600" />
                            </button>

                            {/* Profile Menu Dropdown */}
                            {showProfileMenu && (
                                <div className="absolute right-0 mt-2 w-52 bg-white rounded-xl shadow-lg border border-slate-100 py-2 z-50 animate-in fade-in slide-in-from-top-2">
                                    <button
                                        onClick={() => { navigate('/profile'); setShowProfileMenu(false); }}
                                        className="w-full px-4 py-2.5 text-left text-sm font-medium text-slate-700 hover:bg-slate-50 flex items-center gap-3 transition-colors"
                                    >
                                        <User className="w-4 h-4 text-slate-400" />
                                        마이페이지
                                    </button>
                                    <button
                                        onClick={() => { navigate('/my-capsule'); setShowProfileMenu(false); }}
                                        className="w-full px-4 py-2.5 text-left text-sm font-medium text-slate-700 hover:bg-slate-50 flex items-center gap-3 transition-colors"
                                    >
                                        <Package className="w-4 h-4 text-slate-400" />
                                        내 캡슐 구독 조회
                                    </button>
                                    <button
                                        onClick={() => { navigate('/my-history'); setShowProfileMenu(false); }}
                                        className="w-full px-4 py-2.5 text-left text-sm font-medium text-slate-700 hover:bg-slate-50 flex items-center gap-3 transition-colors"
                                    >
                                        <History className="w-4 h-4 text-slate-400" />
                                        보험 가입 이력 확인
                                    </button>
                                    <div className="h-px bg-slate-100 my-1" />
                                    <button
                                        className="w-full px-4 py-2.5 text-left text-sm font-medium text-red-600 hover:bg-red-50 flex items-center gap-3 transition-colors"
                                        onClick={() => { navigate('/login'); setShowProfileMenu(false); }}
                                    >
                                        <LogOut className="w-4 h-4 text-red-400" />
                                        로그아웃
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                    </div>
                </header>

                {/* Main Content Area */}
                <main className="flex-1 w-full bg-slate-50 relative flex flex-col">
                    <div className="flex-1 w-full">
                        <Outlet />
                    </div>
                    {/* Footer */}
                    <footer className="w-full bg-slate-900 text-slate-400 py-10 px-8 text-sm">
                        <div className="max-w-5xl mx-auto flex flex-col gap-8">
                            {/* Top Links */}
                            <div className="flex flex-wrap gap-x-6 gap-y-2 font-medium text-slate-300">
                                <a href="#" className="hover:text-white transition-colors">공시실</a>
                                <a href="#" className="hover:text-white transition-colors">보호금융상품등록부</a>
                                <a href="#" className="hover:text-white transition-colors">FOREIGN</a>
                                <a href="#" className="hover:text-white transition-colors font-bold">개인정보처리방침</a>
                                <a href="#" className="hover:text-white transition-colors">고객정보 취급방침</a>
                                <a href="#" className="hover:text-white transition-colors">사회공헌활동</a>
                            </div>

                            <div className="flex flex-col md:flex-row justify-between gap-8 border-t border-slate-700 pt-8">
                                {/* Left Info */}
                                <div className="space-y-4">
                                    <div className="flex items-center gap-2 mb-2">
                                        <ShieldAlert className="w-6 h-6 text-slate-500" />
                                        <span className="text-lg font-black tracking-tight text-white">Capsure</span>
                                        <span className="text-xs text-slate-500 ml-2">by SSAFY</span>
                                    </div>
                                    <p className="text-xs space-y-1 text-slate-400">
                                        <span className="block">Capsure 대표이사 정정교 | 사업자등록번호 000-00-00000</span>
                                        <span className="block">서울특별시 강남구 테헤란로 212 (역삼동, SSAFY) (우) 06220</span>
                                    </p>
                                    <p className="text-xs mt-4 text-slate-500">
                                        COPYRIGHT ⓒCAPSURE CO., LTD ALL RIGHTS RESERVED.
                                    </p>
                                </div>

                                {/* Center/Right Info (CS Center & Awards) */}
                                <div className="space-y-6 md:w-1/2">
                                    {/* CS Center */}
                                    <div>
                                        <p className="font-bold text-slate-300 mb-2">고객상담센터 (콜센터) 안내</p>
                                        <div className="space-y-1 text-xs text-slate-400">
                                            <p><span className="font-semibold text-slate-200">고객상담센터</span> 1588-0000 <span className="text-slate-500 ml-1">(해외문의 82-2-1234-1234)</span></p>
                                            <p><span className="font-semibold text-slate-200">장애인고객전용</span> 1670-0000 <span className="font-semibold text-slate-200 ml-3">외국인고객전용</span> 1566-0000</p>
                                            <p><span className="font-semibold text-slate-200">보험계약대출</span> 1544-0000 <span className="font-semibold text-slate-200 ml-3">TM 전용상품</span> 1566-0000</p>
                                        </div>
                                    </div>

                                    {/* Awards */}
                                    <div className="text-[10px] leading-tight text-slate-500 space-y-1">
                                        <p>· SSAFY 최우수상 수상자가 있는 팀</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </footer>
                </main>
            </div>
        </div>
    );
};

export default MainLayout;
