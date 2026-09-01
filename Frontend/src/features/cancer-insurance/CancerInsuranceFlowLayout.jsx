import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { CancerInsuranceProvider } from './context/CancerInsuranceContext';

const STEPS = [
    { path: '/cancer-insurance', label: '상품' },
    { path: '/cancer-insurance/application', label: '청약' },
    { path: '/cancer-insurance/payment', label: '결제' },
    { path: '/cancer-insurance/policy', label: '증권' },
];

const CancerInsuranceFlowLayout = () => {
    const location = useLocation();
    const activeIndex = Math.max(
        0,
        STEPS.findIndex((step, index) => (
            index === 0
                ? location.pathname === step.path
                : location.pathname.startsWith(step.path)
        )),
    );

    return (
        <CancerInsuranceProvider>
            <div className="min-h-full bg-[#020715]">
                <div className="sticky top-[75px] z-30 border-b border-slate-800/70 bg-[#020715]/95 px-6 py-4 backdrop-blur-xl">
                    <div className="flex items-center gap-2" aria-label="암보험 가입 진행 단계">
                        {STEPS.map((step, index) => (
                            <React.Fragment key={step.path}>
                                <div className="flex min-w-0 items-center gap-2">
                                    <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-black ${index <= activeIndex ? 'bg-[#82D8FC] text-[#020715]' : 'bg-slate-800 text-slate-500'}`}>
                                        {index + 1}
                                    </span>
                                    <span className={`text-xs font-bold ${index <= activeIndex ? 'text-white' : 'text-slate-600'}`}>
                                        {step.label}
                                    </span>
                                </div>
                                {index < STEPS.length - 1 && (
                                    <span className={`h-px flex-1 ${index < activeIndex ? 'bg-[#82D8FC]/60' : 'bg-slate-800'}`} />
                                )}
                            </React.Fragment>
                        ))}
                    </div>
                </div>
                <Outlet />
            </div>
        </CancerInsuranceProvider>
    );
};

export default CancerInsuranceFlowLayout;
