import React from 'react';
import { CircleAlert } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const CATEGORY_LABELS = {
    DEATH: '사망',
    CANCER: '암',
    BRAIN_HEART: '뇌/심장',
    ACTUAL_LOSS: '실손',
    SURGERY: '수술',
    ACCIDENT: '상해',
    LIABILITY: '배상',
    ETC: '기타',
};

const CoverageAnalysis = ({ diagnosisReport, coveragePercentile }) => {
    const navigate = useNavigate();
    const gradientId = React.useId();
    const score = Math.max(0, Math.min(coveragePercentile?.coveragePercentile || 0, 100));
    const total = coveragePercentile?.totalCategoryCount || 7;
    const covered = coveragePercentile?.coveredCategoryCount || 0;
    const diagnoses = diagnosisReport?.diagnoses || [];
    const coveredBadges = diagnoses
        .filter((item) => item.insured)
        .map((item) => CATEGORY_LABELS[item.categoryCode] || item.categoryName || item.categoryCode)
        .slice(0, 6);

    const radius = 48;
    const stroke = 7;
    const normalizedRadius = radius - stroke / 2;
    const circumference = 2 * Math.PI * normalizedRadius;
    const strokeDashoffset = circumference * (1 - score / 100);

    return (
        <section className="mb-7">
            <div className="mb-4 flex items-center gap-2 text-slate-100">
                <CircleAlert className="h-5 w-5 text-slate-300" />
                <h3 className="text-[17px] font-semibold">정밀 진단 리포트</h3>
            </div>

            <button
                type="button"
                onClick={() => navigate('/dashboard/diagnosis-report', { state: { diagnosisReport } })}
                className="w-full rounded-[30px] border border-slate-800 bg-[#161B26] px-6 py-7 text-left shadow-xl transition hover:border-slate-700"
            >
                <div className="mb-3 flex justify-end">
                    <span className="inline-flex items-center rounded-full bg-[#1F2736] px-2.5 py-1 text-[11px] font-semibold text-[#82D8FC]">상세보기</span>
                </div>
                <div className="flex items-center gap-5">
                    <div className="relative h-32 w-32 shrink-0">
                        <svg className="h-32 w-32 -rotate-90" viewBox="0 0 96 96" aria-hidden="true">
                            <defs>
                                <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="0%">
                                    <stop offset="0%" stopColor="#82D8FC" />
                                    <stop offset="56%" stopColor="#D8B7EE" />
                                    <stop offset="100%" stopColor="#F2CE49" />
                                </linearGradient>
                            </defs>
                            <circle
                                cx={radius}
                                cy={radius}
                                r={normalizedRadius}
                                fill="none"
                                stroke="#1C2640"
                                strokeWidth={stroke}
                            />
                            <circle
                                cx={radius}
                                cy={radius}
                                r={normalizedRadius}
                                fill="none"
                                stroke={`url(#${gradientId})`}
                                strokeWidth={stroke}
                                strokeDasharray={circumference}
                                strokeDashoffset={strokeDashoffset}
                                strokeLinecap="round"
                            />
                        </svg>
                        <div className="absolute inset-0 flex items-center justify-center text-[19px] font-semibold text-[#DDE4F2]">
                            {score}%
                        </div>
                    </div>

                    <div className="min-w-0">
                        <p className="text-[15px] leading-relaxed text-slate-300">
                            현재 <span className="font-semibold text-[#82D8FC]">{total}개 중 {covered}개</span>의 보장 카테고리가 안전하게 보호되고 있습니다.
                        </p>

                        <div className="mt-3 flex flex-wrap gap-2">
                            {coveredBadges.length ? (
                                coveredBadges.map((badge) => (
                                    <span
                                        key={badge}
                                        className="rounded-xl bg-[#1F2736] px-3 py-1.5 text-[13px] font-medium text-[#82D8FC]"
                                    >
                                        {badge}
                                    </span>
                                ))
                            ) : (
                                <span className="rounded-xl bg-[#1F2736] px-3 py-1.5 text-[13px] font-medium text-slate-300">
                                    보호 중인 항목이 없습니다
                                </span>
                            )}
                        </div>
                    </div>
                </div>
            </button>
        </section>
    );
};

export default CoverageAnalysis;
