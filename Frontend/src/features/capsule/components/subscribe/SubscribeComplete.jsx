import React from 'react';
import { ArrowRight, CheckCircle } from 'lucide-react';

const SubscribeComplete = ({ selectedCells, onNext }) => {
    // Unique list of selected items for summary
    const selectedItemsDetails = selectedCells.filter(c => c !== null).reduce((acc, current) => {
        const x = acc.find(item => item.groupId === current.groupId);
        if (!x) return acc.concat([current]);
        return acc;
    }, []);

    return (
        <div className="max-w-2xl mx-auto space-y-12 animate-in fade-in zoom-in-95 duration-500 py-12">
            <div className="text-center space-y-6">
                <div className="w-24 h-24 bg-emerald-100 text-emerald-500 rounded-full flex flex-col items-center justify-center mx-auto mb-8 shadow-inner shadow-emerald-200">
                    <CheckCircle className="w-12 h-12" />
                </div>

                <h2 className="text-4xl font-black text-slate-800 tracking-tight">보험 가입 완료</h2>
                <p className="text-lg text-slate-500 leading-relaxed max-w-sm mx-auto">
                    성공적으로 맞춤형 캡슐 보험 구독을 시작했습니다!
                    아래 구독 내역을 확인해 보세요.
                </p>
            </div>

            <div className="bg-white p-8 rounded-3xl shadow-xl shadow-slate-200/50 border border-slate-100">
                <h3 className="font-bold text-slate-800 text-xl border-b border-slate-100 pb-4 mb-6">나만의 캡슐 보장 내역</h3>

                <ul className="space-y-4">
                    {selectedItemsDetails.map((item, idx) => (
                        <li key={idx} className="flex justify-between items-center p-5 bg-slate-50 rounded-2xl border border-slate-100 hover:bg-white hover:shadow-md hover:-translate-y-0.5 transition-all">
                            <div className="flex items-center gap-4">
                                <div className={`w-3 h-3 rounded-full ${item.category.id === 'shilson' ? 'bg-teal-500' :
                                    item.category.id === 'disease' ? 'bg-rose-500' :
                                        item.category.id === 'pet' ? 'bg-amber-500' :
                                            item.category.id === 'driver' ? 'bg-indigo-500' : 'bg-blue-500'}`} />
                                <div className="flex flex-col gap-1">
                                    <span className="font-bold text-slate-700">{item.name}</span>
                                    <span className="text-xs font-semibold text-slate-400">{item.company}</span>
                                </div>
                            </div>
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
