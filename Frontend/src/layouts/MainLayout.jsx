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
        { name: '일일 보험', path: '/daily-insurance' },
    ];

    return (
        <div className="min-h-screen bg-slate-50 flex flex-col items-center">
            {/* Centered Desktop Container */}
            <div className="w-full max-w-5xl flex flex-col min-h-screen bg-white shadow-2xl relative overflow-hidden">

                {/* Header Navigation */}
                <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-lg border-b border-slate-200 px-6 py-4 flex items-center justify-between">
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
                </header>

                {/* Main Content Area */}
                <main className="flex-1 w-full bg-slate-50 relative">
                    <Outlet />
                </main>
            </div>
        </div>
    );
};

export default MainLayout;
