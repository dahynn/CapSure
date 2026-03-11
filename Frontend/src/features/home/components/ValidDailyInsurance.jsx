import React, { useState, useEffect } from 'react';
import { ChevronDown, ChevronUp, Loader2 } from 'lucide-react';
import { getValidDailyInsurances } from '../api/home.api.js';

const ValidDailyInsurance = () => {
    const [expandedItems, setExpandedItems] = useState({});
    const [insurances, setInsurances] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchInsurances = async () => {
            try {
                const data = await getValidDailyInsurances();
                setInsurances(data);
            } catch (error) {
                console.error("Failed to fetch insurances:", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchInsurances();
    }, []);

    if (isLoading) {
        return (
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-center min-h-[150px]">
                <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
            </div>
        );
    }

    if (insurances.length === 0) return null;

    const toggleExpand = (id) => {
        setExpandedItems(prev => ({ ...prev, [id]: !prev[id] }));
    };

    const calculateProgress = (passed, total) => {
        return Math.min(100, Math.max(0, (passed / total) * 100));
    };

    const formatRemainingTime = (total, passed) => {
        const remainingMins = Math.max(0, total - passed);
        const days = Math.floor(remainingMins / (24 * 60));
        const hours = Math.floor((remainingMins % (24 * 60)) / 60);
        const mins = remainingMins % 60;
        return `${String(days).padStart(2, '0')}일 ${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}`;
    };

    return (
        <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex flex-col gap-4">
            <h2 className="text-xl font-bold text-slate-800">현재 적용되고 있는 보험은 아래와 같아요!</h2>

            <div className="flex flex-col gap-4">
                {insurances.map((ins) => {
                    const progress = calculateProgress(ins.passedTime, ins.totalTime);
                    const isExpanded = expandedItems[ins.id];

                    return (
                        <div key={ins.id} className="p-4 rounded-2xl bg-slate-50 border border-slate-100 flex flex-col gap-3">
                            <div className="flex items-center justify-between">
                                <span className="font-bold text-slate-800">{ins.name} <span className="text-slate-500 font-medium text-sm">({ins.company})</span></span>
                            </div>

                            <div className="flex flex-col gap-1">
                                <div className="flex justify-between text-sm font-medium">
                                    <span className="text-slate-600">진행률</span>
                                    <span className="text-primary-600 font-bold">남은 시간 : {formatRemainingTime(ins.totalTime, ins.passedTime)}</span>
                                </div>
                                <div className="w-full bg-slate-200 rounded-full h-2.5 overflow-hidden">
                                    <div
                                        className="bg-primary-500 h-2.5 rounded-full transition-all duration-500"
                                        style={{ width: `${progress}%` }}
                                    ></div>
                                </div>
                            </div>

                            <button
                                onClick={() => toggleExpand(ins.id)}
                                className="mt-2 text-sm text-slate-500 hover:text-slate-800 flex items-center gap-1 font-medium transition-colors"
                            >
                                {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                                보장 항목 상세보기
                            </button>

                            {isExpanded && (
                                <div className="mt-2 bg-white rounded-xl border border-slate-200 overflow-hidden">
                                    <table className="w-full text-sm text-left">
                                        <thead className="bg-slate-50 text-slate-600 border-b border-slate-200">
                                            <tr>
                                                <th className="px-4 py-3 font-semibold text-center">보장항목</th>
                                                <th className="px-4 py-3 font-semibold text-center">금액</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {ins.guarantees.map((item, idx) => (
                                                <tr key={idx} className="border-b border-slate-100 last:border-0 hover:bg-slate-50 transition-colors">
                                                    <td className="px-4 py-3 text-slate-700 text-center">{item.item}</td>
                                                    <td className="px-4 py-3 text-slate-700 text-center font-medium">{item.amount}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default ValidDailyInsurance;
