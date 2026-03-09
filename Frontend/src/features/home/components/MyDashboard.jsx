import React, { useState, useEffect } from 'react';
import { ShieldCheck, AlertCircle, Loader2 } from 'lucide-react';
import { getMyDataStatus, agreeMyData } from '../api/home.api.js';

const MyDashboard = () => {
    const [isMyDataAgreed, setIsMyDataAgreed] = useState(false);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchMyDataStatus = async () => {
            try {
                const status = await getMyDataStatus();
                setIsMyDataAgreed(status);
            } catch (error) {
                console.error("Failed to fetch my data status:", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchMyDataStatus();
    }, []);

    const handleAgree = async () => {
        setIsLoading(true);
        try {
            await agreeMyData();
            setIsMyDataAgreed(true);
        } catch (error) {
            console.error("Failed to agree my data:", error);
        } finally {
            setIsLoading(false);
        }
    };

    if (isLoading) {
        return (
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-center min-h-[150px]">
                <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
            </div>
        );
    }

    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100">
            <h2 className="text-xl font-bold text-slate-800 mb-4 px-1">마이 대시보드</h2>

            {!isMyDataAgreed ? (
                <div className="flex flex-col items-center justify-center py-6 px-4 bg-orange-50 rounded-2xl border border-orange-100 text-center gap-4">
                    <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center shadow-sm">
                        <AlertCircle className="w-6 h-6 text-orange-500" />
                    </div>
                    <div>
                        <h3 className="text-lg font-bold text-slate-800 mb-1">마이데이터 동의가 필요해요!</h3>
                        <p className="text-sm text-slate-600">안전하고 정확한 진단을 위해 동의를 진행해주세요.</p>
                    </div>
                    <button
                        onClick={handleAgree}
                        className="w-full max-w-xs py-3 px-4 bg-primary-600 hover:bg-primary-700 text-white font-bold rounded-xl transition-colors shadow-sm"
                    >
                        동의하러 가기
                    </button>
                </div>
            ) : (
                <div className="flex flex-col items-center justify-center py-8 px-4 bg-slate-50 rounded-2xl border border-slate-100 text-center gap-3 animate-in fade-in duration-500">
                    <div className="w-12 h-12 bg-white rounded-full flex items-center justify-center shadow-sm">
                        <ShieldCheck className="w-6 h-6 text-emerald-500" />
                    </div>
                    <h3 className="text-lg font-bold text-slate-800">마이데이터 동의가 완료됨</h3>
                </div>
            )}
        </div>
    );
};

export default MyDashboard;
