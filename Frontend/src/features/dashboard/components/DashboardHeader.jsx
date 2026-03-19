import React from 'react';
import { Bell } from 'lucide-react';
import defaultAvatar from '@/assets/logo.png'; // Fallback to logo or empty

const DashboardHeader = ({ userName }) => {
    return (
        <div className="flex justify-between items-center mb-6">
            <div className="flex items-center gap-3">
                <div className="w-[46px] h-[46px] rounded-full bg-[#E5D0CA] p-1 flex items-center justify-center shadow-inner overflow-hidden border border-slate-700/50">
                    <div className="w-[32px] h-[32px] bg-white rounded-md shadow-sm flex items-center justify-center text-[8px] font-bold text-slate-300">
                        <span className="opacity-50 tracking-widest leading-none mt-2">LOGO</span>
                    </div>
                </div>
                <div className="flex flex-col">
                    <span className="text-[11px] font-bold text-[#9D9DA4] tracking-[0.15em] mb-0.5 uppercase">Welcome Back</span>
                    <h1 className="text-xl md:text-2xl font-bold text-white tracking-tight leading-none">대시보드</h1>
                </div>
            </div>
            
            <button className="w-10 h-10 rounded-full bg-[#161B26] hover:bg-[#1F2736] flex items-center justify-center transition-colors border border-slate-800/80 group">
                <Bell className="w-5 h-5 text-slate-300 group-hover:text-white transition-colors" />
            </button>
        </div>
    );
};

export default DashboardHeader;
