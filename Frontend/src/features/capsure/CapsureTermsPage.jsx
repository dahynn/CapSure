import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCapsure } from './context/CapsureContext';
import { X, Check } from 'lucide-react';
import TermsCard from './components/TermsCard';

// Mock function to generate terms based on product company or name
const getMockTerms = (product) => {
    const nameStr = (product.company + ' ' + product.name).toLowerCase();
    
    if (nameStr.includes('현대')) {
        return [
            "상해 사고 시 수술비 최대 500만원 지원 (특약 가입 시)",
            "일상생활 배상책임 담보 포함 (자기부담금 20만원)"
        ];
    }
    if (nameStr.includes('삼성')) {
        return [
            "암 진단 시 확정일로부터 90일 면책 기간 적용",
            "응급실 내원 시 비응급 환자도 정액 보상 (3만원)"
        ];
    }
    if (nameStr.includes('db')) {
        return [
            "교통사고 처리 지원금 최대 2억원 보장",
            "법률 비용 손해 및 벌금 보장 (특약)"
        ];
    }
    
    // Default generic terms
    return [
        "선택하신 상품에 대한 주요 보장 내용과 유의사항입니다.",
        "자세한 사항은 상세 약관 보기를 통해 반드시 확인해주세요."
    ];
};

const CapsureTermsPage = () => {
    const navigate = useNavigate();
    const { selectedProducts, completeSubscription } = useCapsure();
    
    // Initialize agreements state, false for each selected product
    const [agreements, setAgreements] = useState(
        selectedProducts.reduce((acc, product) => {
            acc[product.id] = false;
            return acc;
        }, {})
    );

    const isAllAgreed = selectedProducts.length > 0 && Object.values(agreements).every(Boolean);

    const handleToggle = (id) => {
        setAgreements(prev => ({
            ...prev,
            [id]: !prev[id]
        }));
    };

    const handleToggleAll = () => {
        const newValue = !isAllAgreed;
        setAgreements(
            selectedProducts.reduce((acc, product) => {
                acc[product.id] = newValue;
                return acc;
            }, {})
        );
    };

    const handleNext = () => {
        if (!isAllAgreed) return;
        // In a real app, we would complete the subscription and navigate to a success page
        completeSubscription();
        navigate('/home', { state: { subscriptionSuccess: true } });
    };

    // Redirect to home if accessed directly without products
    if (selectedProducts.length === 0) {
        return (
            <div className="flex-1 flex flex-col items-center justify-center p-6 text-center">
                <p className="text-white mb-4">선택된 상품이 없습니다.</p>
                <button 
                    onClick={() => navigate('/capsure-insurance')}
                    className="bg-brand-blue text-[#020715] px-6 py-3 rounded-xl font-bold"
                >
                    캡슐 보험 시작하기
                </button>
            </div>
        );
    }

    return (
        <div className="flex flex-col min-h-full pb-32">
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 sticky top-0 z-10 bg-capsure-bg">
                <div className="flex items-center gap-2">
                    <span className="text-white font-bold tracking-widest">CAPSURE</span>
                </div>
                <button onClick={() => navigate('/home')} className="text-slate-400 hover:text-white transition-colors">
                    <X className="w-6 h-6" />
                </button>
            </div>

            <div className="px-6 pt-4 pb-8">
                <h1 className="text-[26px] font-bold text-white mb-3 tracking-tight">
                    약관 확인 및 동의
                </h1>
                <p className="text-slate-400 text-sm leading-snug mb-8 break-keep">
                    보안 알고리즘이 분석한 보험사별 핵심 약관 요약입니다. 내용을 확인하고 동의해 주세요.
                </p>

                {/* Terms Cards */}
                <div className="flex flex-col">
                    {selectedProducts.map(product => (
                        <TermsCard 
                            key={product.id}
                            product={product}
                            termsList={getMockTerms(product)}
                            isChecked={agreements[product.id]}
                            onToggle={() => handleToggle(product.id)}
                        />
                    ))}
                </div>
            </div>

            {/* Bottom Sticky Action Area */}
            <div className="fixed bottom-0 w-full max-w-[560px] p-6 bg-capsure-bg border-t border-slate-800 z-20">
                {/* 둥근 체크박스 영역 */}
                <div className="bg-[#192235] border border-slate-700/60 rounded-2xl p-4 mb-4 flex items-start gap-3 cursor-pointer" onClick={handleToggleAll}>
                    <div className="mt-0.5">
                        <div className={`w-6 h-6 rounded-full border flex items-center justify-center transition-all ${isAllAgreed ? 'bg-slate-700 border-slate-500' : 'border-slate-500'}`}>
                            {isAllAgreed && <Check className="w-4 h-4 text-white" strokeWidth={3} />}
                        </div>
                    </div>
                    <div className="flex flex-col flex-1">
                        <span className="text-white font-bold text-base mb-1">필수 약관 전체 동의</span>
                        <p className="text-slate-400 text-xs leading-snug break-keep">
                            모든 보험사의 핵심 요약 내용을 확인하였으며 이에 동의합니다.
                        </p>
                    </div>
                </div>

                <button 
                    disabled={!isAllAgreed}
                    onClick={handleNext}
                    className={`w-full py-4 rounded-2xl font-bold text-lg transition-all ${isAllAgreed ? 'bg-[#7CE1FA] text-[#020715] active:scale-[0.98]' : 'bg-slate-800 text-slate-500 cursor-not-allowed'}`}
                >
                    다음 단계
                </button>
            </div>
        </div>
    );
};

export default CapsureTermsPage;
