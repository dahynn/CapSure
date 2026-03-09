import React, { useEffect, useState } from 'react';
import { Loader2, ArrowRight } from 'lucide-react';
import { getBenefitTracker } from '../../api/home.api';

const BenefitTracker = () => {
    const [data, setData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const result = await getBenefitTracker();
                setData(result);
            } catch (error) {
                console.error('Failed to fetch benefit tracker:', error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchData();
    }, []);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center p-6 min-h-[120px]">
                <Loader2 className="w-6 h-6 text-primary-500 animate-spin" />
            </div>
        );
    }

    if (!data) return null;

    return (
        <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm flex flex-col justify-between gap-4 h-full min-h-[160px]">
            <h3 className="text-slate-800 font-bold">누적 혜택 트래커</h3>

            <div className="flex-grow flex items-center">
                <p className="text-2xl md:text-3xl font-bold leading-tight">
                    <span className="text-primary-600">{data.savedAmount}</span>원을 절약하고<br />
                    더 좋은 혜택을 받을 수 있어요!
                </p>
            </div>

            <button className="w-full mt-2 py-3 bg-slate-50 text-slate-700 hover:bg-slate-100 font-bold rounded-xl transition-colors flex items-center justify-center gap-2">
                더 자세히 보기
                <ArrowRight className="w-4 h-4" />
            </button>
        </div>
    );
};

export default BenefitTracker;
