import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMyCapsureInsurance } from '../../capsure/api/capsureInsurance.api';
import { Loader2, ShieldCheck, Box, ChevronDown, ChevronUp } from 'lucide-react';
import CapsureModify from '../../capsureModify/CapsureModify';

const MyCapsureInsurance = () => {
  const navigate = useNavigate();
  const [viewTab, setViewTab] = useState('this-month'); // 'this-month' | 'next-month'
  const [subData, setSubData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchMyCapsures = async () => {
      setIsLoading(true);
      try {
        const data = await getMyCapsureInsurance();
        setSubData(data);
      } catch (error) {
        console.error('Failed to fetch my capsure info', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchMyCapsures();
  }, []);

  if (isLoading) {
    return (
      <div className="flex min-h-[500px] flex-col items-center justify-center space-y-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary-500" />
        <p className="font-medium text-slate-500">내 캡슐 정보를 불러오는 중입니다...</p>
      </div>
    );
  }

  if (!subData) return null;

  const { targetAmount, selectedCells } = subData;

  return (
    <div className="animate-in fade-in slide-in-from-bottom-4 mx-auto max-w-6xl space-y-8 px-4 py-8 sm:px-6">
      <div className="mb-4 flex flex-col items-start justify-between border-b border-slate-200 pb-4 sm:flex-row sm:items-end">
        <div className="flex items-center gap-4">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary-100 text-primary-500 shadow-sm">
            <ShieldCheck className="h-7 w-7" />
          </div>
          <div>
            <h2 className="text-3xl font-black text-slate-800">내 캡슐 보험 조회하기</h2>
            <p className="mt-1 text-slate-600">
              현재 활성화된 구독 상태와 보장 내역을 확인할 수 있습니다.
            </p>
          </div>
        </div>

        <div className="mt-6 flex rounded-xl bg-slate-200/60 p-1.5 sm:mt-0">
          <button
            onClick={() => setViewTab('this-month')}
            className={`rounded-lg px-4 py-2 text-sm font-bold transition-all ${viewTab === 'this-month' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
          >
            이번달 구독 확인하기
          </button>
          <button
            onClick={() => setViewTab('next-month')}
            className={`rounded-lg px-4 py-2 text-sm font-bold transition-all ${viewTab === 'next-month' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
          >
            다음달 구독 변경 예약하기
          </button>
        </div>
      </div>

      {viewTab === 'this-month' && (
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-2">
          {/* Left: Cells */}
          <div className="flex flex-col items-center rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="relative mb-6 flex w-full items-center justify-between">
              <h3 className="flex items-center gap-2 text-lg font-bold text-slate-800">
                <Box className="h-5 w-5 text-primary-500" />
                현재 내 캡슐 판
              </h3>
              <div className="flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5">
                <span className="text-xs font-bold text-slate-500">구독 액수</span>
                <span className="text-sm font-black text-primary-600">{targetAmount}만원</span>
              </div>
            </div>

            <div className="grid w-full max-w-md grid-cols-5 place-content-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-8">
              {selectedCells.map((cell, i) => (
                <div
                  key={i}
                  className={`flex aspect-square flex-col items-center justify-center rounded-xl border shadow-[inset_0_2px_4px_rgba(255,255,255,0.6)] sm:aspect-auto sm:h-20 ${
                    cell ? cell.category.color : 'border-slate-200 bg-white/50 text-slate-300'
                  }`}
                >
                  {cell ? (
                    <span className="px-1 text-center text-[10px] font-bold leading-tight sm:text-xs">
                      {cell.productName}
                    </span>
                  ) : (
                    <span className="text-xs font-bold opacity-50">빈 칸</span>
                  )}
                </div>
              ))}
            </div>

            <div className="mt-auto w-full pt-8 text-center">
              <button
                onClick={() => navigate('/capsure-cancel')}
                className="text-sm font-bold text-red-500 underline decoration-red-200 underline-offset-4 transition-colors hover:text-red-600 hover:decoration-red-400"
              >
                현재 보험 취소하기
              </button>
            </div>
          </div>

          {/* Right: Coverages List (Accordion-like UI Mock) */}
          <div className="flex h-full flex-col rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="mb-6 text-lg font-bold text-slate-800">적용 중인 보장 항목</h3>

            <div className="custom-scrollbar space-y-4 overflow-y-auto pr-2">
              {selectedCells
                .filter((c) => c !== null)
                .map((cell, idx) => (
                  <div
                    key={idx}
                    className="overflow-hidden rounded-2xl border border-slate-100 shadow-sm"
                  >
                    <div className="flex items-center justify-between bg-slate-50 p-4">
                      <div className="flex items-center gap-3">
                        <div
                          className={`h-3 w-3 rounded-full ${cell.category.color.split(' ')[0]}`}
                        />
                        <span className="font-bold text-slate-700">
                          {cell.productName}{' '}
                          <span className="text-xs font-normal text-slate-400">
                            ({cell.category.name})
                          </span>
                        </span>
                      </div>
                    </div>
                    <div className="space-y-3 border-t border-slate-100 bg-white p-4">
                      <div className="flex items-center justify-between border-b border-dashed border-slate-100 pb-2 text-sm">
                        <span className="font-medium text-slate-600">기본 입원 일당</span>
                        <span className="font-bold text-primary-600">500만원</span>
                      </div>
                      <div className="flex items-center justify-between border-b border-dashed border-slate-100 pb-2 text-sm">
                        <span className="font-medium text-slate-600">
                          {cell.category.id === 'pet' ? '수술비 지원' : '통원 치료비'}
                        </span>
                        <span className="font-bold text-primary-600">3,000만원</span>
                      </div>
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-medium text-slate-600">약제비 지원</span>
                        <span className="font-bold text-primary-600">20만원</span>
                      </div>
                    </div>
                  </div>
                ))}
              {selectedCells.filter((c) => c !== null).length === 0 && (
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
          <CapsureModify />
        </div>
      )}
    </div>
  );
};

export default MyCapsureInsurance;
