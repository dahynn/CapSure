import React, { useEffect, useState } from 'react';
import { getInsuranceCoverages } from '../../api/capsureInsurance.api';
import { Loader2, ArrowRight } from 'lucide-react';

const InsuranceInfoConfirm = ({ selectedCells, onNext, onPrev }) => {
    const [coverages, setCoverages] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchCoverages = async () => {
            setIsLoading(true);
            try {
                // Extract unique insurance IDs from selected cells
                const uniqueIds = Array.from(new Set(
                    selectedCells
                        .filter(cell => cell !== null && cell.groupId)
                        .map(cell => cell.groupId) // We need original item ID, but we only have groupId in cell. 
                    // Wait, looking at the previous code, we didn't save `item.id`, we saved `groupId`.
                    // Let's assume for mock purposes, we extract from category/price.
                    // Actually, I should just pass selectedCells to mock API and parse it there, or just mock it here.
                ));

                // For mock API, we'll just pass a dummy array since real IDs weren't strictly mapped in selectedCells.
                // We will collect unique names/companies from selectedCells to form the mock IDs
                const selectedItemsDetails = selectedCells.filter(c => c !== null).reduce((acc, current) => {
                    const x = acc.find(item => item.groupId === current.groupId);
                    if (!x) {
                        return acc.concat([current]);
                    } else {
                        return acc;
                    }
                }, []);

                // Let's mock coverages based on the selected items' name and company directly 
                // to avoid complex ID mapping in dummy state
                const mappedCoverages = selectedItemsDetails.map(item => {
                    const priceMatch = item.name.match(/\((\d+)만\)/);
                    const price = priceMatch ? Number(priceMatch[1]) : 1;

                    const detail = [
                        { label: '입원 일당', amount: (price * 100) + '만원' },
                        { label: '수술비 지원', amount: (price * 300) + '만원' },
                    ];
                    if (item.category.id === 'pet') {
                        detail.push({ label: '개물림 사고 처벌 벌금 지원', amount: '300만원' });
                    } else if (item.category.id === 'disease') {
                        detail.push({ label: '사망', amount: '2000만원' });
                        detail.push({ label: '진료비', amount: '500만원' });
                    }

                    return {
                        id: item.groupId,
                        title: `${item.name.replace(/\s*\(\d+만\)/, '')} - ${item.company}`,
                        details: detail
                    };
                });

                setCoverages(mappedCoverages);
            } catch (error) {
                console.error("Failed to fetch coverages", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchCoverages();
    }, [selectedCells]);

    if (isLoading) {
        return (
            <div className="flex flex-col items-center justify-center h-64 space-y-4">
                <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
                <p className="text-slate-500 font-medium">보험 정보를 불러오는 중입니다...</p>
            </div>
        );
    }

    return (
        <div className="max-w-3xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4">
            <div className="text-center py-6">
                <h2 className="text-2xl font-bold text-slate-800">보험 정보 확인</h2>
                <p className="text-slate-500 mt-2">선택하신 맞춤형 캡슐 보험의 보장 내역을 확인해주세요.</p>
            </div>

            <div className="space-y-6">
                {coverages.map((item) => (
                    <div key={item.id} className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
                        <div className="bg-slate-50 px-6 py-4 border-b border-slate-200">
                            <h3 className="font-bold text-slate-800 text-lg">{item.title}</h3>
                        </div>
                        <div className="p-6">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="border-b-2 border-slate-100 text-slate-500 text-sm">
                                        <th className="py-3 font-medium">보장 항목</th>
                                        <th className="py-3 font-medium text-right">보장 금액</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {item.details.map((detail, idx) => (
                                        <tr key={idx} className="border-b border-slate-50 last:border-0 text-sm">
                                            <td className="py-4 font-bold text-slate-700">{detail.label}</td>
                                            <td className="py-4 font-bold text-primary-600 text-right">{detail.amount}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                ))}
            </div>

            <div className="flex justify-between items-center pt-8 border-t border-slate-200 mt-8">
                <button
                    onClick={onPrev}
                    className="px-6 py-3 text-slate-500 hover:text-slate-700 font-medium transition-colors"
                >
                    이전으로
                </button>
                <button
                    onClick={onNext}
                    className="px-8 py-3 bg-primary-600 text-white font-bold rounded-xl shadow-lg hover:bg-primary-700 transition-colors flex items-center gap-2 group"
                >
                    다음
                    <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </button>
            </div>
        </div>
    );
};

export default InsuranceInfoConfirm;
