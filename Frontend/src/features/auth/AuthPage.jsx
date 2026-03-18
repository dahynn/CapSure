import React from 'react';
import { Outlet } from 'react-router-dom';

// 레이아웃은 각 하위 페이지(LoginPage, SignupPage 등)에서 직접 담당
const AuthPage = () => {
    return <Outlet />;
};

export default AuthPage;
