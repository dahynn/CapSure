import React, { useEffect, useState } from 'react';
import { Loader2, ChevronDown, ChevronUp } from 'lucide-react';
import { getMyInsurances } from '../../api/home.api';

const MyInsurances = () => {
    const [data, setData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [expandedItems, setExpandedItems] = useState({});

    const toggleExpand = (id) => {
        setExpandedItems((prev) => ({ ...prev, [id]: !prev[id] }));
    };

    const dummyGuarantees = [
        { item: '진료비', amount: '300만원' },
        { item: '수술비', amount: '500만원' },
        { item: '입원일당', amount: '5만원' }
    ];

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

            <div className="flex flex-col gap-3 mt-1 overflow-y-auto max-h-[400px] pr-2">
                {data.map((ins) => (
                    <div key={ins.id} className="p-3 bg-slate-50 rounded-xl flex flex-col border border-slate-100">
                        <div 
                            className="flex items-center justify-between cursor-pointer"
                            onClick={() => toggleExpand(ins.id)}
                        >
                            <span className="font-bold text-slate-800">{ins.name}</span>
                            <div className="flex items-center gap-2">
                                <span className="text-xs font-medium text-slate-400">{ins.company}</span>
                                {expandedItems[ins.id] ? <ChevronUp className="w-4 h-4 text-slate-400"/> : <ChevronDown className="w-4 h-4 text-slate-400"/>}
                            </div>
                        </div>

                        <div className={`grid transition-all duration-300 ease-in-out ${expandedItems[ins.id] ? "grid-rows-[1fr] opacity-100 mt-3" : "grid-rows-[0fr] opacity-0"}`}>
                            <div className="overflow-hidden">
                                <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
                                    <table className="w-full text-sm text-left">
                                        <thead className="bg-slate-50 text-slate-600 border-b border-slate-200">
                                            <tr>
                                                <th className="px-4 py-3 font-semibold text-center">보장 항목</th>
                                                <th className="px-4 py-3 font-semibold text-center">보장 금액</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {(ins.guarantees || dummyGuarantees).map((g, idx) => (
                                                <tr key={idx} className="border-b border-slate-100 last:border-0 hover:bg-slate-50 transition-colors">
                                                    <td className="px-4 py-3 text-slate-700 text-center">{g.item}</td>
                                                    <td className="px-4 py-3 text-slate-700 text-center font-medium">{g.amount}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default MyInsurances;
