import React from 'react';
import { Bell } from 'lucide-react';
import defaultAvatar from '@/assets/logo.png'; // Fallback to logo or empty

const DashboardHeader = ({ userName }) => {
    return (
        <div className="flex justify-between items-center mb-6">
            <div className="flex items-center gap-3">
                <div className="flex flex-col">
                    <h1 className="text-xl md:text-2xl font-bold text-white tracking-tight leading-none">대시보드</h1>
                </div>
            </div>
        </div>
    );
};

export default DashboardHeader;
