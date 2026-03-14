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
        <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
            <div className="w-full max-w-md bg-white rounded-3xl shadow-xl shadow-slate-200/50 overflow-hidden flex flex-col h-[85vh] max-h-[800px]">
                
                {/* 헤더 부분 (뒤로가기 & 진행 바) */}
                <header className="px-6 py-4 flex flex-col gap-4 border-b border-slate-100 shrink-0">
                    <div className="flex items-center justify-between">
                        <button 
                            onClick={handlePrevStep}
                            className="p-2 -ml-2 text-slate-500 hover:text-slate-800 hover:bg-slate-50 rounded-full transition-colors"
                        >
                            <ChevronLeft className="w-6 h-6" />
                        </button>
                        
                        <span className="text-sm font-semibold text-primary-600">
                            {currentStep} / {TOTAL_STEPS}
                        </span>
                        
                        <div className="w-10"></div> {/* 여백 맞추기용 더미 */}
                    </div>

                    {/* 진행 상태 바 */}
                    <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
                        <div 
                            className="h-full bg-primary-500 transition-all duration-300 ease-out rounded-full"
                            style={{ width: `${(currentStep / TOTAL_STEPS) * 100}%` }}
                        />
                    </div>
                </header>

                {/* 콘텐츠 (동적 렌더링) */}
                <main className="flex-1 overflow-hidden p-6">
                    {currentStep === 1 && <DigitalSealStep onNext={handleNextStep} />}
                    {currentStep === 2 && <MyDataConsentStep onNext={handleNextStep} />}
                    {currentStep === 3 && <CategorySelectionStep onComplete={handleComplete} />}
                </main>

            </div>
        </div>
    );
};

export default OnboardingPage;
