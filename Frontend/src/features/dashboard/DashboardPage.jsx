import React, { useEffect, useState } from 'react';
import { Loader2 } from 'lucide-react';
import DashboardHeader from './components/DashboardHeader';
import ActiveSubscription from './components/ActiveSubscription';
import CoverageAnalysis from './components/CoverageAnalysis';
import ScheduleSummary from './components/ScheduleSummary';
import { getUserProfile } from '@/features/mypage/api/mypage.api';
import {
    getCoveragePercentile,
    getDiagnosisReport,
    getMonthlyBilling,
    getScheduleBilling,
} from './api/dashboard.api';

const DashboardPage = () => {
    const [dashboardData, setDashboardData] = useState({
        userName: '사용자',
        monthlyBilling: null,
        scheduleBilling: null,
        diagnosisReport: null,
        coveragePercentile: null,
    });
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');

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

    if (isLoading) {
        return (
            <div className="mx-auto flex min-h-screen w-full max-w-[560px] items-center justify-center px-8 py-8 md:px-12 md:py-10">
                <Loader2 className="h-9 w-9 animate-spin text-[#82D8FC]" />
            </div>
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
