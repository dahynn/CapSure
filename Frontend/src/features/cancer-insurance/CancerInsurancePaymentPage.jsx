import React, { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import {
    AlertCircle,
    AlertTriangle,
    Check,
    CheckCircle2,
    ChevronLeft,
    Clock3,
    CreditCard,
    FileCheck2,
    Loader2,
    RefreshCw,
    RotateCcw,
    ServerCog,
    ShieldCheck,
    XCircle,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';
import {
    confirmInitialPremiumPayment,
    createInitialPremiumOrder,
    getInitialPremiumPayment,
} from './api/cancerInsurance.api';
import { useCancerInsurance } from './context/CancerInsuranceContext';

const currency = new Intl.NumberFormat('ko-KR');

const formatWon = (value) => `${currency.format(Number(value || 0))}원`;

const formatDateTime = (value) => {
    if (!value) return '-';
    return new Intl.DateTimeFormat('ko-KR', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(new Date(value));
};

const PAYMENT_SCENARIOS = [
    {
        id: 'paid',
        title: '정상 승인',
        actionLabel: '정상 승인으로 결제하기',
        description: 'PG 승인과 계약 활성화가 한 번의 흐름으로 완료됩니다.',
        icon: CheckCircle2,
        tone: 'blue',
    },
    {
        id: 'failed',
        title: '승인 거절',
        actionLabel: '승인 거절로 결제하기',
        description: '결제는 실패하고 대기 중인 계약은 활성화되지 않습니다.',
        icon: XCircle,
        tone: 'rose',
    },
    {
        id: 'timeout',
        title: '응답 지연',
        actionLabel: '응답 지연으로 결제하기',
        description: '성공·실패를 단정하지 않고 UNKNOWN으로 대사를 기다립니다.',
        icon: Clock3,
        tone: 'yellow',
    },
];

const STATUS_COPY = {
    PAID: {
        title: '초회 보험료 결제가 완료됐습니다',
        description: '결제 원장과 계약 활성화가 완료되어 보험증권을 확인할 수 있습니다.',
        icon: CheckCircle2,
        classes: 'border-[#82D8FC]/30 bg-[#82D8FC]/10 text-[#82D8FC]',
    },
    FAILED: {
        title: '결제가 승인되지 않았습니다',
        description: '실패한 주문으로는 계약이 활성화되지 않습니다. 새 견적부터 다시 진행해주세요.',
        icon: XCircle,
        classes: 'border-rose-300/30 bg-rose-300/10 text-rose-200',
    },
    UNKNOWN: {
        title: '결제 결과를 확인하고 있습니다',
        description: '응답 지연 상황에서는 중복 결제를 시도하지 않고 운영 대사 결과를 기다립니다.',
        icon: Clock3,
        classes: 'border-amber-300/30 bg-amber-300/10 text-amber-200',
    },
    APPROVING: {
        title: '결제 승인을 처리하고 있습니다',
        description: '같은 승인 요청을 다시 보내지 않고 현재 주문 상태를 확인합니다.',
        icon: Loader2,
        classes: 'border-amber-300/30 bg-amber-300/10 text-amber-200',
    },
};

const CancerInsurancePaymentPage = () => {
    const navigate = useNavigate();
    const {
        flowIds,
        updateFlowIds,
        payment,
        setPayment,
        getRequestKey,
        resetFlow,
    } = useCancerInsurance();
    const [scenario, setScenario] = useState('paid');
    const [loading, setLoading] = useState(true);
    const [confirming, setConfirming] = useState(false);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
        let active = true;

        const restoreOrCreateOrder = async () => {
            if (!flowIds.applicationId) {
                setLoading(false);
                return;
            }
            try {
                const order = flowIds.paymentOrderId
                    ? await getInitialPremiumPayment(flowIds.paymentOrderId)
                    : await createInitialPremiumOrder(
                        flowIds.applicationId,
                        getRequestKey('initial-premium-order'),
                    );
                if (!active) return;
                setPayment(order);
                updateFlowIds({
                    paymentOrderId: order.paymentOrderId,
                    policyId: order.policyId,
                });
            } catch (requestError) {
                if (active) {
                    setError(requestError.message || '초회 보험료 주문을 준비하지 못했습니다.');
                }
            } finally {
                if (active) setLoading(false);
            }
        };

        restoreOrCreateOrder();
        return () => {
            active = false;
        };
    }, []);

    const selectedScenario = useMemo(
        () => PAYMENT_SCENARIOS.find((item) => item.id === scenario),
        [scenario],
    );
    const terminalCopy = STATUS_COPY[payment?.status];

    const refreshPayment = async () => {
        if (!flowIds.paymentOrderId || refreshing) return;
        setRefreshing(true);
        setError('');
        try {
            const restored = await getInitialPremiumPayment(flowIds.paymentOrderId);
            setPayment(restored);
            updateFlowIds({ policyId: restored.policyId });
        } catch (requestError) {
            setError(requestError.message || '결제 상태를 다시 확인하지 못했습니다.');
        } finally {
            setRefreshing(false);
        }
    };

    const confirmPayment = async () => {
        if (!payment?.paymentOrderId || payment.status !== 'CREATED' || confirming) return;
        setConfirming(true);
        setError('');
        try {
            const providerSeed = getRequestKey(`fake-provider-${scenario}`);
            const providerPaymentKey = `fake-${scenario}-${providerSeed}`;
            const confirmed = await confirmInitialPremiumPayment(
                payment.paymentOrderId,
                providerPaymentKey,
                payment.amount,
                getRequestKey(`initial-premium-confirm-${scenario}`),
            );
            setPayment(confirmed);
            updateFlowIds({
                paymentOrderId: confirmed.paymentOrderId,
                policyId: confirmed.policyId,
            });
        } catch (requestError) {
            setError(requestError.message || '초회 보험료를 승인하지 못했습니다.');
        } finally {
            setConfirming(false);
        }
    };

    const restart = () => {
        resetFlow();
        navigate('/cancer-insurance', { replace: true });
    };

    if (!flowIds.applicationId) {
        return <Navigate to="/cancer-insurance/application" replace />;
    }

    if (loading) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center gap-4 px-6 text-center">
                <Loader2 className="h-9 w-9 animate-spin text-[#82D8FC]" />
                <div>
                    <p className="font-bold text-white">초회 보험료 주문을 만들고 있습니다</p>
                    <p className="mt-1 text-sm text-slate-500">승인된 청약의 서버 견적 금액을 사용합니다.</p>
                </div>
            </div>
        );
    }

    if (error && !payment) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center px-8 text-center">
                <AlertCircle className="h-10 w-10 text-rose-300" />
                <p className="mt-4 font-bold text-white">결제 주문을 준비하지 못했습니다</p>
                <p className="mt-2 text-sm leading-6 text-slate-400">{error}</p>
                <AppButton
                    onClick={() => navigate('/cancer-insurance/application')}
                    fullWidth={false}
                    className="mt-6 px-6"
                >
                    청약 결과로 돌아가기
                </AppButton>
            </div>
        );
    }

    if (terminalCopy) {
        const StatusIcon = terminalCopy.icon;
        const pending = ['UNKNOWN', 'APPROVING'].includes(payment.status);
        return (
            <div className="px-6 pb-36 pt-8">
                <section className={`rounded-[30px] border p-7 ${terminalCopy.classes}`}>
                    <StatusIcon className={`h-12 w-12 ${payment.status === 'APPROVING' ? 'animate-spin' : ''}`} />
                    <p className="mt-6 text-xs font-black">결제 상태 · {payment.status}</p>
                    <h1 className="mt-2 text-2xl font-black tracking-[-0.04em] text-white">
                        {terminalCopy.title}
                    </h1>
                    <p className="mt-3 text-sm leading-6 text-slate-300">{terminalCopy.description}</p>
                </section>

                <section className="mt-5 rounded-2xl border border-slate-800 bg-[#09111F] p-5">
                    <div className="flex items-center justify-between gap-3">
                        <span className="text-sm text-slate-500">결제 주문</span>
                        <span className="text-sm font-bold text-white">{payment.orderNo}</span>
                    </div>
                    <div className="mt-3 flex items-center justify-between gap-3">
                        <span className="text-sm text-slate-500">초회 보험료</span>
                        <span className="text-sm font-black text-[#82D8FC]">{formatWon(payment.amount)}</span>
                    </div>
                    <div className="mt-3 flex items-center justify-between gap-3">
                        <span className="text-sm text-slate-500">계약 상태</span>
                        <span className="text-sm font-bold text-white">{payment.policyStatus}</span>
                    </div>
                    <div className="mt-3 flex items-center justify-between gap-3">
                        <span className="text-sm text-slate-500">PG 승인 시도</span>
                        <span className="text-sm font-bold text-white">{payment.attempts?.length || 0}건</span>
                    </div>
                </section>

                {error && (
                    <div className="mt-4 flex items-start gap-2 rounded-2xl border border-rose-400/20 bg-rose-400/10 p-4 text-sm text-rose-200">
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                        {error}
                    </div>
                )}

                <div className="mt-8 space-y-3">
                    {payment.status === 'PAID' && (
                        <AppButton onClick={() => navigate('/cancer-insurance/policy')}>
                            <FileCheck2 className="h-5 w-5" /> 발행된 보험증권 확인하기
                        </AppButton>
                    )}
                    {pending && (
                        <AppButton onClick={refreshPayment} disabled={refreshing}>
                            {refreshing
                                ? <Loader2 className="h-5 w-5 animate-spin" />
                                : <RefreshCw className="h-5 w-5" />}
                            결제 결과 다시 확인
                        </AppButton>
                    )}
                    <AppButton onClick={restart} tone="subtle">
                        <RotateCcw className="h-4 w-4" /> 새 견적으로 다시 시작
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
                    onClick={() => navigate('/cancer-insurance/application')}
                    className="-ml-2 rounded-full p-2 text-white transition-colors hover:bg-slate-800"
                    aria-label="청약 화면으로 돌아가기"
                >
                    <ChevronLeft className="h-6 w-6" />
                </button>
                <div className="ml-2">
                    <p className="text-xs font-bold text-[#82D8FC]">STEP 3 · 초회 보험료</p>
                    <h1 className="mt-0.5 text-xl font-black text-white">가상 결제를 진행해주세요</h1>
                </div>
            </header>

            <main className="space-y-6 px-6">
                <section className="overflow-hidden rounded-[28px] border border-[#82D8FC]/20 bg-gradient-to-br from-[#10253A] to-[#07101C] p-6">
                    <div className="flex items-start justify-between gap-4">
                        <div>
                            <p className="text-xs font-bold text-[#82D8FC]">INITIAL PREMIUM</p>
                            <p className="mt-2 text-3xl font-black tracking-[-0.05em] text-white">
                                {formatWon(payment.amount)}
                            </p>
                            <p className="mt-2 text-xs text-slate-500">주문 만료 {formatDateTime(payment.expiresAt)}</p>
                        </div>
                        <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#82D8FC]/10 text-[#82D8FC]">
                            <CreditCard className="h-6 w-6" />
                        </span>
                    </div>
                    <div className="mt-6 border-t border-white/10 pt-4">
                        <div className="flex items-center justify-between gap-3 text-xs">
                            <span className="text-slate-500">결제 주문번호</span>
                            <span className="font-bold text-slate-300">{payment.orderNo}</span>
                        </div>
                        <div className="mt-3 flex items-center justify-between gap-3 text-xs">
                            <span className="text-slate-500">결제 전 계약</span>
                            <span className="font-bold text-amber-200">{payment.policyStatus}</span>
                        </div>
                    </div>
                </section>

                <section className="rounded-2xl border border-amber-300/20 bg-amber-300/10 p-4">
                    <div className="flex items-start gap-3">
                        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-200" />
                        <div>
                            <p className="text-sm font-black text-amber-100">실제 결제정보를 입력하지 않습니다</p>
                            <p className="mt-1 text-xs leading-5 text-amber-100/60">
                                Fake PG가 승인·거절·응답 지연을 재현하며 실제 카드 승인이나 보험료 송금은 발생하지 않습니다.
                            </p>
                        </div>
                    </div>
                </section>

                <section>
                    <div className="mb-4 flex items-center gap-3">
                        <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[#F2BEF7]/10 text-[#F2BEF7]">
                            <ServerCog className="h-5 w-5" />
                        </span>
                        <div>
                            <p className="text-xs font-bold text-[#F2BEF7]">장애 시나리오</p>
                            <h2 className="text-lg font-black text-white">PG 응답을 선택해보세요</h2>
                        </div>
                    </div>
                    <div className="space-y-3">
                        {PAYMENT_SCENARIOS.map((item) => {
                            const ScenarioIcon = item.icon;
                            const selected = item.id === scenario;
                            return (
                                <button
                                    key={item.id}
                                    type="button"
                                    onClick={() => setScenario(item.id)}
                                    className={`flex w-full items-start gap-4 rounded-2xl border p-4 text-left transition-all ${selected ? 'border-[#82D8FC] bg-[#82D8FC]/10' : 'border-slate-800 bg-[#09111F]'}`}
                                >
                                    <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${selected ? 'bg-[#82D8FC] text-[#020715]' : 'bg-slate-800 text-slate-500'}`}>
                                        <ScenarioIcon className="h-5 w-5" />
                                    </span>
                                    <span className="min-w-0 flex-1">
                                        <span className="block text-sm font-black text-white">{item.title}</span>
                                        <span className="mt-1 block text-xs leading-5 text-slate-500">{item.description}</span>
                                    </span>
                                    <span className={`mt-1 flex h-5 w-5 items-center justify-center rounded-full border ${selected ? 'border-[#82D8FC] bg-[#82D8FC] text-[#020715]' : 'border-slate-700 text-transparent'}`}>
                                        <Check className="h-3 w-3" strokeWidth={3} />
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                </section>

                <section className="rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
                    <div className="flex items-start gap-3">
                        <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-[#82D8FC]" />
                        <p className="text-xs leading-5 text-slate-400">
                            서버 주문 금액만 승인하고, 동일 승인 요청은 한 번만 PG에 전달합니다. 성공할 때만 계약과 증권이 활성화됩니다.
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
                <AppButton onClick={confirmPayment} disabled={confirming}>
                    {confirming
                        ? <Loader2 className="h-5 w-5 animate-spin" />
                        : <CreditCard className="h-5 w-5" />}
                    {confirming ? '결제 원장을 처리하고 있어요' : selectedScenario.actionLabel}
                </AppButton>
            </div>
        </div>
    );
};

export default CancerInsurancePaymentPage;
