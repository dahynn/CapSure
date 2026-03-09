import React, { useEffect, useState } from 'react';
import { getMyCapsuleInsurance } from '../api/capsuleInsurance.api';
import { Loader2, ShieldCheck, Box } from 'lucide-react';

const MyCapsuleInsurance = () => {
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
        <div className="max-w-4xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 py-8">
            <div className="flex items-center gap-4 bg-primary-50 p-6 rounded-3xl border border-primary-100">
                <div className="w-16 h-16 bg-white text-primary-500 rounded-2xl flex items-center justify-center shadow-sm">
                    <ShieldCheck className="w-8 h-8" />
                </div>
                <div>
                    <h2 className="text-2xl font-black text-slate-800">내 캡슐 보험 조회하기</h2>
                    <p className="text-slate-600 mt-1">현재 활성화된 구독 상태와 보장 내역을 확인할 수 있습니다.</p>
                </div>
            </div>

            <div className="bg-white p-8 rounded-3xl shadow-lg border border-slate-100 flex flex-col items-center gap-8">

                <div className="flex items-center justify-between w-full border-b border-slate-100 pb-4">
                    <h3 className="font-bold text-slate-800 text-lg flex items-center gap-2">
                        <Box className="w-5 h-5 text-primary-500" />
                        나의 조합 테이블
                    </h3>
                    <div className="flex items-center gap-2 bg-slate-50 px-4 py-2 rounded-xl border border-slate-200">
                        <span className="text-sm font-bold text-slate-600">구독 금액:</span>
                        <span className="text-xl font-black text-primary-600">{targetAmount}만원</span>
                    </div>
                </div>

                <div className="bg-slate-100 rounded-2xl border-2 border-slate-200 p-8 grid grid-cols-5 gap-2 w-full max-w-2xl place-content-center">
                    {/* Render Read-Only Grid Cells */}
                    {selectedCells.map((cell, i) => (
                        <div
                            key={i}
                            className={`aspect-square sm:aspect-auto sm:h-24 rounded-xl border flex flex-col items-center justify-center shadow-sm ${cell ? cell.category.color : 'bg-white/50 border-slate-200 text-slate-300'
                                }`}
                        >
                            {cell ? (
                                <>
                                    <span className="font-bold text-[10px] sm:text-xs text-center px-1 leading-tight">{cell.name}</span>
                                </>
                            ) : (
                                <span className="text-xs font-bold opacity-50">빈 칸</span>
                            )}
                        </div>
                    ))}
                </div>

                <div className="w-full mt-4 text-center">
                    <p className="text-sm font-bold text-slate-400 bg-slate-50 py-3 rounded-xl border border-slate-100 max-w-xs mx-auto">
                        ※ 조합 테이블은 다음 구독 변경 기간에만 수정이 가능합니다.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default MyCapsuleInsurance;
