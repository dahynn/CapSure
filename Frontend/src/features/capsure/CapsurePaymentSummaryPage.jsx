import React, { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import InsuranceInfoConfirm from './components/subscribe/InsuranceInfoConfirm';
import { useCapsure } from './context/CapsureContext';
import { getProductSourceId } from './utils/productSource';
import { httpClient } from '@/common/api/httpClient';
import { getCurrentPaymentMethod } from '@/features/mypage/api/mypage.api';

const CapsurePaymentSummaryPage = () => {
    const navigate = useNavigate();
    const { selectedProducts, capsuleName, setCapsuleName, completeSubscription } = useCapsure();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [paymentMethod, setPaymentMethod] = useState(null);
    const [paymentLoading, setPaymentLoading] = useState(true);
    const [paymentError, setPaymentError] = useState('');

    if (selectedProducts.length === 0) {
        return <Navigate to="/capsure-insurance/maker" replace />;
    }

    const totalPremium = selectedProducts.reduce((sum, product) => sum + product.monthlyPrice, 0);

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
        const rafId = window.requestAnimationFrame(() => {
            window.scrollTo({ top: 0, behavior: 'auto' });
        });
        return () => window.cancelAnimationFrame(rafId);
    }, []);

    useEffect(() => {
        let active = true;

        const fetchPaymentMethod = async () => {
            setPaymentLoading(true);
            setPaymentError('');
            try {
                const data = await getCurrentPaymentMethod();
                if (!active) {
                    return;
                }
                setPaymentMethod(data ?? null);
            } catch (error) {
                if (!active) {
                    return;
                }
                console.error('Payment method load error', error);
                setPaymentMethod(null);
                setPaymentError('등록된 결제수단을 불러오지 못했습니다.');
            } finally {
                if (active) {
                    setPaymentLoading(false);
                }
            }
        };

        fetchPaymentMethod();
        return () => {
            active = false;
        };
    }, []);

    const handlePayment = async () => {
        if (isSubmitting) {
            return;
        }

        setIsSubmitting(true);
        try {
            const productSourceIds = selectedProducts.map(getProductSourceId);
            const response = await httpClient.post('/subscriptions', {
                productSourceIds,
                capsuleName: capsuleName?.trim(),
            });
            const data = response.data;

            if (!data.success) {
                alert(data.message || '결제 처리 중 오류가 발생했습니다.');
                return;
            }

            completeSubscription({
                subscriptionId: data.data,
                products: selectedProducts,
                capsuleName,
            });

            navigate('/capsure-insurance/result', {
                state: {
                    subscriptionId: data.data,
                    capsuleName,
                    totalPremium,
                    products: selectedProducts,
                },
            });
        } catch (error) {
            console.error('Subscription payment error', error);
            alert('결제 처리 중 서버 오류가 발생했습니다. 다시 시도해 주세요.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <InsuranceInfoConfirm
            selectedProducts={selectedProducts}
            capsuleName={capsuleName}
            onCapsuleNameChange={setCapsuleName}
            paymentMethod={paymentMethod}
            paymentLoading={paymentLoading}
            paymentError={paymentError}
            isSubmitting={isSubmitting}
            onPrev={() => navigate('/capsure-insurance/terms')}
            onNext={handlePayment}
        />
    );
};

export default CapsurePaymentSummaryPage;
