import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { UserCircle, ShieldAlert, LogOut, History, User, Package, Home, Pill, LayoutDashboard, Search } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';

import SessionTimer from '@/features/auth/components/SessionTimer';
import logoImg from '@/assets/logo.png';
import capsureLogoImg from '@/assets/capsure_logo.png';

const MainLayout = () => {
    const navigate = useNavigate();
    const location = useLocation();
    
    

    return (
        <div className="min-h-screen bg-black flex flex-col items-center">
            {/* App Container (Max Width 560px for Mobile Feel) */}
            <div className="w-full max-w-[560px] flex flex-col min-h-screen relative overflow-hidden shadow-[0_0_40px_rgba(255,255,255,0.05)]" style={{ backgroundColor: 'var(--color-bg)' }}>

                {/* Header Navigation */}
                <header 
                    className="sticky top-0 z-50 w-full flex justify-center border-b border-slate-800/50"
                    style={{ backgroundColor: 'var(--color-bg)' }}
                >
                    <div className="hidden"><SessionTimer /></div>
                    <div className="w-full max-w-5xl px-6 py-4 flex items-center justify-between">
                        {/* 왼쪽 로고 영역 */}
                        <div
                            className="flex items-center cursor-pointer"
                            onClick={() => navigate('/home')}
                        >
                            <img src={logoImg} alt="Capsure Logo Icon" className="w-[42px] h-[42px] object-contain drop-shadow-md" />
                            <img src={capsureLogoImg} alt="CAPSURE" className="h-[18px] ml-2.5 mt-1 object-contain drop-shadow-md" />
                        </div>

                        {/* 오른쪽 검색 영역 */}
                        <div className="flex items-center justify-end">
                            <div className="flex items-center justify-center h-10 w-10">
                                <button 
                                    onClick={() => navigate('/search')}
                                    className="p-2 hover:bg-slate-800 rounded-full transition-colors text-white"
                                >
                                    <Search className="w-5 h-5" />
                                </button>
                            </div>
                        </div>
                    </div>
                </header>

                {/* Main Content Area */}
                <main className="flex-1 w-full relative flex flex-col" style={{ backgroundColor: 'var(--color-bg)' }}>
                    <div className="flex-1 w-full pb-24 md:pb-8">
                        <Outlet />
                    </div>
                </main>

                {/* Mobile Bottom Navigation */}
                <nav 
                    className="md:hidden sticky bottom-0 w-full z-50 rounded-t-[32px] px-8 py-5 shadow-[0_-10px_40px_rgba(0,0,0,0.5)]"
                    style={{ backgroundColor: 'var(--color-bg)' }}
                >
                    <div className="flex justify-between items-center max-w-full">
                        {[
                            { name: '홈', path: '/home', icon: Home },
                            { name: '캡슐', path: '/capsure-insurance', icon: Pill },
                            { name: '대시보드', path: '/dashboard', icon: LayoutDashboard },
                            { name: '마이페이지', path: '/mypage', icon: User },
                        ].map((item) => {
                            const Icon = item.icon;
                            const isActive = item.path === '/home' 
                                ? location.pathname === '/home'
                                : location.pathname.startsWith(item.path) || (item.name === '마이페이지' && location.pathname.startsWith('/my-'));
                            
                            return (
                                <button
                                    key={item.name}
                                    onClick={() => navigate(item.path)}
                                    className={`flex flex-col items-center gap-1.5 transition-all ${
                                        isActive ? 'text-[#82D8FC]' : 'text-[#9D9DA4]'
                                    }`}
                                >
                                    <Icon 
                                        className="w-[22px] h-[22px]" 
                                        strokeWidth={isActive ? 2.5 : 2}
                                        fill={isActive ? 'currentColor' : 'none'}
                                    />
                                    <span className={`text-[10px] ${isActive ? 'font-bold' : 'font-medium'}`}>
                                        {item.name}
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                </nav>
            </div>
        </div>
    );
};

export default MainLayout;
