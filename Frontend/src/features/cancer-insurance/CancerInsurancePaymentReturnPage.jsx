import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, CheckCircle2, Clock3, Loader2, RotateCcw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AppButton from '@/common/components/ui/button/AppButton';
import { confirmInitialPremiumPayment, getInitialPremiumPayment } from './api/cancerInsurance.api';
import { useCancerInsurance } from './context/CancerInsuranceContext';

const confirmationKey = (paymentOrderId) => `toss-confirm-${paymentOrderId}`;

export default function CancerInsurancePaymentReturnPage({ result }) {
    const navigate = useNavigate();
    const { flowIds, setPayment, updateFlowIds } = useCancerInsurance();
    const params = useMemo(() => new URLSearchParams(window.location.search), []);
    const [status, setStatus] = useState(result === 'success' ? 'confirming' : 'failed');
    const [message, setMessage] = useState(
        result === 'success'
            ? '서버 주문과 Toss Payments 인증 결과를 확인하고 있습니다.'
            : params.get('message') || '결제 인증이 취소되었거나 실패했습니다.',
    );

    useEffect(() => {
        if (result !== 'success') return undefined;
        let active = true;

        const confirm = async () => {
            let approvalRequested = false;
            const paymentKey = params.get('paymentKey');
            const orderId = params.get('orderId');
            const amount = params.get('amount');
            if (!flowIds.paymentOrderId || !paymentKey || !orderId || !amount) {
                setStatus('failed');
                setMessage('결제 승인에 필요한 정보가 없거나 만료되었습니다. 결제 화면으로 돌아가 다시 확인해주세요.');
                return;
            }
            try {
                const stored = await getInitialPremiumPayment(flowIds.paymentOrderId);
                if (stored.orderNo !== orderId || Number(stored.amount) !== Number(amount)) {
                    throw new Error('결제 인증 결과가 서버 주문번호 또는 금액과 일치하지 않습니다.');
                }
                approvalRequested = stored.status === 'CREATED';
                const confirmed = stored.status !== 'CREATED' ? stored : await confirmInitialPremiumPayment(
                    stored.paymentOrderId,
                    paymentKey,
                    orderId,
                    amount,
                    confirmationKey(stored.paymentOrderId),
                );
                if (!active) return;
                setPayment(confirmed);
                updateFlowIds({ paymentOrderId: confirmed.paymentOrderId, policyId: confirmed.policyId });
                if (confirmed.status === 'PAID') {
                    setStatus('completed');
                    setMessage('Toss Payments 테스트 승인과 계약 활성화를 완료했습니다.');
                } else if (['APPROVING', 'UNKNOWN'].includes(confirmed.status)) {
                    setStatus('pending');
                    setMessage('승인 결과를 단정하지 않고 서버 대사 결과를 기다리고 있습니다. 중복 결제는 시도하지 마세요.');
                } else {
                    setStatus('failed');
                    setMessage('Toss Payments에서 결제가 승인되지 않았습니다.');
                }
            } catch (error) {
                if (!active) return;
                setStatus(approvalRequested ? 'pending' : 'failed');
                setMessage(approvalRequested
                    ? '승인 요청 후 결과를 확인하지 못했습니다. 결제 화면에서 주문 상태를 다시 확인해주세요.'
                    : error.message || '결제 승인 결과를 처리하지 못했습니다.');
            }
        };

        const startTimer = window.setTimeout(() => {
            if (active) confirm();
        }, 0);
        return () => {
            active = false;
            window.clearTimeout(startTimer);
        };
    }, [flowIds.paymentOrderId, params, result, setPayment, updateFlowIds]);

    const completed = status === 'completed';
    const confirming = status === 'confirming';
    const pending = status === 'pending';
    const StatusIcon = confirming ? Loader2 : completed ? CheckCircle2 : pending ? Clock3 : AlertCircle;
    const statusIconClassName = confirming
        ? 'h-12 w-12 animate-spin text-[#82D8FC]'
        : completed
            ? 'h-12 w-12 text-emerald-300'
            : pending
                ? 'h-12 w-12 text-amber-200'
                : 'h-12 w-12 text-rose-300';

    return (
        <div className="flex min-h-[620px] flex-col items-center justify-center px-7 text-center">
            <StatusIcon className={statusIconClassName} />
            <p className="mt-6 text-xs font-black text-[#82D8FC]">TOSS PAYMENTS · TEST</p>
            <h1 className="mt-2 text-2xl font-black text-white">
                {confirming
                    ? '결제 승인 확인 중'
                    : completed
                        ? '테스트 결제 완료'
                        : pending
                            ? '결제 결과 확인 중'
                            : '테스트 결제 미완료'}
            </h1>
            <p className="mt-3 max-w-sm text-sm leading-6 text-slate-400">{message}</p>
            {!confirming && (
                <div className="mt-8 w-full max-w-sm space-y-3">
                    {completed && (
                        <AppButton onClick={() => navigate('/cancer-insurance/policy', { replace: true })}>
                            발행된 보험증권 확인하기
                        </AppButton>
                    )}
                    <AppButton onClick={() => navigate('/cancer-insurance/payment', { replace: true })} tone="subtle">
                        <RotateCcw className="h-4 w-4" /> 결제 화면으로 돌아가기
                    </AppButton>
                </div>
            )}
        </div>
    );
}
