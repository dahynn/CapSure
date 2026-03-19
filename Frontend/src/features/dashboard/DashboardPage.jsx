import React from 'react';
import DashboardHeader from './components/DashboardHeader';
import ActiveSubscription from './components/ActiveSubscription';
import CoverageAnalysis from './components/CoverageAnalysis';
import ScheduleSummary from './components/ScheduleSummary';

const DashboardPage = () => {
    // 백엔드 연동 전까지 하드코딩된 유저, 차후엔 props나 context/api로 받아옵니다.
    const userName = "사용자";

    return (
        <div className="px-8 py-8 md:px-12 md:py-10 max-w-[560px] mx-auto w-full transition-all min-h-screen animate-in fade-in duration-500">
            <DashboardHeader userName={userName} />
            <ActiveSubscription />
            <CoverageAnalysis />
            <ScheduleSummary />
        </div>
    );
};

export default DashboardPage;
