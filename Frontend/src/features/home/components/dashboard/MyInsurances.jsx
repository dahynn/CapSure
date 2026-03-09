import React, { useEffect, useState } from 'react';
import { Loader2 } from 'lucide-react';
import { getMyInsurances } from '../../api/home.api';

const MyInsurances = () => {
    const [data, setData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const result = await getMyInsurances();
                setData(result);
            } catch (error) {
                console.error('Failed to fetch my insurances:', error);
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

    if (!data || data.length === 0) return null;

    return (
        <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm flex flex-col gap-4 h-full min-h-[160px]">
            <h3 className="text-slate-800 font-bold">내 보험 보관함</h3>

            <p className="text-sm font-medium text-slate-500">다른 플랫폼에서 적용중인 보험 목록이에요!</p>

            <div className="flex flex-col gap-3 mt-1 overflow-y-auto max-h-[200px] pr-2">
                {data.map((ins) => (
                    <div key={ins.id} className="p-3 bg-slate-50 rounded-xl flex items-center justify-between border border-slate-100">
                        <span className="font-bold text-slate-800">{ins.name}</span>
                        <span className="text-xs font-medium text-slate-400">{ins.company}</span>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default MyInsurances;
