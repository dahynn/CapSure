import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/features/auth/api/auth.api';

const SessionTimer = () => {
    const [timeLeft, setTimeLeft] = useState(30 * 60); // 30 minutes in seconds
    const navigate = useNavigate();

    useEffect(() => {
        if (timeLeft <= 0) {
            sessionStorage.clear();
            alert("세션 시간이 만료되었습니다. 다시 로그인해주세요.");
            navigate('/login');
            return;
        }

        const timerId = setInterval(() => {
            setTimeLeft((prev) => prev - 1);
        }, 1000);

        return () => clearInterval(timerId);
    }, [timeLeft, navigate]);

    const handleExtend = async () => {
        try {
            const response = await authApi.extendSession();
            if (response.success) {
                setTimeLeft(30 * 60); // Reset timer to 30 minutes
            }
        } catch (error) {
            console.error('Session extension failed:', error);
        }
    };

    const formatTime = (seconds) => {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    };

    return (
        <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-100 rounded-full border border-slate-200">
            <span className="text-sm font-medium text-slate-600 font-mono w-[42px] text-center">
                {formatTime(timeLeft)}
            </span>
            <div className="w-px h-3 bg-slate-300"></div>
            <button
                onClick={handleExtend}
                className="text-xs font-semibold text-primary-600 hover:text-primary-800 transition-colors"
            >
                연장
            </button>
        </div>
    );
};

export default SessionTimer;
