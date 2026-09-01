import React, { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import {
    AlertCircle,
    AlertTriangle,
    Check,
    CheckCircle2,
    ChevronLeft,
    ClipboardCheck,
    FileSignature,
    HeartPulse,
    Loader2,
    LockKeyhole,
    RotateCcw,
    ShieldAlert,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';
import {
    createCancerApplication,
    getCancerApplication,
    getCancerQuote,
    recordCancerConsent,
    replaceCancerDisclosures,
    submitCancerApplication,
} from './api/cancerInsurance.api';
import { useCancerInsurance } from './context/CancerInsuranceContext';

const QUESTIONS = [
    {
        key: 'diagnosedCancer',
        title: '과거 암 진단을 받은 사실이 있나요?',
        description: '예를 선택하면 현재 가상 인수 규칙에서는 가입 거절로 분류됩니다.',
    },
    {
        key: 'underCancerExamination',
        title: '현재 암 관련 검사 결과를 기다리고 있나요?',
        description: '검사 진행 중인 경우 담당자 수동심사가 필요합니다.',
    },
    {
        key: 'recentHospitalization',
        title: '최근 입원 또는 수술한 사실이 있나요?',
        description: '최근 치료 이력이 있으면 추가 확인 대상으로 분류됩니다.',
    },
];

const CONSENTS = [
    {
        key: 'PRODUCT_TERMS',
        title: '상품 약관에 동의합니다',
        description: '견적에 고정된 약관 버전과 해시를 기준으로 기록합니다.',
    },
    {
        key: 'PRODUCT_EXPLANATION',
        title: '상품의 중요 설명을 확인했습니다',
        description: '보장 범위, 면책기간과 감액기간 설명을 확인합니다.',
    },
];

const RESULT_COPY = {
    APPROVED: {
        eyebrow: '자동 인수심사 승인',
        title: '가입 심사를 통과했습니다',
        description: '초회 보험료 결제가 완료되면 보험증권이 발행됩니다.',
        icon: CheckCircle2,
        tone: 'blue',
    },
    MANUAL_REVIEW: {
        eyebrow: '수동심사 필요',
        title: '추가 확인이 필요한 청약입니다',
        description: '실제 보험사에서는 담당 심사자가 고지 내용과 추가 서류를 확인합니다.',
        icon: AlertTriangle,
        tone: 'yellow',
    },
    DECLINED: {
        eyebrow: '자동 인수심사 거절',
        title: '현재 조건으로는 가입할 수 없습니다',
        description: '가상 인수 규칙에 따라 기존 암 진단 고지가 확인됐습니다.',
        icon: ShieldAlert,
        tone: 'rose',
    },
};

const initialAnswers = QUESTIONS.reduce((answers, question) => ({
    ...answers,
    [question.key]: null,
}), {});

const CancerInsuranceApplicationPage = () => {
    const navigate = useNavigate();
    const {
        flowIds,
        updateFlowIds,
        quote,
        setQuote,
        application,
        setApplication,
        getRequestKey,
        resetFlow,
    } = useCancerInsurance();
    const [answers, setAnswers] = useState(initialAnswers);
    const [consents, setConsents] = useState({});
    const [loading, setLoading] = useState(Boolean(flowIds.quoteId && !quote));
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
        let active = true;

        const restoreFlow = async () => {
            if (!flowIds.quoteId) {
                setLoading(false);
                return;
            }
            try {
                const [restoredQuote, restoredApplication] = await Promise.all([
                    quote ? Promise.resolve(quote) : getCancerQuote(flowIds.quoteId),
                    flowIds.applicationId
                        ? getCancerApplication(flowIds.applicationId)
                        : Promise.resolve(null),
                ]);
                if (!active) return;
                setQuote(restoredQuote);
                if (restoredApplication) {
                    setApplication(restoredApplication);
                    if (restoredApplication.disclosureAnswers) {
                        setAnswers(restoredApplication.disclosureAnswers);
                    }
                    setConsents(
                        restoredApplication.consents.reduce((result, consent) => ({
                            ...result,
                            [consent.consentType]: consent.agreed,
                        }), {}),
                    );
                }
            } catch (requestError) {
                if (active) {
                    setError(requestError.message || '청약 정보를 불러오지 못했습니다.');
                }
            } finally {
                if (active) setLoading(false);
            }
        };

        restoreFlow();
        return () => {
            active = false;
        };
    }, []);

    const allAnswered = useMemo(
        () => QUESTIONS.every((question) => typeof answers[question.key] === 'boolean'),
        [answers],
    );
    const allConsented = CONSENTS.every((consent) => consents[consent.key]);
    const finalStatus = application?.underwritingDecision?.decision || (
        RESULT_COPY[application?.status] ? application.status : null
    );

    const answerQuestion = (key, value) => {
        setAnswers((previous) => ({ ...previous, [key]: value }));
    };

    const toggleConsent = (key) => {
        setConsents((previous) => ({ ...previous, [key]: !previous[key] }));
    };

    const handleSubmit = async () => {
        if (!flowIds.quoteId || !allAnswered || !allConsented || submitting) {
            return;
        }
        setSubmitting(true);
        setError('');
        try {
            const draft = application?.applicationId
                ? application
                : await createCancerApplication(flowIds.quoteId);
            setApplication(draft);
            updateFlowIds({ applicationId: draft.applicationId });

            await replaceCancerDisclosures(draft.applicationId, answers);
            for (const consent of CONSENTS) {
                await recordCancerConsent(
                    draft.applicationId,
                    consent.key,
                    quote.termsDocumentHash,
                );
            }
            const submitted = await submitCancerApplication(
                draft.applicationId,
                getRequestKey('application-submit'),
            );
            setApplication(submitted);
        } catch (requestError) {
            setError(requestError.message || '청약을 제출하지 못했습니다.');
        } finally {
            setSubmitting(false);
        }
    };

    const restart = () => {
        resetFlow();
        navigate('/cancer-insurance', { replace: true });
    };

    if (!flowIds.quoteId && !quote) {
        return <Navigate to="/cancer-insurance" replace />;
    }

    if (loading) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center gap-4 px-6">
                <Loader2 className="h-9 w-9 animate-spin text-[#82D8FC]" />
                <p className="font-bold text-white">견적 Snapshot을 확인하고 있습니다</p>
            </div>
        );
    }

    if (finalStatus) {
        const result = RESULT_COPY[finalStatus];
        const ResultIcon = result.icon;
        const toneClasses = {
            blue: 'border-[#82D8FC]/30 bg-[#82D8FC]/10 text-[#82D8FC]',
            yellow: 'border-amber-300/30 bg-amber-300/10 text-amber-200',
            rose: 'border-rose-300/30 bg-rose-300/10 text-rose-200',
        };
        return (
            <div className="px-6 pb-36 pt-8">
                <section className={`rounded-[30px] border p-7 ${toneClasses[result.tone]}`}>
                    <ResultIcon className="h-12 w-12" />
                    <p className="mt-6 text-xs font-black">{result.eyebrow}</p>
                    <h1 className="mt-2 text-2xl font-black tracking-[-0.04em] text-white">{result.title}</h1>
                    <p className="mt-3 text-sm leading-6 text-slate-300">{result.description}</p>
                </section>

                <section className="mt-5 rounded-2xl border border-slate-800 bg-[#09111F] p-5">
                    <div className="flex items-center justify-between gap-3">
                        <span className="text-sm text-slate-500">청약번호</span>
                        <span className="text-sm font-bold text-white">{application.applicationNo}</span>
                    </div>
                    <div className="mt-3 flex items-center justify-between gap-3">
                        <span className="text-sm text-slate-500">심사 규칙</span>
                        <span className="text-sm font-bold text-white">{application.underwritingDecision?.ruleVersion}</span>
                    </div>
                    <div className="mt-3 flex items-center justify-between gap-3">
                        <span className="text-sm text-slate-500">판정 사유</span>
                        <span className="text-right text-xs font-bold text-[#82D8FC]">
                            {application.underwritingDecision?.reasonCodes?.join(', ')}
                        </span>
                    </div>
                </section>

                <div className="mt-8 space-y-3">
                    {finalStatus === 'APPROVED' && (
                        <AppButton onClick={() => navigate('/cancer-insurance/payment')}>
                            초회 보험료 결제하기
                        </AppButton>
                    )}
                    <AppButton onClick={restart} tone="subtle">
                        <RotateCcw className="h-4 w-4" /> 다른 시나리오로 다시 시작
                    </AppButton>
                </div>
            </div>
        );
    }

    return (
        <div className="pb-44">
            <header className="flex items-center px-5 py-5">
                <button
                    type="button"
                    onClick={() => navigate('/cancer-insurance')}
                    className="-ml-2 rounded-full p-2 text-white transition-colors hover:bg-slate-800"
                    aria-label="상품 화면으로 돌아가기"
                >
                    <ChevronLeft className="h-6 w-6" />
                </button>
                <div className="ml-2">
                    <p className="text-xs font-bold text-[#82D8FC]">STEP 2 · 청약과 인수심사</p>
                    <h1 className="mt-0.5 text-xl font-black text-white">가상 고지사항에 답해주세요</h1>
                </div>
            </header>

            <main className="space-y-6 px-6">
                <section className="rounded-2xl border border-amber-300/20 bg-amber-300/10 p-4">
                    <div className="flex items-start gap-3">
                        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-200" />
                        <div>
                            <p className="text-sm font-black text-amber-100">실제 병력은 입력하지 마세요</p>
                            <p className="mt-1 text-xs leading-5 text-amber-100/60">
                                인수심사 상태 전이를 확인하는 교육용 선택지입니다. 개인정보나 의료정보를 저장하지 않습니다.
                            </p>
                        </div>
                    </div>
                </section>

                <section>
                    <div className="mb-4 flex items-center gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#82D8FC]/10 text-[#82D8FC]">
                            <HeartPulse className="h-5 w-5" />
                        </span>
                        <div>
                            <p className="text-xs font-bold text-[#82D8FC]">계약 전 알릴 의무</p>
                            <h2 className="text-lg font-black text-white">고지사항 3개</h2>
                        </div>
                    </div>
                    <div className="space-y-3">
                        {QUESTIONS.map((question, index) => (
                            <div key={question.key} className="rounded-2xl border border-slate-800 bg-[#09111F] p-5">
                                <div className="flex items-start gap-3">
                                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-slate-800 text-[11px] font-black text-slate-300">
                                        {index + 1}
                                    </span>
                                    <div>
                                        <h3 className="text-sm font-black leading-6 text-white">{question.title}</h3>
                                        <p className="mt-1 text-xs leading-5 text-slate-500">{question.description}</p>
                                    </div>
                                </div>
                                <div className="mt-4 grid grid-cols-2 gap-2">
                                    {[false, true].map((value) => {
                                        const selected = answers[question.key] === value;
                                        return (
                                            <button
                                                key={String(value)}
                                                type="button"
                                                onClick={() => answerQuestion(question.key, value)}
                                                className={`rounded-xl border py-3 text-sm font-black transition-all ${selected ? 'border-[#82D8FC] bg-[#82D8FC] text-[#020715]' : 'border-slate-700 bg-slate-900 text-slate-400'}`}
                                            >
                                                {value ? '예' : '아니오'}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        ))}
                    </div>
                </section>

                <section>
                    <div className="mb-4 flex items-center gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#F2BEF7]/10 text-[#F2BEF7]">
                            <FileSignature className="h-5 w-5" />
                        </span>
                        <div>
                            <p className="text-xs font-bold text-[#F2BEF7]">동의 증거</p>
                            <h2 className="text-lg font-black text-white">필수 문서를 확인해주세요</h2>
                        </div>
                    </div>
                    <div className="overflow-hidden rounded-2xl border border-slate-800 bg-[#09111F]">
                        {CONSENTS.map((consent, index) => (
                            <button
                                key={consent.key}
                                type="button"
                                onClick={() => toggleConsent(consent.key)}
                                className={`flex w-full items-start gap-3 p-5 text-left ${index > 0 ? 'border-t border-slate-800' : ''}`}
                            >
                                <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-md border ${consents[consent.key] ? 'border-[#82D8FC] bg-[#82D8FC] text-[#020715]' : 'border-slate-600 text-transparent'}`}>
                                    <Check className="h-3.5 w-3.5" strokeWidth={3} />
                                </span>
                                <span>
                                    <span className="block text-sm font-black text-white">{consent.title}</span>
                                    <span className="mt-1 block text-xs leading-5 text-slate-500">{consent.description}</span>
                                </span>
                            </button>
                        ))}
                    </div>
                    <div className="mt-3 flex items-center gap-2 px-1 text-[11px] text-slate-600">
                        <LockKeyhole className="h-3.5 w-3.5" />
                        약관 해시 {quote?.termsDocumentHash?.slice(0, 12)}… 기준으로 기록
                    </div>
                </section>

                <section className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
                    <div className="flex items-start gap-3">
                        <ClipboardCheck className="mt-0.5 h-4 w-4 shrink-0 text-[#82D8FC]" />
                        <p className="text-xs leading-5 text-slate-400">
                            제출하면 입력값을 규칙 버전과 함께 해시로 남기고, 동일 제출 요청은 한 번만 심사합니다.
                        </p>
                    </div>
                </section>

                {error && (
                    <div className="flex items-start gap-2 rounded-2xl border border-rose-400/20 bg-rose-400/10 p-4 text-sm text-rose-200">
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                        {error}
                    </div>
                )}
            </main>

            <div className="fixed app-fixed-cta left-1/2 z-40 w-full max-w-[560px] -translate-x-1/2 bg-gradient-to-t from-[#020715] via-[#020715] to-transparent px-6 pb-6 pt-8">
                <AppButton
                    onClick={handleSubmit}
                    disabled={!allAnswered || !allConsented || submitting}
                >
                    {submitting ? <Loader2 className="h-5 w-5 animate-spin" /> : <ClipboardCheck className="h-5 w-5" />}
                    {submitting ? '청약을 심사하고 있어요' : '고지 제출하고 자동 심사받기'}
                </AppButton>
            </div>
        </div>
    );
};

export default CancerInsuranceApplicationPage;
