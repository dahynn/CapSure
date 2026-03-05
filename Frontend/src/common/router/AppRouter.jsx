import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '@/layouts/MainLayout';
import AuthPage from '@/features/auth/AuthPage';
import HomePage from '@/features/home/HomePage';
import BlockInsurancePage from '@/features/block/BlockInsurancePage';
import DailyInsurancePage from '@/features/daily/DailyInsurancePage';

const AppRouter = () => {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<AuthPage />} />

                {/* Main Layout Pages */}
                <Route element={<MainLayout />}>
                    <Route path="/home" element={<HomePage />} />
                    <Route path="/block-insurance" element={<BlockInsurancePage />} />
                    <Route path="/daily-insurance" element={<DailyInsurancePage />} />
                    <Route path="/" element={<Navigate to="/home" replace />} />
                </Route>
            </Routes>
        </BrowserRouter>
    );
};

export default AppRouter;
