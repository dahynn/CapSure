import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import AuthPage from '@/features/auth/AuthPage';
import LoginPage from '@/features/auth/components/LoginPage';
import SignupPage from '@/features/auth/components/SignupPage';
import HomePage from '@/features/home/HomePage';
import ActiveInsurancesPage from '@/features/home/ActiveInsurancesPage';
import SearchPage from '@/features/search/SearchPage';

// Refactored Capsure Feature Routes
import CapsureFlowLayout from '@/features/capsure/CapsureFlowLayout';
import CapsureBudgetPage from '@/features/capsure/CapsureBudgetPage';
import CapsureMakerPage from '@/features/capsure/CapsureMakerPage';
import CapsureProductDetailPage from '@/features/capsure/CapsureProductDetailPage';
import CapsureTermsPage from '@/features/capsure/CapsureTermsPage';
import CapsurePaymentSummaryPage from '@/features/capsure/CapsurePaymentSummaryPage';
import CapsureResultPage from '@/features/capsure/CapsureResultPage';

// MyPage Features
import MyPage from '@/features/mypage/MyPage';
import ProfileEditPage from '@/features/mypage/components/ProfileEditPage';
import CapsuleDetailPage from '@/features/mypage/components/CapsuleDetailPage';
import CapsuleEditPage from '@/features/mypage/components/CapsuleEditPage';

import OnboardingPage from '@/features/onboarding/components/OnboardingPage';

// Dashboard Feature
import DashboardPage from '@/features/dashboard/DashboardPage';
import DiagnosisReportPage from '@/features/dashboard/DiagnosisReportPage';

// Cancer Insurance Financial Process
import CancerInsuranceFlowLayout from '@/features/cancer-insurance/CancerInsuranceFlowLayout';
import CancerInsuranceProductPage from '@/features/cancer-insurance/CancerInsuranceProductPage';
import CancerInsuranceApplicationPage from '@/features/cancer-insurance/CancerInsuranceApplicationPage';

const AppRouter = () => {
  return (
    <BrowserRouter>
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
          </Route>

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
    </BrowserRouter>
  );
};

export default AppRouter;
