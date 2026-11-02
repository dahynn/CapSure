import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/features/auth/api/auth.api';
import { getAccessToken } from '@/common/api/httpClient';
import { tokenRemainingSeconds } from '@/common/utils/tokenExpiry.mjs';

const SessionTimer = () => {
    const [timeLeft, setTimeLeft] = useState(() => tokenRemainingSeconds(getAccessToken()));
    const [extending, setExtending] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        const timerId = setInterval(() => {
            setTimeLeft(tokenRemainingSeconds(getAccessToken()));
        }, 1000);

        return () => clearInterval(timerId);
    }, []);

    const handleExtend = async () => {
        if (extending) return;
        setExtending(true);
        setError('');
        try {
            const response = await authApi.extendSession();
            if (response.success) {
                setTimeLeft(tokenRemainingSeconds(getAccessToken()));
            }
        } catch (error) {
            setError(error.message || '세션을 연장하지 못했습니다.');
            if ([401, 403].includes(error.status)) navigate('/login', { replace: true });
        } finally {
            setExtending(false);
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
                disabled={extending}
                className="text-xs font-semibold text-primary-600 hover:text-primary-800 transition-colors"
            >
                {extending ? '연장 중' : '연장'}
            </button>
            {error && <span role="alert" className="text-xs text-red-700">{error}</span>}
        </div>
    );
};

export default SessionTimer;
