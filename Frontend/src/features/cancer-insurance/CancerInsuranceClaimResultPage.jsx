import React, { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import {
    AlertCircle,
    BadgeCheck,
    Check,
    CircleDollarSign,
    Clock3,
    FileCheck2,
    Fingerprint,
    Loader2,
    RefreshCw,
    RotateCcw,
    Scale,
    ShieldCheck,
    XCircle,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';
import {
    getCancerClaim,
    getTermsClause,
    payCancerClaim,
} from './api/cancerInsurance.api';
import { useCancerInsurance } from './context/CancerInsuranceContext';

const currency = new Intl.NumberFormat('ko-KR');

const formatWon = (value) => `${currency.format(Number(value || 0))}원`;

const formatDateTime = (value) => {
    if (!value) return '-';
    return new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(new Date(value));
};

const STATUS_DETAILS = {
    APPROVED: {
        eyebrow: '지급심사 · APPROVED',
        title: '약관상 지급 대상으로 승인됐습니다',
        description: '결정 금액과 약관 근거가 고정됐으며 가상 지급 원장을 생성할 수 있습니다.',
        icon: BadgeCheck,
        classes: 'border-[#82D8FC]/30 bg-[#82D8FC]/10 text-[#82D8FC]',
    },
    MANUAL_REVIEW: {
        eyebrow: '지급심사 · MANUAL_REVIEW',
        title: '자동 부지급하지 않고 수동심사로 보냈습니다',
        description: '증빙이 부족한 상태는 부지급 확정이 아닙니다. 담당자 보완 확인이 필요한 상태입니다.',
        icon: Clock3,
        classes: 'border-amber-300/30 bg-amber-300/10 text-amber-200',
    },
    DENIED: {
        eyebrow: '지급심사 · DENIED',
        title: '약관상 부지급 사유가 확인됐습니다',
        description: '규칙 엔진이 사고일 당시 약관과 담보 조건을 기준으로 부지급 근거를 기록했습니다.',
        icon: XCircle,
        classes: 'border-rose-300/30 bg-rose-300/10 text-rose-200',
    },
    PAID: {
        eyebrow: '지급 원장 · PAID',
        title: '가상 보험금 지급이 완료됐습니다',
        description: '실제 송금 없이 결정 금액·지급 번호·지급 시각을 내부 원장에 한 번만 기록했습니다.',
        icon: CircleDollarSign,
        classes: 'border-emerald-300/30 bg-emerald-300/10 text-emerald-200',
    },
};

const REASON_LABELS = {
    ELIGIBLE_FULL_BENEFIT: '감액기간이 지나 가입금액 전액 지급 대상입니다.',
    ELIGIBLE_REDUCED_BENEFIT: '감액기간 안의 사고로 가입금액 일부 지급 대상입니다.',
    COVERAGE_NOT_STARTED: '사고일이 해당 담보의 보장개시일보다 빠릅니다.',
    COVERAGE_ENDED: '사고일 당시 해당 담보의 보장기간이 종료된 상태입니다.',
    DIAGNOSIS_CATEGORY_NOT_COVERED: '입력된 진단 분류가 해당 담보의 보장 범위에 포함되지 않습니다.',
    FIRST_DIAGNOSIS_BENEFIT_ALREADY_PAID: '최초 1회 진단비가 이미 지급된 담보입니다.',
    REQUIRED_EVIDENCE_MISSING_OR_UNVERIFIED: '필수 증빙이 누락됐거나 검증되지 않아 담당자 확인이 필요합니다.',
};

const EVIDENCE_LABELS = {
    DEMO_DIAGNOSIS_CERTIFICATE: '합성 진단서',
    DEMO_PATHOLOGY_REPORT: '합성 병리검사 결과',
    DEMO_SURGERY_CERTIFICATE: '합성 수술확인서',
};

const CancerInsuranceClaimResultPage = () => {
    const navigate = useNavigate();
    const {
        flowIds,
        claim,
        setClaim,
        getRequestKey,
        resetClaim,
    } = useCancerInsurance();
    const [clause, setClause] = useState(null);
    const [loading, setLoading] = useState(Boolean(flowIds.claimId && !claim));
    const [paying, setPaying] = useState(false);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState('');

    const restore = async () => {
        if (!flowIds.claimId) return null;
        const restored = await getCancerClaim(flowIds.claimId);
        setClaim(restored);
        if (restored.decision?.termsClauseId) {
            try {
                setClause(await getTermsClause(restored.decision.termsClauseId));
            } catch {
                setClause(null);
            }
        }
        return restored;
    };

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
        let active = true;

        const load = async () => {
            if (!flowIds.claimId) {
                setLoading(false);
                return;
            }
            try {
                const restored = claim || await getCancerClaim(flowIds.claimId);
                if (!active) return;
                setClaim(restored);
                if (restored.decision?.termsClauseId) {
                    try {
                        const restoredClause = await getTermsClause(restored.decision.termsClauseId);
                        if (active) setClause(restoredClause);
                    } catch {
                        if (active) setClause(null);
                    }
                }
            } catch (requestError) {
                if (active) {
                    setError(requestError.message || '지급심사 결과를 불러오지 못했습니다.');
                }
            } finally {
                if (active) setLoading(false);
            }
        };

        load();
        return () => {
            active = false;
        };
    }, []);

    const refreshClaim = async () => {
        if (!flowIds.claimId || refreshing) return;
        setRefreshing(true);
        setError('');
        try {
            await restore();
        } catch (requestError) {
            setError(requestError.message || '청구 상태를 다시 확인하지 못했습니다.');
        } finally {
            setRefreshing(false);
        }
    };

    const executePayment = async () => {
        if (!claim?.claimId || claim.status !== 'APPROVED' || paying) return;
        setPaying(true);
        setError('');
        try {
            const paid = await payCancerClaim(
                claim.claimId,
                getRequestKey(`claim-payment-${claim.claimId}`),
            );
            setClaim(paid);
        } catch (requestError) {
            try {
                const restored = await restore();
                if (restored?.status === 'PAID') return;
            } catch {
                // 원래 지급 오류를 유지합니다.
            }
            setError(requestError.message || '가상 보험금 지급을 실행하지 못했습니다.');
        } finally {
            setPaying(false);
        }
    };

    const startAnotherClaim = () => {
        resetClaim();
        navigate('/cancer-insurance/claim', { replace: true });
    };

    if (!flowIds.claimId) {
        return <Navigate to="/cancer-insurance/claim" replace />;
    }

    if (loading) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center gap-4 px-6 text-center">
                <Loader2 className="h-9 w-9 animate-spin text-[#82D8FC]" />
                <div>
                    <p className="font-bold text-white">지급심사 결정 원장을 불러오고 있습니다</p>
                    <p className="mt-1 text-sm text-slate-500">결정·약관 조항·입력 해시를 함께 확인합니다.</p>
                </div>
            </div>
        );
    }

    if (error && !claim) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center px-8 text-center">
                <AlertCircle className="h-10 w-10 text-rose-300" />
                <p className="mt-4 font-bold text-white">지급심사 결과를 불러오지 못했습니다</p>
                <p className="mt-2 text-sm leading-6 text-slate-400">{error}</p>
                <AppButton onClick={refreshClaim} fullWidth={false} className="mt-6 px-6">
                    다시 확인하기
                </AppButton>
            </div>
        );
    }

    if (claim && !STATUS_DETAILS[claim.status]) {
        return <Navigate to="/cancer-insurance/claim" replace />;
    }

    const statusDetail = STATUS_DETAILS[claim.status];
    const StatusIcon = statusDetail.icon;
    const decision = claim?.decision;
    const payment = claim?.payment;

    return (
        <div className="px-6 pb-36 pt-8">
            <section className={`rounded-[30px] border p-7 ${statusDetail.classes}`}>
                <StatusIcon className="h-12 w-12" />
                <p className="mt-6 text-xs font-black">{statusDetail.eyebrow}</p>
                <h1 className="mt-2 text-2xl font-black tracking-[-0.04em] text-white">
                    {statusDetail.title}
                </h1>
                <p className="mt-3 text-sm leading-6 text-slate-300">{statusDetail.description}</p>
            </section>

            <section className="mt-5 rounded-2xl border border-slate-800 bg-[#09111F] p-5">
                <div className="flex items-center justify-between gap-3">
                    <span className="text-sm text-slate-500">청구번호</span>
                    <span className="text-sm font-bold text-white">{claim?.claimNo}</span>
                </div>
                <div className="mt-3 flex items-center justify-between gap-3">
                    <span className="text-sm text-slate-500">사고일</span>
                    <span className="text-sm font-bold text-white">{formatDateTime(claim?.incidentAt)}</span>
                </div>
                <div className="mt-3 flex items-center justify-between gap-3">
                    <span className="text-sm text-slate-500">심사 결정 금액</span>
                    <span className="text-base font-black text-[#82D8FC]">
                        {decision?.benefitAmount == null ? '-' : formatWon(decision.benefitAmount)}
                    </span>
                </div>
                {payment && (
                    <>
                        <div className="mt-3 flex items-center justify-between gap-3">
                            <span className="text-sm text-slate-500">지급번호</span>
                            <span className="text-sm font-bold text-emerald-200">{payment.payoutOrderNo}</span>
                        </div>
                        <div className="mt-3 flex items-center justify-between gap-3">
                            <span className="text-sm text-slate-500">지급시각</span>
                            <span className="text-sm font-bold text-white">{formatDateTime(payment.paidAt)}</span>
                        </div>
                    </>
                )}
            </section>

            <section className="mt-6">
                <div className="mb-4 flex items-center gap-3">
                    <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#F2BEF7]/10 text-[#F2BEF7]">
                        <Scale className="h-5 w-5" />
                    </span>
                    <div>
                        <p className="text-xs font-bold text-[#F2BEF7]">결정 이유</p>
                        <h2 className="text-lg font-black text-white">코드와 약관 근거</h2>
                    </div>
                </div>
                <div className="space-y-3">
                    {decision?.reasonCodes?.map((reasonCode) => (
                        <div key={reasonCode} className="rounded-2xl border border-slate-800 bg-[#09111F] p-4">
                            <p className="text-xs font-black text-[#82D8FC]">{reasonCode}</p>
                            <p className="mt-2 text-sm leading-6 text-slate-300">
                                {REASON_LABELS[reasonCode] || '지급심사 규칙에 기록된 결정 사유입니다.'}
                            </p>
                        </div>
                    ))}
                </div>
            </section>

            {clause && (
                <section className="mt-6 rounded-2xl border border-[#82D8FC]/20 bg-[#82D8FC]/5 p-5">
                    <div className="flex items-start gap-3">
                        <FileCheck2 className="mt-0.5 h-5 w-5 shrink-0 text-[#82D8FC]" />
                        <div>
                            <p className="text-xs font-black text-[#82D8FC]">{clause.clauseCode} · 약관 v{clause.documentVersion}</p>
                            <h2 className="mt-1 text-base font-black text-white">{clause.title}</h2>
                            <p className="mt-3 whitespace-pre-line text-xs leading-5 text-slate-400">{clause.content}</p>
                        </div>
                    </div>
                </section>
            )}

            <section className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/40 p-5">
                <div className="flex items-start gap-3">
                    <Fingerprint className="mt-0.5 h-5 w-5 shrink-0 text-[#82D8FC]" />
                    <div className="min-w-0 flex-1">
                        <p className="text-sm font-black text-white">같은 입력이면 같은 결정을 재현합니다</p>
                        <p className="mt-1 text-xs leading-5 text-slate-500">
                            AI가 결정하지 않고 계약 Snapshot과 규칙 엔진이 결과를 만들며, 입력 해시로 판단 근거를 추적합니다.
                        </p>
                        <dl className="mt-4 space-y-2 text-xs">
                            <div className="flex items-center justify-between gap-3">
                                <dt className="text-slate-600">규칙 버전</dt>
                                <dd className="font-bold text-slate-300">{decision?.ruleVersion || '-'}</dd>
                            </div>
                            <div className="flex items-center justify-between gap-3">
                                <dt className="text-slate-600">결정 주체</dt>
                                <dd className="font-bold text-slate-300">{decision?.actorType || '-'}</dd>
                            </div>
                        </dl>
                        <div className="mt-3 truncate rounded-xl bg-[#020715] px-3 py-2 text-[11px] text-slate-600">
                            {decision?.inputHash || '-'}
                        </div>
                    </div>
                </div>
            </section>

            <section className="mt-6 rounded-2xl border border-slate-800 bg-[#09111F] p-5">
                <div className="mb-4 flex items-center justify-between gap-3">
                    <p className="text-sm font-black text-white">합성 증빙 원장</p>
                    <span className="text-xs font-black text-[#82D8FC]">{claim?.evidence?.length || 0}건</span>
                </div>
                <div className="space-y-2">
                    {claim?.evidence?.length > 0 ? claim.evidence.map((item) => (
                        <div key={item.claimEvidenceId} className="flex items-center justify-between rounded-xl bg-slate-900 px-3 py-3">
                            <span className="text-xs font-bold text-slate-300">
                                {EVIDENCE_LABELS[item.evidenceType] || item.evidenceType}
                            </span>
                            <span className="flex items-center gap-1 text-xs font-black text-[#82D8FC]">
                                <Check className="h-3.5 w-3.5" /> 검증됨
                            </span>
                        </div>
                    )) : (
                        <p className="rounded-xl bg-slate-900 px-3 py-4 text-center text-xs text-slate-600">
                            제출된 합성 증빙이 없습니다.
                        </p>
                    )}
                </div>
            </section>

            {error && (
                <div className="mt-4 flex items-start gap-2 rounded-2xl border border-rose-400/20 bg-rose-400/10 p-4 text-sm text-rose-200">
                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                    {error}
                </div>
            )}

            <div className="mt-8 space-y-3">
                {claim?.status === 'APPROVED' && (
                    <AppButton onClick={executePayment} disabled={paying}>
                        {paying
                            ? <Loader2 className="h-5 w-5 animate-spin" />
                            : <CircleDollarSign className="h-5 w-5" />}
                        {paying ? '지급 원장을 처리하고 있어요' : `${formatWon(decision?.benefitAmount)} 가상 지급하기`}
                    </AppButton>
                )}
                <AppButton onClick={refreshClaim} disabled={refreshing} tone="subtle">
                    {refreshing
                        ? <Loader2 className="h-4 w-4 animate-spin" />
                        : <RefreshCw className="h-4 w-4" />}
                    청구 상태 다시 확인
                </AppButton>
                <AppButton onClick={startAnotherClaim} tone="subtle">
                    <RotateCcw className="h-4 w-4" /> 다른 청구 시나리오 실행
                </AppButton>
                <AppButton onClick={() => navigate('/cancer-insurance/policy')} tone="subtle">
                    <ShieldCheck className="h-4 w-4" /> 보험증권으로 돌아가기
                </AppButton>
            </div>
        </div>
    );
};

export default CancerInsuranceClaimResultPage;
