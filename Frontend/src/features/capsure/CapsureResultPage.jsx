import React from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useCapsure } from './context/CapsureContext';
import SubscribeComplete from './components/subscribe/SubscribeComplete';

const CapsureResultPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { checkoutSummary } = useCapsure();

    React.useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
        const rafId = window.requestAnimationFrame(() => {
            window.scrollTo({ top: 0, behavior: 'auto' });
        });
        return () => window.cancelAnimationFrame(rafId);
    }, []);

    const summary = checkoutSummary ?? (location.state?.subscriptionId ? {
        subscriptionId: location.state.subscriptionId,
        capsuleName: location.state.capsuleName ?? '나만의 캡슐',
        products: location.state.products ?? [],
        totalPremium: location.state.totalPremium ?? 0,
    } : null);

    if (!summary) {
        return <Navigate to="/capsure-insurance/maker" replace />;
    }

    return (
        <SubscribeComplete
            selectedProducts={summary.products}
            totalPremium={summary.totalPremium}
            subscriptionId={summary.subscriptionId}
            capsuleName={summary.capsuleName}
            onNext={() => navigate('/home')}
        />
    );
};

export default CapsureResultPage;
