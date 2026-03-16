import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import MainLayout from "@/layouts/MainLayout";
import AuthPage from "@/features/auth/AuthPage";
import LoginPage from "@/features/auth/components/LoginPage";
import SignupPage from "@/features/auth/components/SignupPage";
import HomePage from "@/features/home/HomePage";
import CapsuleInsurancePage from "@/features/capsule/CapsuleInsurancePage";

// MyPage Features
import Profile from "@/features/mypage/Profile";
import MyCapsuleInsurance from "@/features/mypage/MyCapsuleInsurance";
import InsuranceHistory from "@/features/mypage/InsuranceHistory";
import CapsuleCancelPage from "@/features/capsuleCancel/CapsuleCancelPage";
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
                <Route element={<MainLayout />}>
                    <Route path="/onboarding" element={<OnboardingPage />} />
                    <Route path="/home" element={<HomePage />} />
                    <Route
                        path="/capsule-insurance"
                        element={<CapsuleInsurancePage />}
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
                        path="/my-capsule"
                        element={<MyCapsuleInsurance />}
                    />
                    <Route path="/my-history" element={<InsuranceHistory />} />

                    {/* Extra Flows */}
                    <Route
                        path="/capsule-cancel"
                        element={<CapsuleCancelPage />}
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
