import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import AuthPage from '@/features/auth/AuthPage';
import LoginPage from '@/features/auth/components/LoginPage';
import SignupPage from '@/features/auth/components/SignupPage';
import HomePage from '@/features/home/HomePage';
import SearchPage from '@/features/search/SearchPage';

// Refactored Capsure Feature Routes
import CapsureFlowLayout from '@/features/capsure/CapsureFlowLayout';
import CapsureBudgetPage from '@/features/capsure/CapsureBudgetPage';
import CapsureMakerPage from '@/features/capsure/CapsureMakerPage';
import CapsureProductDetailPage from '@/features/capsure/CapsureProductDetailPage';
import CapsureTermsPage from '@/features/capsure/CapsureTermsPage';

// MyPage Features
import MyPage from '@/features/mypage/MyPage';

import OnboardingPage from '@/features/onboarding/components/OnboardingPage';

// Dashboard Feature
import DashboardPage from '@/features/dashboard/DashboardPage';

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

          {/* Capsure Insurance Flow (Context Provider Wrapped) */}
          <Route path="/capsure-insurance" element={<CapsureFlowLayout />}>
            <Route index element={<CapsureBudgetPage />} />
            <Route path="maker" element={<CapsureMakerPage />} />
            <Route path="detail/:id" element={<CapsureProductDetailPage />} />
            <Route path="terms" element={<CapsureTermsPage />} />
          </Route>

          {/* Dashboard Route */}
          <Route path="/dashboard" element={<DashboardPage />} />

          {/* consolidated MyPage Routes */}
          <Route path="/mypage">
            <Route index element={<MyPage initialView="main" />} />
            <Route path="capsure" element={<MyPage initialView="capsule" />} />
            <Route path="history" element={<MyPage initialView="history" />} />
          </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;
