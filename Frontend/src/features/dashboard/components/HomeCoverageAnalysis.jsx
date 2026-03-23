import React, { useEffect, useState } from 'react';
import { Loader2 } from 'lucide-react';
import { getCoverageAnalysis } from '@/features/home/api/home.api';

const CoverageAnalysis = () => {
    const [data, setData] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const result = await getCoverageAnalysis();
                setData(result);
            } catch (error) {
                console.error('Failed to fetch coverage analysis:', error);
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

    // 히스토그램 형태를 위한 가변 데이터 배열 (예: 일반적인 정규분포 형태)
    const distribution = [5, 12, 25, 45, 80, 100, 85, 40, 20, 8, 3];
    // 상위 %에 해당하는 대략적인 인덱스 (0% 오른쪽, 100% 왼쪽이라 가정)
    // 상위 18%이므로 대략 오른쪽에서 두번째 기둥 주변
    const targetIndex = 8;

    return (
        <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm flex flex-col gap-4">
            <h3 className="text-slate-800 font-bold">보장 수준 백분위 분석</h3>
            <p className="text-slate-700 font-medium">
                <span className="text-primary-600 font-bold">{data.name}</span>님은 동일 나이대에서 상위 <span className="text-primary-600 font-bold">{data.percentile}%</span> 보장을 받고 있어요!
            </p>

            {/* Histogram Chart UI */}
            <div className="relative mt-6 pt-6 pb-2 px-2 flex items-end justify-between h-32 w-full gap-1 sm:gap-2">
                {/* Pointer for current user percentile */}
                <div
                    className="absolute top-0 flex flex-col items-center transform -translate-x-1/2 transition-all duration-1000 ease-out"
                    style={{ left: `${(targetIndex / (distribution.length - 1)) * 100}%` }}
                >
                    <div className="bg-slate-800 text-white text-xs font-bold px-2 py-1 rounded-md whitespace-nowrap">
                        상위 {data.percentile}%
                    </div>
                    <div className="w-0 h-0 border-l-[6px] border-r-[6px] border-t-[6px] border-l-transparent border-r-transparent border-t-slate-800 mt-0.5"></div>
                </div>

                {/* Bars */}
                {distribution.map((height, idx) => (
                    <div
                        key={idx}
                        className={`w-full rounded-t-sm transition-all duration-500 ${idx === targetIndex ? 'bg-primary-500' : 'bg-slate-200'}`}
                        style={{ height: `${height}%` }}
                    ></div>
                ))}
            </div>

            <div className="flex justify-between text-xs text-slate-400 font-medium px-1">
                <span>하위 99%</span>
                <span>상위 1%</span>
            </div>
        </div>
    );
};

export default CoverageAnalysis;
