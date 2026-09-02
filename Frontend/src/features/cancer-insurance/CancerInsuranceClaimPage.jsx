import React, { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import {
    AlertCircle,
    Check,
    ChevronLeft,
    ClipboardCheck,
    FileSearch,
    FlaskConical,
    Loader2,
    Scale,
    ShieldAlert,
    Stethoscope,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';
import {
    createCancerClaim,
    getCancerClaim,
    getCancerPolicy,
    recordCancerClaimEvidence,
    submitCancerClaim,
} from './api/cancerInsurance.api';
import { useCancerInsurance } from './context/CancerInsuranceContext';

const COVERAGE_CODES = {
    diagnosis: 'DEMO_GENERAL_CANCER_DIAGNOSIS',
    surgery: 'DEMO_CANCER_SURGERY',
};

const EVIDENCE_LABELS = {
    DEMO_DIAGNOSIS_CERTIFICATE: '합성 진단서',
    DEMO_PATHOLOGY_REPORT: '합성 병리검사 결과',
    DEMO_SURGERY_CERTIFICATE: '합성 수술확인서',
};

const SCENARIOS = [
    {
        id: 'approved',
        title: '수술비 정상 지급',
        description: '보장개시 후 사고와 필수 증빙 2종을 제출해 자동 승인과 가상 지급을 확인합니다.',
        expected: 'APPROVED',
        coverageCode: COVERAGE_CODES.surgery,
        evidenceMode: 'all',
        icon: ClipboardCheck,
        tone: 'blue',
    },
    {
        id: 'manual',
        title: '증빙 보완 필요',
        description: '필수 증빙 1종을 비워 자동 부지급 대신 수동심사로 보내는 규칙을 확인합니다.',
        expected: 'MANUAL_REVIEW',
        coverageCode: COVERAGE_CODES.surgery,
        evidenceMode: 'missing-one',
        icon: FileSearch,
        tone: 'yellow',
    },
    {
        id: 'denied',
        title: '보장개시 전 진단',
        description: '90일 보장개시 전 일반암 진단을 제출해 약관 조항이 있는 부지급 결정을 확인합니다.',
        expected: 'DENIED',
        coverageCode: COVERAGE_CODES.diagnosis,
        evidenceMode: 'none',
        icon: ShieldAlert,
        tone: 'rose',
    },
];

const TONE_STYLES = {
    blue: 'border-[#82D8FC] bg-[#82D8FC]/10 text-[#82D8FC]',
    yellow: 'border-amber-300/50 bg-amber-300/10 text-amber-200',
    rose: 'border-rose-300/50 bg-rose-300/10 text-rose-200',
};

const TERMINAL_STATUSES = new Set(['APPROVED', 'MANUAL_REVIEW', 'DENIED', 'PAID']);

const evidenceLabel = (type) => EVIDENCE_LABELS[type] || type;

const checksumFor = (index) => (index % 2 === 0 ? 'a' : 'b').repeat(64);

const CancerInsuranceClaimPage = () => {
    const navigate = useNavigate();
    const {
        flowIds,
        updateFlowIds,
        policy,
        setPolicy,
        claim,
        setClaim,
        getRequestKey,
    } = useCancerInsurance();
    const [selectedScenarioId, setSelectedScenarioId] = useState(
        flowIds.claimScenarioId || 'approved',
    );
    const [loading, setLoading] = useState(Boolean(flowIds.policyId));
    const [submitting, setSubmitting] = useState(false);
    const [stage, setStage] = useState('');
    const [error, setError] = useState('');

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
        let active = true;

        const restore = async () => {
            if (!flowIds.policyId) {
                setLoading(false);
                return;
            }
            try {
                const [restoredPolicy, restoredClaim] = await Promise.all([
                    policy ? Promise.resolve(policy) : getCancerPolicy(flowIds.policyId),
                    flowIds.claimId ? getCancerClaim(flowIds.claimId) : Promise.resolve(null),
                ]);
                if (!active) return;
                setPolicy(restoredPolicy);

                if (restoredClaim) {
                    setClaim(restoredClaim);
                    if (TERMINAL_STATUSES.has(restoredClaim.status)) {
                        navigate('/cancer-insurance/claim/result', { replace: true });
                    }
                }
            } catch (requestError) {
                if (active) {
                    setError(requestError.message || '보험금 청구 준비정보를 불러오지 못했습니다.');
                }
            } finally {
                if (active) setLoading(false);
            }
        };

        restore();
        return () => {
            active = false;
        };
    }, []);

    const scenarioSetups = useMemo(() => {
        const policyCoverages = policy?.policyVersion?.coverages || [];
        const snapshot = policy?.policyVersion?.snapshot;
        const quoteCoverages = snapshot?.quote?.coverages || [];
        const claimRules = snapshot?.claimRules || [];

        return SCENARIOS.map((scenario) => {
            const coverage = policyCoverages.find(
                (item) => item.coverageCode === scenario.coverageCode,
            );
            const claimRule = claimRules.find(
                (item) => item.productCoverageId === coverage?.productCoverageId,
            );
            const quoteCoverage = quoteCoverages.find(
                (item) => item.productCoverageId === coverage?.productCoverageId,
            );
            const requiredEvidence = claimRule?.requiredEvidence || [];
            const submittedEvidence = scenario.evidenceMode === 'all'
                ? requiredEvidence
                : scenario.evidenceMode === 'missing-one'
                    ? requiredEvidence.slice(0, Math.max(0, requiredEvidence.length - 1))
                    : [];
            const beforeCoverageStart = coverage?.coverageStartAt
                && Date.now() < new Date(coverage.coverageStartAt).getTime();
            const diagnosisCategory = scenario.id === 'denied' && !beforeCoverageStart
                ? 'DEMO_NOT_COVERED'
                : claimRule?.diagnosisCategories?.[0];

            return {
                ...scenario,
                coverage,
                claimRule,
                quoteCoverage,
                requiredEvidence,
                submittedEvidence,
                diagnosisCategory,
                denialFallback: scenario.id === 'denied' && !beforeCoverageStart,
            };
        });
    }, [policy]);

    const selectedScenario = scenarioSetups.find(
        (scenario) => scenario.id === selectedScenarioId,
    );

    const selectScenario = (scenarioId) => {
        if (submitting || claim?.claimId) return;
        setSelectedScenarioId(scenarioId);
        updateFlowIds({ claimScenarioId: scenarioId });
        setError('');
    };

    const submitScenario = async () => {
        if (!selectedScenario?.coverage || !selectedScenario.claimRule || submitting) return;

        setSubmitting(true);
        setError('');
        let workingClaim = claim;
        try {
            updateFlowIds({ claimScenarioId: selectedScenario.id });
            if (!workingClaim?.claimId) {
                setStage('청구 초안을 원장에 기록하고 있습니다');
                workingClaim = await createCancerClaim(
                    policy.policyId,
                    selectedScenario.coverage.policyCoverageId,
                    new Date().toISOString(),
                    selectedScenario.diagnosisCategory,
                );
                setClaim(workingClaim);
                updateFlowIds({ claimId: workingClaim.claimId });
            }

            setStage('합성 증빙의 참조와 검증 상태를 기록하고 있습니다');
            for (const [index, evidenceType] of selectedScenario.submittedEvidence.entries()) {
                workingClaim = await recordCancerClaimEvidence(workingClaim.claimId, {
                    evidenceType,
                    syntheticReference: `synthetic://capsure/claim/${workingClaim.claimId}/${evidenceType}`,
                    checksum: checksumFor(index),
                    metadata: {
                        simulation: true,
                        scenario: selectedScenario.id,
                    },
                    verified: true,
                });
                setClaim(workingClaim);
            }

            setStage('사고일 당시 계약과 약관 Snapshot으로 심사하고 있습니다');
            const assessed = await submitCancerClaim(
                workingClaim.claimId,
                getRequestKey(`claim-submit-${workingClaim.claimId}`),
            );
            setClaim(assessed);
            navigate('/cancer-insurance/claim/result');
        } catch (requestError) {
            const recoveryId = workingClaim?.claimId || flowIds.claimId;
            if (recoveryId) {
                try {
                    const restored = await getCancerClaim(recoveryId);
                    setClaim(restored);
                    if (TERMINAL_STATUSES.has(restored.status)) {
                        navigate('/cancer-insurance/claim/result');
                        return;
                    }
                } catch {
                    // 원래 오류를 유지해 사용자가 실패 원인을 확인할 수 있게 합니다.
                }
            }
            setError(requestError.message || '보험금 청구를 제출하지 못했습니다.');
        } finally {
            setSubmitting(false);
            setStage('');
        }
    };

    if (!flowIds.policyId) {
        return <Navigate to="/cancer-insurance/policy" replace />;
    }

    if (loading) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center gap-4 px-6 text-center">
                <Loader2 className="h-9 w-9 animate-spin text-[#82D8FC]" />
                <div>
                    <p className="font-bold text-white">계약의 청구 가능 담보를 확인하고 있습니다</p>
                    <p className="mt-1 text-sm text-slate-500">계약 당시 지급규칙 Snapshot을 복원합니다.</p>
                </div>
            </div>
        );
    }

    if (error && !policy) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center px-8 text-center">
                <AlertCircle className="h-10 w-10 text-rose-300" />
                <p className="mt-4 font-bold text-white">청구 준비정보를 불러오지 못했습니다</p>
                <p className="mt-2 text-sm leading-6 text-slate-400">{error}</p>
                <AppButton
                    onClick={() => navigate('/cancer-insurance/policy')}
                    fullWidth={false}
                    className="mt-6 px-6"
                >
                    보험증권으로 돌아가기
                </AppButton>
            </div>
        );
    }

    return (
        <div className="pb-44">
            <header className="flex items-center px-5 py-5">
                <button
                    type="button"
                    onClick={() => navigate('/cancer-insurance/policy')}
                    className="-ml-2 rounded-full p-2 text-white transition-colors hover:bg-slate-800"
                    aria-label="보험증권으로 돌아가기"
                >
                    <ChevronLeft className="h-6 w-6" />
                </button>
                <div className="ml-2">
                    <p className="text-xs font-bold text-[#82D8FC]">STEP 5 · 보험금 청구</p>
                    <h1 className="mt-0.5 text-xl font-black text-white">지급심사 상황을 선택해보세요</h1>
                </div>
            </header>

            <main className="space-y-6 px-6">
                <section className="rounded-2xl border border-slate-800 bg-[#09111F] p-5">
                    <div className="flex items-start gap-3">
                        <Stethoscope className="mt-0.5 h-5 w-5 shrink-0 text-[#82D8FC]" />
                        <div>
                            <p className="text-sm font-black text-white">실제 의료정보를 입력하지 않습니다</p>
                            <p className="mt-1 text-xs leading-5 text-slate-500">
                                합성 사고일·진단 분류·증빙 metadata만 사용하며 의료파일은 저장하거나 전송하지 않습니다.
                            </p>
                        </div>
                    </div>
                </section>

                <section>
                    <div className="mb-4 flex items-center gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#F2BEF7]/10 text-[#F2BEF7]">
                            <Scale className="h-5 w-5" />
                        </span>
                        <div>
                            <p className="text-xs font-bold text-[#F2BEF7]">규칙 엔진 시나리오</p>
                            <h2 className="text-lg font-black text-white">지급·수동심사·부지급</h2>
                        </div>
                    </div>
                    <div className="space-y-3">
                        {scenarioSetups.map((scenario) => {
                            const ScenarioIcon = scenario.icon;
                            const selected = scenario.id === selectedScenarioId;
                            return (
                                <button
                                    key={scenario.id}
                                    type="button"
                                    onClick={() => selectScenario(scenario.id)}
                                    className={`flex w-full items-start gap-4 rounded-2xl border p-4 text-left transition-all ${selected ? TONE_STYLES[scenario.tone] : 'border-slate-800 bg-[#09111F] text-slate-500'}`}
                                >
                                    <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${selected ? 'bg-white/5' : 'bg-slate-800'}`}>
                                        <ScenarioIcon className="h-5 w-5" />
                                    </span>
                                    <span className="min-w-0 flex-1">
                                        <span className="flex items-center justify-between gap-3">
                                            <span className="text-sm font-black text-white">{scenario.title}</span>
                                            <span className="text-[10px] font-black">{scenario.expected}</span>
                                        </span>
                                        <span className="mt-1 block text-xs leading-5 text-slate-500">
                                            {scenario.denialFallback
                                                ? '보장개시가 지난 계약이므로 담보 범위 밖 분류로 동일한 부지급 통제를 검증합니다.'
                                                : scenario.description}
                                        </span>
                                    </span>
                                    <span className={`mt-1 flex h-5 w-5 items-center justify-center rounded-full border ${selected ? 'border-current bg-current text-[#020715]' : 'border-slate-700 text-transparent'}`}>
                                        <Check className="h-3 w-3" strokeWidth={3} />
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                </section>

                {selectedScenario && (
                    <section className="rounded-[28px] border border-[#82D8FC]/20 bg-gradient-to-br from-[#10253A] to-[#07101C] p-6">
                        <div className="flex items-start justify-between gap-4">
                            <div>
                                <p className="text-xs font-bold text-[#82D8FC]">청구 준비도</p>
                                <h2 className="mt-1 text-lg font-black text-white">
                                    {selectedScenario.quoteCoverage?.coverageName || selectedScenario.coverage?.coverageCode}
                                </h2>
                            </div>
                            <span className="rounded-full bg-[#82D8FC]/10 px-3 py-1 text-xs font-black text-[#82D8FC]">
                                {selectedScenario.submittedEvidence.length}/{selectedScenario.requiredEvidence.length}
                            </span>
                        </div>
                        <div className="mt-5 space-y-2">
                            {selectedScenario.requiredEvidence.map((evidenceType) => {
                                const included = selectedScenario.submittedEvidence.includes(evidenceType);
                                return (
                                    <div key={evidenceType} className="flex items-center justify-between rounded-xl bg-white/5 px-4 py-3">
                                        <span className="text-sm font-bold text-slate-300">{evidenceLabel(evidenceType)}</span>
                                        <span className={`text-xs font-black ${included ? 'text-[#82D8FC]' : 'text-amber-200'}`}>
                                            {included ? '검증됨' : '미제출'}
                                        </span>
                                    </div>
                                );
                            })}
                        </div>
                        <div className="mt-4 flex items-start gap-2 rounded-xl bg-[#020715]/70 p-3 text-xs leading-5 text-slate-500">
                            <FlaskConical className="mt-0.5 h-4 w-4 shrink-0 text-[#F2BEF7]" />
                            사고일은 제출 시각으로 생성하고, 계약 발행 당시 담보·약관·규칙 버전으로 심사합니다.
                        </div>
                    </section>
                )}

                {submitting && stage && (
                    <div className="flex items-center gap-3 rounded-2xl border border-[#82D8FC]/20 bg-[#82D8FC]/10 p-4 text-sm text-[#82D8FC]">
                        <Loader2 className="h-5 w-5 shrink-0 animate-spin" />
                        {stage}
                    </div>
                )}

                {error && (
                    <div className="flex items-start gap-2 rounded-2xl border border-rose-400/20 bg-rose-400/10 p-4 text-sm text-rose-200">
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                        {error}
                    </div>
                )}
            </main>

            <div className="fixed app-fixed-cta left-1/2 z-40 w-full max-w-[560px] -translate-x-1/2 bg-gradient-to-t from-[#020715] via-[#020715] to-transparent px-6 pb-6 pt-8">
                <AppButton onClick={submitScenario} disabled={submitting || !selectedScenario?.coverage}>
                    {submitting
                        ? <Loader2 className="h-5 w-5 animate-spin" />
                        : <ClipboardCheck className="h-5 w-5" />}
                    {submitting ? '청구 원장을 처리하고 있어요' : '합성 보험금 청구 제출하기'}
                </AppButton>
            </div>
        </div>
    );
};

export default CancerInsuranceClaimPage;
