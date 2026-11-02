import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';

const AuthPage = lazy(() => import('@/features/auth/AuthPage'));
const LoginPage = lazy(() => import('@/features/auth/components/LoginPage'));
const SignupPage = lazy(() => import('@/features/auth/components/SignupPage'));
const HomePage = lazy(() => import('@/features/home/HomePage'));
const ActiveInsurancesPage = lazy(() => import('@/features/home/ActiveInsurancesPage'));
const SearchPage = lazy(() => import('@/features/search/SearchPage'));
const CapsureFlowLayout = lazy(() => import('@/features/capsure/CapsureFlowLayout'));
const CapsureBudgetPage = lazy(() => import('@/features/capsure/CapsureBudgetPage'));
const CapsureMakerPage = lazy(() => import('@/features/capsure/CapsureMakerPage'));
const CapsureProductDetailPage = lazy(() => import('@/features/capsure/CapsureProductDetailPage'));
const CapsureTermsPage = lazy(() => import('@/features/capsure/CapsureTermsPage'));
const CapsurePaymentSummaryPage = lazy(() => import('@/features/capsure/CapsurePaymentSummaryPage'));
const CapsureResultPage = lazy(() => import('@/features/capsure/CapsureResultPage'));
const MyPage = lazy(() => import('@/features/mypage/MyPage'));
const ProfileEditPage = lazy(() => import('@/features/mypage/components/ProfileEditPage'));
const CapsuleDetailPage = lazy(() => import('@/features/mypage/components/CapsuleDetailPage'));
const CapsuleEditPage = lazy(() => import('@/features/mypage/components/CapsuleEditPage'));
const OnboardingPage = lazy(() => import('@/features/onboarding/components/OnboardingPage'));
const DashboardPage = lazy(() => import('@/features/dashboard/DashboardPage'));
const DiagnosisReportPage = lazy(() => import('@/features/dashboard/DiagnosisReportPage'));
const CancerInsuranceFlowLayout = lazy(() => import('@/features/cancer-insurance/CancerInsuranceFlowLayout'));
const CancerInsuranceProductPage = lazy(() => import('@/features/cancer-insurance/CancerInsuranceProductPage'));
const CancerInsuranceApplicationPage = lazy(() => import('@/features/cancer-insurance/CancerInsuranceApplicationPage'));
const CancerInsurancePaymentPage = lazy(() => import('@/features/cancer-insurance/CancerInsurancePaymentPage'));
const CancerInsurancePaymentReturnPage = lazy(() => import('@/features/cancer-insurance/CancerInsurancePaymentReturnPage'));
const CancerInsurancePolicyPage = lazy(() => import('@/features/cancer-insurance/CancerInsurancePolicyPage'));
const CancerInsuranceClaimPage = lazy(() => import('@/features/cancer-insurance/CancerInsuranceClaimPage'));
const CancerInsuranceClaimResultPage = lazy(() => import('@/features/cancer-insurance/CancerInsuranceClaimResultPage'));
const CancerInsuranceOperationsPage = lazy(() => import('@/features/cancer-insurance/CancerInsuranceOperationsPage'));

const RouteFallback = () => (
  <div className="flex min-h-screen items-center justify-center bg-[#020715] px-6 text-sm text-slate-300">
    화면을 불러오는 중입니다.
  </div>
);

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Suspense fallback={<RouteFallback />}>
        <Routes>
        <Route element={<AuthPage />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
        </Route>

        {/* 내비/푸터 없는 독립 경로 */}
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/search" element={<SearchPage />} />

        <Route element={<MainLayout />}>
          <Route path="/home" element={<HomePage />} />
          <Route path="/home/active-insurances" element={<ActiveInsurancesPage />} />

          {/* Capsure Insurance Flow (Context Provider Wrapped) */}
          <Route path="/capsure-insurance" element={<CapsureFlowLayout />}>
            <Route index element={<CapsureBudgetPage />} />
            <Route path="maker" element={<CapsureMakerPage />} />
            <Route path="detail/:id" element={<CapsureProductDetailPage />} />
            <Route path="terms" element={<CapsureTermsPage />} />
            <Route path="payment-summary" element={<CapsurePaymentSummaryPage />} />
            <Route path="result" element={<CapsureResultPage />} />
          </Route>

          {/* Dashboard Route */}
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/dashboard/diagnosis-report" element={<DiagnosisReportPage />} />

          <Route path="/cancer-insurance" element={<CancerInsuranceFlowLayout />}>
            <Route index element={<CancerInsuranceProductPage />} />
            <Route path="application" element={<CancerInsuranceApplicationPage />} />
            <Route path="payment" element={<CancerInsurancePaymentPage />} />
            <Route path="payment/success" element={<CancerInsurancePaymentReturnPage result="success" />} />
            <Route path="payment/fail" element={<CancerInsurancePaymentReturnPage result="fail" />} />
            <Route path="policy" element={<CancerInsurancePolicyPage />} />
            <Route path="claim" element={<CancerInsuranceClaimPage />} />
            <Route path="claim/result" element={<CancerInsuranceClaimResultPage />} />
          </Route>
          <Route path="/cancer-insurance/operations" element={<CancerInsuranceOperationsPage />} />

          <Route path="/my-capsure" element={<Navigate to="/mypage/capsure" replace />} />

          {/* consolidated MyPage Routes */}
            <Route path="/mypage">
              <Route index element={<MyPage initialView="main" />} />
              <Route path="edit" element={<ProfileEditPage />} />
              <Route path="capsure" element={<MyPage initialView="capsule" />} />
              <Route path="capsule/:id" element={<CapsuleDetailPage />} />
              <Route path="capsule/:id/edit" element={<CapsuleEditPage />} />
              <Route path="history" element={<MyPage initialView="history" />} />
              <Route path="payment-methods" element={<MyPage initialView="payment" />} />
            </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
        </Route>
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
};

export default AppRouter;
