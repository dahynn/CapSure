import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import TextModal from '@/common/components/ui/modal/TextModal';
import CapsureBudgetView from './components/CapsureBudgetView';
import CapsureMakerView from './components/CapsureMakerView';
import CapsureProductDetail from './components/CapsureProductDetail';

// Subscribe Flow Components
import InsuranceInfoConfirm from './components/subscribe/InsuranceInfoConfirm';
import TermsCheck from './components/subscribe/TermsCheck';
import Payment from './components/subscribe/Payment';
import SubscribeComplete from './components/subscribe/SubscribeComplete';
import MyCapsureInsurance from '@/features/mypage/MyCapsureInsurance';

const CapsureInsurancePage = () => {
    const navigate = useNavigate();
    const [hasSubscription, setHasSubscription] = useState(false);
    
    // View State
    const [view, setView] = useState('budget'); 
    const [detailProduct, setDetailProduct] = useState(null);
    
    // Budget State
    const [totalBudget, setTotalBudget] = useState(100000);

    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

    // Cart State
    const [selectedProducts, setSelectedProducts] = useState([]);

    const handleAddItem = (product) => {
        const currentAmount = selectedProducts.reduce((sum, p) => sum + p.price, 0);
        if (currentAmount + product.price > totalBudget) {
            alert("예산을 초과할 수 없습니다.");
            return;
        }
        if (selectedProducts.find(p => p.id === product.id)) return;
        setSelectedProducts([...selectedProducts, product]);
    };

    const handleRemoveItem = (id) => {
        setSelectedProducts(selectedProducts.filter(p => p.id !== id));
    };

    // For Compatibility with existing subscribe flow
    const selectedCells = selectedProducts.map(p => ({
        category: p.category,
        name: p.name,
        groupId: p.id,
        company: p.company
    }));

    const handleSubscribeConfirm = () => {
        if (selectedProducts.length === 0) {
            alert("담은 상품이 없습니다.");
            return;
        }
        setView('step-info'); 
    };

    const performSubscription = () => {
        setIsConfirmModalOpen(false);
        setView('step-info');
    };

    return (
        <div className="min-h-screen bg-[#020715] flex flex-col items-center">
            <div className="w-full max-w-[560px] flex flex-col min-h-screen relative bg-[#020715] animate-in slide-in-from-bottom-8 fade-in duration-500">
                
                {/* ----------------- BUDGET VIEW ----------------- */}
                {!hasSubscription && view === 'budget' && (
                    <CapsureBudgetView 
                        onProceed={(amount) => {
                            setTotalBudget(amount);
                            setView('maker');
                        }} 
                    />
                )}

                {/* ----------------- MAKER VIEW ----------------- */}
                {!hasSubscription && view === 'maker' && (
                    <CapsureMakerView 
                        totalBudget={totalBudget}
                        selectedProducts={selectedProducts}
                        onAddItem={handleAddItem}
                        onRemoveItem={handleRemoveItem}
                        onConfirm={handleSubscribeConfirm}
                        onViewDetail={(p) => {
                            setDetailProduct(p);
                            setView('detail');
                        }}
                    />
                )}

                {/* ----------------- DETAIL VIEW ----------------- */}
                {!hasSubscription && view === 'detail' && detailProduct && (
                    <CapsureProductDetail
                        product={detailProduct}
                        onBack={() => setView('maker')}
                        onAdd={() => {
                            if (!selectedProducts.find(p => p.id === detailProduct.id)) {
                                handleAddItem(detailProduct);
                            }
                            setView('maker');
                        }}
                        isAdded={!!selectedProducts.find(p => p.id === detailProduct.id)}
                    />
                )}

                {/* ----------------- SUBSCRIBE FLOW VIEWS ----------------- */}
                {view === 'step-info' && (
                    <InsuranceInfoConfirm
                        selectedCells={selectedCells}
                        onNext={() => setView('step-terms')}
                        onPrev={() => setView('maker')}
                    />
                )}

                {view === 'step-terms' && (
                    <TermsCheck
                        selectedCells={selectedCells}
                        onNext={() => setView('step-pay')}
                        onPrev={() => setView('step-info')}
                        buttonText="다음"
                    />
                )}

                {view === 'step-pay' && (
                    <Payment
                        onNext={() => {
                            setHasSubscription(true);
                            setView('step-complete');
                        }}
                        onPrev={() => setView('step-terms')}
                    />
                )}

                {view === 'step-complete' && (
                    <SubscribeComplete
                        selectedCells={selectedCells}
                        onNext={() => setView('my-capsure')}
                    />
                )}

                {/* ----------------- MY CAPSURE VIEW ----------------- */}
                {view === 'my-capsure' && (
                    <MyCapsureInsurance />
                )}

            </div>

            {/* Modals */}
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

export default CapsureInsurancePage;
