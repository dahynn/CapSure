import { Outlet, useLocation } from 'react-router-dom';
import { CancerInsuranceProvider } from './context/CancerInsuranceContext';

const STEPS = [
    { path: '/cancer-insurance', label: '상품' },
    { path: '/cancer-insurance/application', label: '청약' },
    { path: '/cancer-insurance/payment', label: '결제' },
    { path: '/cancer-insurance/policy', label: '증권' },
    { path: '/cancer-insurance/claim', label: '청구' },
    { path: '/cancer-insurance/claim/result', label: '지급' },
];

const CancerInsuranceFlowLayout = () => {
    const location = useLocation();
    const activeIndex = Math.max(
        0,
        STEPS.reduce(
            (matchedIndex, step, index) => (
                location.pathname === step.path || location.pathname.startsWith(`${step.path}/`)
                    ? index
                    : matchedIndex
            ),
            -1,
        ),
    );
    const currentStep = STEPS[activeIndex];
    const nextStep = STEPS[activeIndex + 1];
    const progress = ((activeIndex + 1) / STEPS.length) * 100;

    return (
        <CancerInsuranceProvider>
            <div className="min-h-full overflow-x-hidden bg-[#020715]">
                <div className="relative z-30 border-b border-slate-800/70 bg-[#020715]/95 px-5 py-3 backdrop-blur-xl sm:px-6">
                    <div
                        className="mx-auto w-full max-w-[512px]"
                        aria-label={`암보험 진행 단계 ${activeIndex + 1}/${STEPS.length}, 현재 ${currentStep.label}`}
                    >
                        <div className="flex min-w-0 items-center justify-between gap-4">
                            <p className="min-w-0 whitespace-nowrap text-[13px] font-semibold text-slate-100">
                                <span className="mr-2 text-[11px] font-medium text-slate-500">가입 진행</span>
                                {currentStep.label}
                            </p>
                            <p className="shrink-0 whitespace-nowrap text-[11px] font-medium text-slate-500">
                                {activeIndex + 1} / {STEPS.length}
                                {nextStep && <span className="ml-2 text-slate-400">다음 {nextStep.label}</span>}
                            </p>
                        </div>
                        <div
                            className="mt-2 h-[3px] overflow-hidden rounded-full bg-slate-800"
                            role="progressbar"
                            aria-valuemin="1"
                            aria-valuemax={STEPS.length}
                            aria-valuenow={activeIndex + 1}
                        >
                            <div
                                className="h-full rounded-full bg-[#82D8FC] transition-[width] duration-300 ease-out"
                                style={{ width: `${progress}%` }}
                            />
                        </div>
                    </div>
                </div>
                <Outlet />
            </div>
        </CancerInsuranceProvider>
    );
};

export default CancerInsuranceFlowLayout;
