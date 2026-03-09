import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyCapsuleInsurance } from '../capsule/api/capsuleInsurance.api';
import { Loader2, ShieldCheck, Box, ChevronDown, ChevronUp } from 'lucide-react';
import CapsuleModify from '../capsuleModify/CapsuleModify';

const MyCapsuleInsurance = () => {
    const navigate = useNavigate();
    const [viewTab, setViewTab] = useState('this-month'); // 'this-month' | 'next-month'
    const [subData, setSubData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchMyCapsules = async () => {
            setIsLoading(true);
            try {
                const data = await getMyCapsuleInsurance();
                setSubData(data);
            } catch (error) {
                console.error("Failed to fetch my capsule info", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchMyCapsules();
    }, []);

    if (isLoading) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[500px] space-y-4">
                <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
                <p className="text-slate-500 font-medium">내 캡슐 정보를 불러오는 중입니다...</p>
            </div>
        );
    }

    if (!subData) return null;

    const { targetAmount, selectedCells } = subData;

    return (
        <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 py-8 px-4 sm:px-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end mb-4 border-b border-slate-200 pb-4">
                <div className="flex items-center gap-4">
                    <div className="w-14 h-14 bg-primary-100 text-primary-500 rounded-2xl flex items-center justify-center shadow-sm">
                        <ShieldCheck className="w-7 h-7" />
                    </div>
                    <div>
                        <h2 className="text-3xl font-black text-slate-800">내 캡슐 보험 조회하기</h2>
                        <p className="text-slate-600 mt-1">현재 활성화된 구독 상태와 보장 내역을 확인할 수 있습니다.</p>
                    </div>
                </div>

                <div className="flex bg-slate-200/60 p-1.5 rounded-xl mt-6 sm:mt-0">
                    <button
                        onClick={() => setViewTab('this-month')}
                        className={`px-4 py-2 text-sm font-bold rounded-lg transition-all ${viewTab === 'this-month' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                    >
                        이번달 구독 확인하기
                    </button>
                    <button
                        onClick={() => setViewTab('next-month')}
                        className={`px-4 py-2 text-sm font-bold rounded-lg transition-all ${viewTab === 'next-month' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                    >
                        다음달 구독 변경 예약하기
                    </button>
                </div>
            </div>

            {viewTab === 'this-month' && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                    {/* Left: Cells */}
                    <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 flex flex-col items-center">
                        <div className="flex justify-between items-center w-full mb-6 relative">
                            <h3 className="font-bold text-slate-800 text-lg flex items-center gap-2">
                                <Box className="w-5 h-5 text-primary-500" />
                                현재 내 블록 판
                            </h3>
                            <div className="flex items-center gap-1 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200">
                                <span className="text-xs font-bold text-slate-500">구독 액수</span>
                                <span className="text-sm font-black text-primary-600">{targetAmount}만원</span>
                            </div>
                        </div>

                        <div className="bg-slate-50 rounded-2xl border border-slate-200 p-8 grid grid-cols-5 gap-3 w-full max-w-md place-content-center">
                            {selectedCells.map((cell, i) => (
                                <div
                                    key={i}
                                    className={`aspect-square sm:aspect-auto sm:h-20 rounded-xl border flex flex-col items-center justify-center shadow-[inset_0_2px_4px_rgba(255,255,255,0.6)] ${cell ? cell.category.color : 'bg-white/50 border-slate-200 text-slate-300'
                                        }`}
                                >
                                    {cell ? (
                                        <span className="font-bold text-[10px] sm:text-xs text-center px-1 leading-tight">{cell.name}</span>
                                    ) : (
                                        <span className="text-xs font-bold opacity-50">빈 칸</span>
                                    )}
                                </div>
                            ))}
                        </div>

                        <div className="mt-auto pt-8 w-full text-center">
                            <button
                                onClick={() => navigate('/capsule-cancel')}
                                className="text-sm font-bold text-red-500 hover:text-red-600 underline underline-offset-4 decoration-red-200 hover:decoration-red-400 transition-colors"
                            >
                                현재 보험 취소하기
                            </button>
                        </div>
                    </div>

                    {/* Right: Coverages List (Accordion-like UI Mock) */}
                    <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 flex flex-col h-full">
                        <h3 className="font-bold text-slate-800 text-lg mb-6">적용 중인 보장 항목</h3>

                        <div className="space-y-4 overflow-y-auto pr-2 custom-scrollbar">
                            {selectedCells.filter(c => c !== null).map((cell, idx) => (
                                <div key={idx} className="border border-slate-100 rounded-2xl overflow-hidden shadow-sm">
                                    <div className="p-4 bg-slate-50 flex items-center justify-between">
                                        <div className="flex items-center gap-3">
                                            <div className={`w-3 h-3 rounded-full ${cell.category.color.split(' ')[0]}`} />
                                            <span className="font-bold text-slate-700">{cell.name} <span className="text-xs text-slate-400 font-normal">({cell.category.name})</span></span>
                                        </div>
                                    </div>
                                    <div className="p-4 bg-white border-t border-slate-100 space-y-3">
                                        <div className="flex justify-between items-center text-sm border-b border-dashed border-slate-100 pb-2">
                                            <span className="text-slate-600 font-medium">기본 입원 일당</span>
                                            <span className="font-bold text-primary-600">500만원</span>
                                        </div>
                                        <div className="flex justify-between items-center text-sm border-b border-dashed border-slate-100 pb-2">
                                            <span className="text-slate-600 font-medium">{cell.category.id === 'pet' ? '수술비 지원' : '통원 치료비'}</span>
                                            <span className="font-bold text-primary-600">3,000만원</span>
                                        </div>
                                        <div className="flex justify-between items-center text-sm">
                                            <span className="text-slate-600 font-medium">약제비 지원</span>
                                            <span className="font-bold text-primary-600">20만원</span>
                                        </div>
                                    </div>
                                </div>
                            ))}
                            {selectedCells.filter(c => c !== null).length === 0 && (
                                <div className="py-12 text-center text-slate-400">
                                    구독 중인 보장 내역이 없습니다.
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {viewTab === 'next-month' && (
                <div className="animate-in fade-in duration-300">
                    <CapsuleModify />
                </div>
            )}

        </div>
    );
};

export default MyCapsuleInsurance;
