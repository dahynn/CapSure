import React from 'react';
import { ArrowRight, CheckCircle } from 'lucide-react';

const SubscribeComplete = ({ selectedProducts, totalPremium, subscriptionId, capsuleName, onNext }) => {
    const selectedItemsDetails = selectedProducts;

    return (
        <div className="max-w-2xl mx-auto space-y-12 animate-in fade-in zoom-in-95 duration-500 py-12">
            <div className="text-center space-y-6">
                <div className="w-24 h-24 bg-emerald-100 text-emerald-500 rounded-full flex flex-col items-center justify-center mx-auto mb-8 shadow-inner shadow-emerald-200">
                    <CheckCircle className="w-12 h-12" />
                </div>

                <h2 className="text-4xl font-black text-slate-800 tracking-tight">{capsuleName}</h2>
                <p className="text-lg text-slate-500 leading-relaxed max-w-sm mx-auto">
                    성공적으로 맞춤형 캡슐 보험 구독을 시작했습니다!
                    아래 구독 내역을 확인해 보세요.
                </p>
            </div>

            <div className="bg-white p-8 rounded-3xl shadow-xl shadow-slate-200/50 border border-slate-100">
                <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
                    <h3 className="font-bold text-slate-800 text-xl">나만의 캡슐 보장 내역</h3>
                    <div className="text-right">
                        <p className="text-xs font-semibold text-slate-400">총 결제 금액</p>
                        <p className="text-lg font-black text-slate-800">{totalPremium.toLocaleString()}원</p>
                    </div>
                </div>

                {subscriptionId && (
                    <p className="text-sm text-slate-400 mb-6">구독 번호: {subscriptionId}</p>
                )}

                <ul className="space-y-4">
                    {selectedItemsDetails.map((item, idx) => (
                        <li key={idx} className="flex justify-between items-center p-5 bg-slate-50 rounded-2xl border border-slate-100 hover:bg-white hover:shadow-md hover:-translate-y-0.5 transition-all">
                            <div className="flex items-center gap-4">
                                <div className={`w-3 h-3 rounded-full ${item.categoryLabel === '실손' ? 'bg-teal-500' :
                                    item.categoryLabel === '암' ? 'bg-rose-500' :
                                        item.categoryLabel === '상해' ? 'bg-indigo-500' : 'bg-blue-500'}`} />
                                <div className="flex flex-col gap-1">
                                    <span className="font-bold text-slate-700">{item.productName}</span>
                                    <span className="text-xs font-semibold text-slate-400">{item.companyName}</span>
                                </div>
                            </div>
                            <span className="font-bold text-slate-700">{item.monthlyPrice.toLocaleString()}원</span>
                        </li>
                    ))}
                </ul>
            </div>

            <div className="flex justify-center pt-8">
                <button
                    onClick={onNext}
                    className="px-10 py-5 bg-slate-800 text-white font-bold text-lg rounded-2xl shadow-xl shadow-slate-300 hover:bg-slate-900 hover:-translate-y-1 transition-all flex items-center gap-3 group"
                >
                    구독 확인하러 가기
                    <ArrowRight className="w-6 h-6 group-hover:translate-x-1 transition-transform" />
                </button>
            </div>
        </div>
    );
};

export default SubscribeComplete;
