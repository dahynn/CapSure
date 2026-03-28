import React from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import InsuranceInfoConfirm from './components/subscribe/InsuranceInfoConfirm';
import { useCapsure } from './context/CapsureContext';

const CapsurePaymentSummaryPage = () => {
    const navigate = useNavigate();
    const { selectedProducts, capsuleName, setCapsuleName } = useCapsure();

    if (selectedProducts.length === 0) {
        return <Navigate to="/capsure-insurance/maker" replace />;
    }

    return (
        <InsuranceInfoConfirm
            selectedProducts={selectedProducts}
            capsuleName={capsuleName}
            onCapsuleNameChange={setCapsuleName}
            onPrev={() => navigate('/capsure-insurance/terms')}
            onNext={() => navigate('/capsure-insurance/payment')}
        />
    );
};

export default CapsurePaymentSummaryPage;
