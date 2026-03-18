import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';

import DigitalSealStep from './steps/DigitalSealStep';
import MyDataConsentStep from './steps/MyDataConsentStep';
import CategorySelectionStep from './steps/CategorySelectionStep';

// 전체 단계 수
const TOTAL_STEPS = 3;

const OnboardingPage = () => {
    const navigate = useNavigate();
    
    // 현재 진행 단계 (1 ~ 3)
    const [currentStep, setCurrentStep] = useState(1);

    // 이전 단계로 이동
    const handlePrevStep = () => {
        if (currentStep > 1) {
            setCurrentStep(prev => prev - 1);
        } else {
            navigate(-1);
        }
    };

    // 다음 단계로 이동
    const handleNextStep = () => {
        if (currentStep < TOTAL_STEPS) {
            setCurrentStep(prev => prev + 1);
        }
    };

    // 온보딩 최종 완료 처리
    const handleComplete = (selectedCategories) => {
        console.log('선택한 카테고리:', selectedCategories);
        // 완료 후 홈으로 리다이렉트
        navigate('/home', { replace: true });
    };

    return (
        <div className="min-h-screen flex flex-col items-center px-6 py-6" style={{ backgroundColor: 'var(--color-bg)' }}>
            <div className="w-full max-w-sm flex-1 flex flex-col">
                
                {/* 상단 뒤로가기 (앱 내비게이션 역할) */}
                <header className="pt-2 pb-6 flex items-center shrink-0">
                    <button 
                        onClick={handlePrevStep}
                        className="p-2 -ml-2 text-white hover:opacity-70 transition-opacity"
                    >
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="19" y1="12" x2="5" y2="12"></line>
                            <polyline points="12 19 5 12 12 5"></polyline>
                        </svg>
                    </button>
                    {/* 필요 시 여기에 중앙 점선/점 인디케이터를 배치할 수 있습니다 */}
                </header>

                {/* 콘텐츠 (동적 렌더링) */}
                <main className="flex-1 flex flex-col pt-4 pb-0">
                    {currentStep === 1 && <DigitalSealStep onNext={handleNextStep} />}
                    {currentStep === 2 && <MyDataConsentStep onNext={handleNextStep} />}
                    {currentStep === 3 && <CategorySelectionStep onComplete={handleComplete} />}
                </main>

                {/* 하단 점 인디케이터 */}
                <div className="flex justify-center items-center gap-2 pb-8 pt-4">
                    {[1, 2, 3].map((step) => (
                        <div
                            key={step}
                            className={`h-2 rounded-full transition-all duration-300 ${
                                currentStep === step
                                    ? 'w-8'
                                    : 'w-2 bg-slate-600'
                            }`}
                            style={currentStep === step ? { backgroundColor: 'var(--color-brand-yellow)' } : {}}
                        />
                    ))}
                </div>
            </div>
        </div>
    );
};

export default OnboardingPage;
