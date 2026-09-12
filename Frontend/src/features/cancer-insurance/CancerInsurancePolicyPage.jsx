import React, { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import {
    AlertCircle,
    CalendarClock,
    CheckCircle2,
    ClipboardPlus,
    FileCheck2,
    FileKey2,
    Fingerprint,
    Home,
    Loader2,
    RotateCcw,
    ShieldCheck,
    UserRoundCheck,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';
import { getCancerPolicy } from './api/cancerInsurance.api';
import { useCancerInsurance } from './context/CancerInsuranceContext';

const currency = new Intl.NumberFormat('ko-KR');

const formatWon = (value) => `${currency.format(Number(value || 0))}원`;

const formatDate = (value) => {
    if (!value) return '-';
    return new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
    }).format(new Date(value));
};

// 증권번호는 계약을 식별해야 하지만, UUID 전체를 고객 화면에 그대로 노출할 필요는 없습니다.
// 원본 값은 API·계약 원장에 보존하고 화면에는 읽기 쉬운 식별자만 표시합니다.
const formatPolicyNo = (value) => {
    if (!value) return '-';
    const normalized = String(value).replace(/[^a-zA-Z0-9]/g, '');
    if (normalized.length <= 12) return value;
    return `${normalized.slice(0, 8)} ··· ${normalized.slice(-4)}`;
};

const conditionText = (coverage, snapshotCoverage) => {
    const parts = [];
    if (snapshotCoverage?.waitingPeriodDays > 0) {
        parts.push(`면책 ${snapshotCoverage.waitingPeriodDays}일`);
    }
    if (snapshotCoverage?.reductionPeriodDays > 0) {
        parts.push(`감액 ${snapshotCoverage.reductionPeriodDays}일`);
    }
    if (parts.length === 0 && coverage.coverageStartAt) {
        parts.push(`${formatDate(coverage.coverageStartAt)} 보장개시`);
    }
    return parts.join(' · ') || '약관상 보장개시일 적용';
};

const CancerInsurancePolicyPage = () => {
    const navigate = useNavigate();
    const {
        flowIds,
        policy,
        setPolicy,
        resetClaim,
        resetFlow,
    } = useCancerInsurance();
    const [loading, setLoading] = useState(Boolean(flowIds.policyId && !policy));
    const [error, setError] = useState('');

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
        let active = true;

        const restorePolicy = async () => {
            if (!flowIds.policyId) {
                setLoading(false);
                return;
            }
            try {
                const restored = policy || await getCancerPolicy(flowIds.policyId);
                if (active) setPolicy(restored);
            } catch (requestError) {
                if (active) {
                    setError(requestError.message || '보험증권을 불러오지 못했습니다.');
                }
            } finally {
                if (active) setLoading(false);
            }
        };

        restorePolicy();
        return () => {
            active = false;
        };
    }, []);

    const quoteSnapshot = policy?.policyVersion?.snapshot?.quote;
    const snapshotCoverages = quoteSnapshot?.coverages || [];
    const snapshotByCoverageId = useMemo(() => new Map(
        snapshotCoverages.map((coverage) => [coverage.productCoverageId, coverage]),
    ), [snapshotCoverages]);

    const restart = () => {
        resetFlow();
        navigate('/cancer-insurance', { replace: true });
    };

    const startClaim = () => {
        resetClaim();
        navigate('/cancer-insurance/claim');
    };

    if (!flowIds.policyId) {
        return <Navigate to="/cancer-insurance/payment" replace />;
    }

    if (loading) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center gap-4 px-6 text-center">
                <Loader2 className="h-9 w-9 animate-spin text-[#82D8FC]" />
                <div>
                    <p className="font-bold text-white">계약 원장에서 보험증권을 불러오고 있습니다</p>
                    <p className="mt-1 text-sm text-slate-500">가입 당시 상품·약관·담보 Snapshot을 확인합니다.</p>
                </div>
            </div>
        );
    }

    if (error || !policy) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center px-8 text-center">
                <AlertCircle className="h-10 w-10 text-rose-300" />
                <p className="mt-4 font-bold text-white">보험증권을 불러오지 못했습니다</p>
                <p className="mt-2 text-sm leading-6 text-slate-400">{error}</p>
                <AppButton
                    onClick={() => navigate('/cancer-insurance/payment')}
                    fullWidth={false}
                    className="mt-6 px-6"
                >
                    결제 결과로 돌아가기
                </AppButton>
            </div>
        );
    }

    return (
        <div className="min-w-0 overflow-x-clip px-5 pb-36 pt-6 sm:px-6 sm:pt-8">
            <section className="overflow-hidden rounded-[20px] border border-slate-700/80 bg-[#0A1424] p-7">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <p className="text-[11px] font-medium text-[#82D8FC]">INSURANCE POLICY</p>
                        <h1 className="mt-2 text-2xl font-semibold tracking-[-0.03em] text-white">
                            암보험 가입이 완료됐습니다
                        </h1>
                        <p className="mt-3 max-w-sm text-sm leading-6 text-slate-300">
                            보험증권은 보험계약의 성립과 약속된 보장내용을 증명하는 계약 문서입니다.
                        </p>
                    </div>
                    <span className="shrink-0 text-[#82D8FC]">
                        <FileCheck2 className="h-7 w-7" />
                    </span>
                </div>

                <div className="mt-7 grid grid-cols-2 gap-3">
                    <div className="min-w-0 rounded-xl border border-slate-700/70 bg-slate-900/40 p-3.5 sm:p-4">
                        <p className="text-[11px] font-bold text-slate-500">증권번호</p>
                        <p className="mt-2 truncate text-xs font-semibold tracking-[0.02em] text-white sm:text-sm" title={policy.policyNo}>
                            {formatPolicyNo(policy.policyNo)}
                        </p>
                    </div>
                    <div className="min-w-0 rounded-xl border border-slate-700/70 bg-slate-900/40 p-3.5 sm:p-4">
                        <p className="text-[11px] font-bold text-slate-500">계약 상태</p>
                        <p className="mt-2 flex items-center gap-1.5 text-sm font-semibold text-[#82D8FC]">
                            <CheckCircle2 className="h-4 w-4" /> {policy.status}
                        </p>
                    </div>
                </div>
            </section>

            <section className="mt-6 rounded-2xl border border-slate-800 bg-[#09111F] p-5">
                <div className="mb-5 flex items-center gap-3">
                    <span className="text-[#F2BEF7]">
                        <UserRoundCheck className="h-5 w-5" />
                    </span>
                    <div>
                        <p className="text-[11px] font-medium text-[#F2BEF7]">계약 기본정보</p>
                        <h2 className="mt-1 text-lg font-semibold text-white">가입 당시 내용</h2>
                    </div>
                </div>
                <dl className="space-y-3 text-sm">
                    <div className="flex items-center justify-between gap-4">
                        <dt className="text-slate-500">보험상품</dt>
                        <dd className="text-right font-bold text-white">{quoteSnapshot?.productName || '-'}</dd>
                    </div>
                    <div className="flex items-center justify-between gap-4">
                        <dt className="text-slate-500">월 보험료</dt>
                        <dd className="font-semibold text-[#82D8FC]">{formatWon(quoteSnapshot?.monthlyPremium)}</dd>
                    </div>
                    <div className="flex items-center justify-between gap-4">
                        <dt className="text-slate-500">계약 활성일</dt>
                        <dd className="font-bold text-white">{formatDate(policy.activatedAt)}</dd>
                    </div>
                    <div className="flex items-center justify-between gap-4">
                        <dt className="text-slate-500">증권 버전</dt>
                        <dd className="font-bold text-white">v{policy.policyVersion?.version}</dd>
                    </div>
                </dl>
            </section>

            <section className="mt-6">
                <div className="mb-4 flex items-center gap-3">
                    <span className="text-[#82D8FC]">
                        <ShieldCheck className="h-5 w-5" />
                    </span>
                    <div>
                        <p className="text-[11px] font-medium text-[#82D8FC]">가입 담보</p>
                        <h2 className="mt-1 text-lg font-semibold text-white">보장 3개가 증권에 고정됐습니다</h2>
                    </div>
                </div>
                <div className="space-y-3">
                    {policy.policyVersion?.coverages?.map((coverage) => {
                        const snapshot = snapshotByCoverageId.get(coverage.productCoverageId);
                        return (
                            <article key={coverage.policyCoverageId} className="rounded-xl border border-slate-800 bg-[#09111F] p-5">
                                <div className="flex items-start justify-between gap-4">
                                    <div>
                                        <p className="text-xs font-bold text-[#82D8FC]">{coverage.coverageCode}</p>
                                        <h3 className="mt-1 text-base font-semibold text-white">
                                            {snapshot?.coverageName || coverage.coverageCode}
                                        </h3>
                                    </div>
                                    <p className="shrink-0 text-base font-semibold text-white">
                                        {formatWon(coverage.insuredAmount)}
                                    </p>
                                </div>
                                <div className="mt-4 flex items-center gap-2 rounded-xl bg-slate-900 px-3 py-2 text-xs text-slate-400">
                                    <CalendarClock className="h-4 w-4 text-amber-200" />
                                    {conditionText(coverage, snapshot)}
                                </div>
                            </article>
                        );
                    })}
                </div>
            </section>

            <section className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/40 p-5">
                <div className="flex items-start gap-3">
                    <FileKey2 className="mt-0.5 h-5 w-5 shrink-0 text-[#82D8FC]" />
                    <div>
                        <p className="text-sm font-semibold text-white">계약 당시 약관을 기준으로 심사합니다</p>
                        <p className="mt-1 text-xs leading-5 text-slate-500">
                            이후 상품이 변경돼도 이 계약에는 증권 발행 당시의 상품 버전과 약관 해시가 유지됩니다.
                        </p>
                        <div className="mt-3 flex items-center gap-2 rounded-xl bg-[#020715] px-3 py-2 text-[11px] text-slate-500">
                            <Fingerprint className="h-3.5 w-3.5 shrink-0" />
                            <span className="truncate">{quoteSnapshot?.termsHash || '-'}</span>
                        </div>
                    </div>
                </div>
            </section>

            <div className="mt-8 space-y-3">
                <AppButton onClick={startClaim}>
                    <ClipboardPlus className="h-5 w-5" /> 보험금 청구 시뮬레이션
                </AppButton>
                <AppButton onClick={() => navigate('/home')}>
                    <Home className="h-5 w-5" /> 홈으로 돌아가기
                </AppButton>
                <AppButton onClick={restart} tone="subtle">
                    <RotateCcw className="h-4 w-4" /> 새 암보험 시나리오 시작
                </AppButton>
            </div>
        </div>
    );
};

export default CancerInsurancePolicyPage;
