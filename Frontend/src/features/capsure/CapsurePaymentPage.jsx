import React, { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import Payment from './components/subscribe/Payment';
import { useCapsure } from './context/CapsureContext';
import { getProductSourceId } from './utils/productSource';
import { httpClient } from '@/common/api/httpClient';

const CapsurePaymentPage = () => {
    const navigate = useNavigate();
    const { selectedProducts, completeSubscription, capsuleName } = useCapsure();
    const [isSubmitting, setIsSubmitting] = useState(false);

    if (selectedProducts.length === 0) {
        return <Navigate to="/capsure-insurance/maker" replace />;
    }

    const totalPremium = selectedProducts.reduce((sum, product) => sum + product.monthlyPrice, 0);

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
        <Payment
            totalPremium={totalPremium}
            isSubmitting={isSubmitting}
            onPrev={() => navigate('/capsure-insurance/payment-summary')}
            onNext={handlePayment}
        />
    );
};

export default CapsurePaymentPage;
