import React, { useEffect, useState } from 'react';
import { Loader2, ArrowRight } from 'lucide-react';
import { getSubscriptionSummary } from '../../api/home.api';

const SubscriptionSummary = () => {
    const [data, setData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const result = await getSubscriptionSummary();
                setData(result);
            } catch (error) {
                console.error('Failed to fetch subscription summary:', error);
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

    // Conic gradient string generation
    const colors = ['#f97316', '#0ea5e9', '#10b981', '#6366f1', '#ec4899'];
    let currentPercentage = 0;
    const gradientStops = data.details.map((item, idx) => {
        const start = currentPercentage;
        currentPercentage += item.percentage;
        const end = currentPercentage;
        return `${colors[idx % colors.length]} ${start}% ${end}%`;
    }).join(', ');

    const conicGradientStyle = {
        background: `conic-gradient(${gradientStops})`,
        borderRadius: '50%'
    };

    return (
        <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm flex flex-col gap-4">
            <h3 className="text-slate-800 font-bold">캡슐 보험 구독 요약</h3>

            <div className="flex flex-col sm:flex-row gap-6 mt-2">
                {/* Left: Pie Chart */}
                <div className="flex-shrink-0 flex justify-center items-center">
                    <div className="relative w-32 h-32 md:w-40 md:h-40 shadow-inner" style={conicGradientStyle}>
                        {/* Inner circle for donut style (optional, remove if solid pie is preferred) */}
                        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-1/2 h-1/2 bg-white rounded-full"></div>
                    </div>
                </div>

                {/* Right: Details */}
                <div className="flex flex-col flex-1 justify-between">
                    <div>
                        <p className="text-sm font-medium text-slate-500 mb-1">이번달 요금은</p>
                        <p className="text-2xl font-bold text-slate-800 mb-4">{data.totalAmount.toLocaleString()}원이에요!</p>

                        <ul className="space-y-2">
                            {data.details.map((item, idx) => (
                                <li key={idx} className="flex items-center justify-between text-sm">
                                    <div className="flex items-center gap-2">
                                        <div className="w-3 h-3 rounded-full" style={{ backgroundColor: colors[idx % colors.length] }}></div>
                                        <span className="text-slate-700">{item.company} {item.name}</span>
                                    </div>
                                    <span className="font-semibold text-slate-800">{item.percentage}%</span>
                                </li>
                            ))}
                        </ul>
                    </div>

                    <div className="mt-4 pt-4 border-t border-slate-100 flex justify-end">
                        <button 
                            onClick={() => window.location.href = '/my-capsure'}
                            className="text-sm text-slate-500 hover:text-primary-600 font-medium flex items-center gap-1 transition-colors"
                        >
                            더 자세히 보러가기 <ArrowRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SubscriptionSummary;
