import React from 'react';
import { useNavigate } from 'react-router-dom';
import CapsureBudgetView from './components/CapsureBudgetView';
import { useCapsure } from './context/CapsureContext';

const CapsureBudgetPage = () => {
    const navigate = useNavigate();
    const { setTotalBudget } = useCapsure();

    const handleProceed = (amount) => {
        setTotalBudget(amount);
        navigate('/capsure-insurance/maker');
    };

    return (
        <CapsureBudgetView onProceed={handleProceed} />
    );
};

export default CapsureBudgetPage;
