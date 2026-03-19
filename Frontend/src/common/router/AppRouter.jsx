import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import MainLayout from "@/layouts/MainLayout";
import AuthPage from "@/features/auth/AuthPage";
import LoginPage from "@/features/auth/components/LoginPage";
import SignupPage from "@/features/auth/components/SignupPage";
import HomePage from "@/features/home/HomePage";
import SearchPage from "@/features/search/SearchPage";
import CapsureInsurancePage from "@/features/capsure/CapsureInsurancePage";

// MyPage Features
import Profile from "@/features/mypage/Profile";
import MyCapsureInsurance from "@/features/mypage/MyCapsureInsurance";
import InsuranceHistory from "@/features/mypage/InsuranceHistory";
import CapsureCancelPage from "@/features/capsureCancel/CapsureCancelPage";
import SearchInsurancePage from "@/features/searchInsurance/SearchInsurancePage";
import InsuranceDetailPage from "@/features/searchInsurance/InsuranceDetailPage";
import OnboardingPage from "@/features/onboarding/components/OnboardingPage";

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
                    <Route
                        path="/capsure-insurance"
                        element={<CapsureInsurancePage />}
                    />
                    <Route
                        path="/search-insurance"
                        element={<SearchInsurancePage />}
                    />
                    <Route
                        path="/search-insurance/:id"
                        element={<InsuranceDetailPage />}
                    />

                    {/* MyPage Routes */}
                    <Route path="/profile" element={<Profile />} />
                    <Route
                        path="/my-capsure"
                        element={<MyCapsureInsurance />}
                    />
                    <Route path="/my-history" element={<InsuranceHistory />} />

                    {/* Extra Flows */}
                    <Route
                        path="/capsure-cancel"
                        element={<CapsureCancelPage />}
                    />

                    <Route
                        path="/"
                        element={<Navigate to="/login" replace />}
                    />
                </Route>
            </Routes>
        </BrowserRouter>
    );
};

export default AppRouter;
