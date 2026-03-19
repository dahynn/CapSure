import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCapsure } from './context/CapsureContext';
import TextModal from '@/common/components/ui/modal/TextModal';
import InsuranceInfoConfirm from './components/subscribe/InsuranceInfoConfirm';
import TermsCheck from './components/subscribe/TermsCheck';
import Payment from './components/subscribe/Payment';
import SubscribeComplete from './components/subscribe/SubscribeComplete';

const CapsureCheckoutFlow = () => {
    const navigate = useNavigate();
    const { selectedProducts, setHasSubscription } = useCapsure();
    const [step, setStep] = useState('info');
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

    // If a user navigates directly here without products, kick them back
    React.useEffect(() => {
        if (selectedProducts.length === 0 && step !== 'complete') {
            navigate('/capsure-insurance/maker', { replace: true });
        }
    }, [selectedProducts, navigate, step]);

    const selectedCells = selectedProducts.map((p) => ({
        category: p.category,
        name: p.name,
        groupId: p.id,
        company: p.company,
    }));

    const performSubscription = () => {
        setIsConfirmModalOpen(false);
        setStep('terms'); // Or go to next flow part
    };

    return (
        <div className="w-full flex-1 relative">
            {step === 'info' && (
                <InsuranceInfoConfirm
                    selectedCells={selectedCells}
                    onNext={() => setStep('terms')}
                    onPrev={() => navigate(-1)}
                />
            )}

            {step === 'terms' && (
                <TermsCheck
                    selectedCells={selectedCells}
                    onNext={() => setStep('pay')}
                    onPrev={() => setStep('info')}
                    buttonText="다음"
                />
            )}

            {step === 'pay' && (
                <Payment
                    onNext={() => {
                        setHasSubscription(true);
                        setStep('complete');
                    }}
                    onPrev={() => setStep('terms')}
                />
            )}

            {step === 'complete' && (
                <SubscribeComplete 
                    selectedCells={selectedCells} 
                    onNext={() => navigate('/my-capsure', { replace: true })} 
                />
            )}

            <TextModal
                isOpen={isConfirmModalOpen}
                onClose={() => setIsConfirmModalOpen(false)}
                onConfirm={performSubscription}
                contents="진행할까요?"
                confirmText="확인"
                cancelText="취소"
            />
        </div>
    );
};

export default CapsureCheckoutFlow;
