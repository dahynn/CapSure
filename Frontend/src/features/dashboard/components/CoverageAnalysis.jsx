import React from 'react';
import { CircleAlert, ShieldCheck, Sparkles } from 'lucide-react';

const scoreMessage = (score) => {
    if (score >= 85) {
        return '핵심 보장이 전반적으로 잘 갖춰져 있습니다.';
    }
    if (score >= 55) {
        return '기본 보장은 갖췄지만 몇몇 카테고리는 보강 여지가 있습니다.';
    }
    return '미가입 카테고리가 많아 우선 보완이 필요합니다.';
};

const CoverageAnalysis = ({ diagnosisReport, coveragePercentile }) => {
    const score = coveragePercentile?.coveragePercentile || 0;

    return (
        <div className="mb-8">
            <div className="mb-4 flex items-center justify-between px-1">
                <h2 className="text-[20px] font-bold tracking-tight text-white">정밀 진단 리포트</h2>
                <div className="flex h-6 w-6 items-center justify-center rounded-full border border-slate-700 bg-[#161B26] text-[#82D8FC]">
                    <CircleAlert className="h-3.5 w-3.5" strokeWidth={3} />
                </div>
            </div>

            <div className="rounded-3xl border border-slate-800/80 bg-[#10141D] p-6 shadow-lg">
                <div className="mb-5 flex items-end justify-between gap-4">
                    <div>
                        <p className="mb-1 text-[13px] font-medium text-[#9D9DA4]">7대 보장 충족률</p>
                        <div className="flex items-baseline gap-1">
                            <h3 className="text-[30px] font-black tracking-tight text-white">{score}</h3>
                            <span className="text-[26px] font-black text-[#F2BEF7]">%</span>
                        </div>
                    </div>
                    <span className="pb-1 text-[11px] font-black uppercase tracking-widest text-[#F6CD3C]">
                        Coverage Score
                    </span>
                </div>

                <div className="relative mb-5 h-3.5 w-full overflow-hidden rounded-full bg-[#1C212E]">
                    <div
                        className="absolute left-0 top-0 h-full rounded-full"
                        style={{
                            width: `${Math.max(0, Math.min(score, 100))}%`,
                            background: 'linear-gradient(90deg, #82D8FC 0%, #F2BEF7 50%, #F6CD3C 100%)',
                        }}
                    />
                </div>

                <div className="mb-6 flex gap-3 rounded-2xl border border-[#2B233A] bg-[#191523] p-4 text-white">
                    <Sparkles className="mt-0.5 h-5 w-5 flex-shrink-0 text-[#F2BEF7]" />
                    <div>
                        <p className="text-[13px] leading-snug text-[#BBBBCA]">
                            {coveragePercentile?.message || scoreMessage(score)}
                        </p>
                        {diagnosisReport?.description ? (
                            <p className="mt-2 text-[12px] text-slate-400">{diagnosisReport.description}</p>
                        ) : null}
                    </div>
                </div>

                <div className="space-y-3">
                    {(diagnosisReport?.diagnoses || []).map((item) => {
                        const isInsured = item.insured;
                        return (
                            <div
                                key={item.categoryCode}
                                className="rounded-2xl border border-slate-800 bg-[#151A24] px-4 py-4"
                            >
                                <div className="flex items-start justify-between gap-4">
                                    <div className="min-w-0">
                                        <div className="flex items-center gap-2">
                                            <span
                                                className={`h-2.5 w-2.5 rounded-full ${
                                                    isInsured ? 'bg-[#79E79C]' : 'bg-[#F36B7F]'
                                                }`}
                                            />
                                            <p className="text-[15px] font-semibold text-white">{item.categoryName}</p>
                                            <span
                                                className={`rounded-full px-2.5 py-1 text-[11px] font-bold ${
                                                    isInsured
                                                        ? 'bg-[#1E2D24] text-[#8AE7A3]'
                                                        : 'bg-[#351923] text-[#FF9CAC]'
                                                }`}
                                            >
                                                {item.status}
                                            </span>
                                        </div>
                                        {item.coverageNames?.length ? (
                                            <p className="mt-2 text-[12px] leading-relaxed text-slate-400">
                                                보유 보장: {item.coverageNames.join(', ')}
                                            </p>
                                        ) : null}
                                    </div>
                                    {isInsured ? <ShieldCheck className="h-5 w-5 text-[#82D8FC]" /> : null}
                                </div>

                                {!isInsured && item.recommendedProduct ? (
                                    <div className="mt-3 rounded-xl border border-dashed border-slate-700 bg-[#10141D] px-3 py-3">
                                        <p className="text-[11px] font-bold uppercase tracking-widest text-[#82D8FC]">
                                            추천 상품
                                        </p>
                                        <p className="mt-1 text-[13px] font-semibold text-white">
                                            {item.recommendedProduct.productName}
                                        </p>
                                        <p className="mt-1 text-[12px] text-slate-400">
                                            {item.recommendedProduct.companyName} ·{' '}
                                            {new Intl.NumberFormat('ko-KR').format(
                                                Number(item.recommendedProduct.monthlyPrice || 0),
                                            )}
                                            원/월
                                        </p>
                                    </div>
                                ) : null}
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default CoverageAnalysis;
