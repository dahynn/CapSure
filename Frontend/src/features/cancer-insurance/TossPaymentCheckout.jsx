import React, { useEffect, useRef, useState } from 'react';
import { ANONYMOUS, loadTossPayments } from '@tosspayments/tosspayments-sdk';
import { CreditCard, Loader2 } from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';

const METHODS_SELECTOR = '#capsure-toss-payment-methods';
const AGREEMENT_SELECTOR = '#capsure-toss-payment-agreement';

export default function TossPaymentCheckout({ clientKey, payment, onError }) {
    const widgetsRef = useRef(null);
    const renderedRef = useRef([]);
    const [ready, setReady] = useState(false);
    const [requesting, setRequesting] = useState(false);

    useEffect(() => {
        let active = true;

        const initialize = async () => {
            try {
                const tossPayments = await loadTossPayments(clientKey);
                if (!active) return;
                const widgets = tossPayments.widgets({ customerKey: ANONYMOUS });
                await widgets.setAmount({
                    currency: payment.currencyCode,
                    value: Number(payment.amount),
                });
                if (!active) return;
                const rendered = await Promise.all([
                    widgets.renderPaymentMethods({
                        selector: METHODS_SELECTOR,
                        variantKey: 'DEFAULT',
                    }),
                    widgets.renderAgreement({
                        selector: AGREEMENT_SELECTOR,
                        variantKey: 'AGREEMENT',
                    }),
                ]);
                if (!active) {
                    await Promise.allSettled(rendered.map((widget) => widget.destroy()));
                    return;
                }
                widgetsRef.current = widgets;
                renderedRef.current = rendered;
                setReady(true);
            } catch (error) {
                if (active) onError(error?.message || 'Toss Payments 결제 UI를 준비하지 못했습니다.');
            }
        };

        initialize();
        return () => {
            active = false;
            widgetsRef.current = null;
            const rendered = renderedRef.current;
            renderedRef.current = [];
            Promise.allSettled(rendered.map((widget) => widget.destroy())).catch(() => {});
        };
    }, [clientKey, onError, payment.amount, payment.currencyCode]);

    const requestPayment = async () => {
        if (!ready || requesting || !widgetsRef.current) return;
        setRequesting(true);
        onError('');
        try {
            await widgetsRef.current.requestPayment({
                orderId: payment.orderNo,
                orderName: 'CapSure 암보험 초회 보험료',
                successUrl: `${window.location.origin}/cancer-insurance/payment/success`,
                failUrl: `${window.location.origin}/cancer-insurance/payment/fail`,
            });
        } catch (error) {
            if (error?.code !== 'USER_CANCEL') {
                onError(error?.message || 'Toss Payments 결제 요청을 시작하지 못했습니다.');
            }
            setRequesting(false);
        }
    };

    return (
        <>
            <section className="overflow-hidden rounded-2xl border border-slate-800 bg-white">
                <div id="capsure-toss-payment-methods" />
                <div id="capsure-toss-payment-agreement" />
                {!ready && (
                    <div className="flex items-center justify-center gap-2 px-5 py-8 text-sm text-slate-600">
                        <Loader2 className="h-4 w-4 animate-spin" /> 결제 UI를 준비하고 있습니다
                    </div>
                )}
            </section>
            <div className="fixed app-fixed-cta left-1/2 z-40 w-full max-w-[560px] -translate-x-1/2 bg-gradient-to-t from-[#020715] via-[#020715] to-transparent px-6 pb-6 pt-8">
                <AppButton onClick={requestPayment} disabled={!ready || requesting}>
                    {requesting
                        ? <Loader2 className="h-5 w-5 animate-spin" />
                        : <CreditCard className="h-5 w-5" />}
                    {requesting ? 'Toss Payments로 이동하고 있어요' : 'Toss 테스트 결제하기'}
                </AppButton>
            </div>
        </>
    );
}
