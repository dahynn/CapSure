import React, { useEffect, useState } from 'react';
import DashboardHeader from './components/DashboardHeader';
import ActiveSubscription from './components/ActiveSubscription';
import CoverageAnalysis from './components/CoverageAnalysis';
import ScheduleSummary from './components/ScheduleSummary';
import PageTransitionLoading from '@/common/components/ui/loading/PageTransitionLoading';
import { getUserProfile } from '@/features/mypage/api/mypage.api';
import {
    getCoveragePercentile,
    getDiagnosisReport,
    getMonthlyBilling,
    getScheduleBilling,
} from './api/dashboard.api';

const DashboardPage = () => {
    const MIN_TRANSITION_VISIBLE_MS = 760;
    const [dashboardData, setDashboardData] = useState({
        userName: '사용자',
        monthlyBilling: null,
        scheduleBilling: null,
        diagnosisReport: null,
        coveragePercentile: null,
    });
    const [isLoading, setIsLoading] = useState(true);
    const [showTransitionLoading, setShowTransitionLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const transitionShownAtRef = React.useRef(0);

    useEffect(() => {
        let isMounted = true;

        const fetchDashboardData = async () => {
            setIsLoading(true);
            setErrorMessage('');

            try {
                const [profile, monthlyBilling, scheduleBilling, diagnosisReport, coveragePercentile] = await Promise.all([
                    getUserProfile(),
                    getMonthlyBilling(),
                    getScheduleBilling(),
                    getDiagnosisReport(),
                    getCoveragePercentile(),
                ]);

                if (!isMounted) {
                    return;
                }

                setDashboardData({
                    userName: profile?.name || '사용자',
                    monthlyBilling,
                    scheduleBilling,
                    diagnosisReport,
                    coveragePercentile,
                });
            } catch (error) {
                if (!isMounted) {
                    return;
                }
                console.error('Failed to load dashboard page:', error);
                setErrorMessage('대시보드 정보를 불러오지 못했습니다.');
            } finally {
                if (isMounted) {
                    setIsLoading(false);
                }
            }
        };

        fetchDashboardData();

        return () => {
            isMounted = false;
        };
    }, []);

    useEffect(() => {
        let openTimer;
        let closeTimer;

        if (isLoading) {
            openTimer = window.setTimeout(() => {
                transitionShownAtRef.current = Date.now();
                setShowTransitionLoading(true);
            }, 40);
        } else if (showTransitionLoading) {
            const elapsed = Date.now() - transitionShownAtRef.current;
            const remaining = Math.max(0, MIN_TRANSITION_VISIBLE_MS - elapsed);
            closeTimer = window.setTimeout(() => {
                setShowTransitionLoading(false);
            }, remaining);
        }

        return () => {
            if (openTimer) {
                window.clearTimeout(openTimer);
            }
            if (closeTimer) {
                window.clearTimeout(closeTimer);
            }
        };
    }, [isLoading, showTransitionLoading]);

    if (showTransitionLoading) {
        return (
            <PageTransitionLoading
                message="대시보드를 불러오는 중이에요"
                backgroundClassName="bg-[#020715]"
                openDelayMs={0}
                textDelayMs={120}
                doneDelayMs={420}
            />
        );
    }

    return (
        <div className="mx-auto min-h-screen w-full max-w-[560px] animate-in px-8 py-8 transition-all fade-in duration-500 md:px-12 md:py-10">
            <DashboardHeader userName={dashboardData.userName} />

            {errorMessage ? (
                <div className="mb-8 rounded-3xl border border-red-500/30 bg-red-500/10 px-5 py-4 text-sm text-red-100">
                    {errorMessage}
                </div>
            ) : null}

            <ActiveSubscription data={dashboardData.monthlyBilling} />
            <CoverageAnalysis
                diagnosisReport={dashboardData.diagnosisReport}
                coveragePercentile={dashboardData.coveragePercentile}
            />
            <ScheduleSummary data={dashboardData.scheduleBilling} />
        </div>
    );
};

export default DashboardPage;
