import React, { useEffect, useState } from 'react';
import { getPaymentHistory } from '../api/mypage.api';
import { Loader2, Receipt, Calendar, CreditCard, ChevronDown, ChevronUp } from 'lucide-react';

const InsuranceHistory = () => {
    const [history, setHistory] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [openReceiptId, setOpenReceiptId] = useState(null);

    useEffect(() => {
        const fetchHistory = async () => {
            setIsLoading(true);
            try {
                const data = await getPaymentHistory();
                setHistory(data);
            } catch (error) {
                console.error("Failed to fetch history", error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchHistory();
    }, []);

    const toggleReceipt = (id) => {
        setOpenReceiptId(openReceiptId === id ? null : id);
    };

    if (isLoading) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[500px] space-y-4">
                <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
                <p className="text-slate-500 font-medium">결제 내역을 불러오는 중입니다...</p>
            </div>
        );
    }

    return (
        <div className="max-w-3xl mx-auto px-6 py-12 animate-in fade-in slide-in-from-bottom-4">
            <div className="flex justify-between items-end mb-8 border-b border-slate-200 pb-6">
                <div>
                    <h1 className="text-3xl font-black text-slate-900 flex items-center gap-3">
                        <Receipt className="w-8 h-8 text-primary-500" />
                        결제 내역
                    </h1>
                    <p className="text-slate-500 mt-2 font-medium">지금까지 구독 및 취소한 보험의 결제 내역입니다.</p>
                </div>
                <div className="hidden sm:block text-sm font-bold text-slate-400 bg-slate-100 px-4 py-2 rounded-xl">
                    총 {history.length}건
                </div>
            </div>

            {history.length === 0 ? (
                <div className="text-center py-20 bg-white rounded-3xl border border-slate-200">
                    <p className="text-slate-500">결제 내역이 없습니다.</p>
                </div>
            ) : (
                <div className="space-y-6">
                    {history.map((receipt) => {
                        const isOpen = openReceiptId === receipt.id;
                        return (
                            <div key={receipt.id} className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden hover:shadow-md transition-shadow">
                                {/* Receipt Header (Always visible) */}
                                <div
                                    className="p-6 cursor-pointer flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4"
                                    onClick={() => toggleReceipt(receipt.id)}
                                >
                                    <div className="space-y-2">
                                        <div className="flex items-center gap-3">
                                            <span className={`px-2.5 py-1 text-xs font-bold rounded-md ${receipt.status === '결제 완료' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                                                {receipt.status}
                                            </span>
                                            <span className="text-sm font-bold text-slate-400 flex items-center gap-1 text-xs">
                                                <Calendar className="w-3.5 h-3.5" />
                                                {receipt.date}
                                            </span>
                                        </div>
                                        <h3 className="font-bold text-lg text-slate-800 flex items-center gap-2">
                                            {receipt.items.length === 1
                                                ? receipt.items[0].name
                                                : `${receipt.items[0].name} 외 ${receipt.items.length - 1}건`
                                            }
                                        </h3>
                                    </div>

                                    <div className="flex items-center justify-between w-full sm:w-auto gap-6 sm:gap-4">
                                        <div className="text-left sm:text-right flex flex-col">
                                            <span className="text-xs text-slate-400 font-medium">결제 금액</span>
                                            <span className="font-black text-xl text-primary-600">{receipt.totalAmount.toLocaleString()}원</span>
                                        </div>
                                        <button className="text-slate-400 bg-slate-50 hover:bg-slate-100 p-2 rounded-full transition-colors">
                                            {isOpen ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                                        </button>
                                    </div>
                                </div>

                                {/* Receipt Details (Collapsible) */}
                                {isOpen && (
                                    <div className="p-6 bg-slate-50 border-t border-slate-100 border-dashed animate-in slide-in-from-top-4">

                                        <div className="mb-6">
                                            <h4 className="text-sm font-bold text-slate-500 mb-3 border-b-2 border-slate-200 pb-2 inline-block">가입한 보험 품목</h4>
                                            <ul className="space-y-3">
                                                {receipt.items.map((item, idx) => (
                                                    <li key={idx} className="flex justify-between items-center text-sm">
                                                        <div className="flex flex-col">
                                                            <span className="font-bold text-slate-700">{item.name} <span className="text-xs text-slate-400 ml-1">({item.company})</span></span>
                                                            <span className="text-xs text-slate-500 mt-0.5">{item.type}</span>
                                                        </div>
                                                        <span className="font-bold text-slate-600">{item.amount.toLocaleString()}원</span>
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>

                                        <div className="mb-6">
                                            <h4 className="text-sm font-bold text-slate-500 mb-3 border-b-2 border-slate-200 pb-2 inline-block">주요 보장 내용</h4>
                                            <div className="bg-white p-4 rounded-xl border border-slate-200">
                                                <ul className="space-y-2">
                                                    {receipt.coverages.map((cov, idx) => (
                                                        <li key={idx} className="flex justify-between items-center text-sm">
                                                            <span className="font-medium text-slate-600">{cov.label}</span>
                                                            <span className="font-bold text-primary-600 border-b border-primary-100 border-dotted">{cov.amount}</span>
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        </div>

                                        <div className="flex items-center justify-between text-xs text-slate-400 pt-4 border-t border-slate-200">
                                            <span className="flex items-center gap-1">
                                                <CreditCard className="w-3.5 h-3.5" />
                                                토스페이 결제
                                            </span>
                                            <span>승인번호: {receipt.receiptNumber}</span>
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
};

export default InsuranceHistory;
