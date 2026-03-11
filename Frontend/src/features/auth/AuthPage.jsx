import React from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import logo from '@/assets/logo.png';

const AuthPage = () => {
    const navigate = useNavigate();

    return (
        <div className="min-h-screen bg-slate-50 p-4 sm:p-8 flex justify-center items-center">
            {/* 
              가로 사이즈 제한을 넓히고 (max-w-4xl), 
              전체 화면을 유동적으로 쓸 수 있도록 레이아웃 구성.
              Login과 Signup 모두 이 틀 안에서 렌더링 됩니다.
            */}
            <div className="w-full max-w-4xl bg-white rounded-3xl shadow-2xl relative overflow-hidden flex flex-col md:flex-row min-h-[600px]">

                {/* 헤더/사이드바 영역 (데스크탑에서는 왼쪽, 모바일에서는 위쪽) 공통 적용 */}
                <div className="bg-primary-600 p-8 flex flex-col items-center justify-center md:w-1/3 relative shrink-0">
                    <div className="absolute top-0 left-0 w-full h-full opacity-10 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')]"></div>
                    <img src={logo} alt="CapsuleCover Logo" className="w-16 h-16 object-cover mb-4 relative z-10" />
                    <h1 className="text-3xl font-black text-white tracking-tight relative z-10 text-center">CapsuleCover</h1>
                    <p className="text-primary-100 mt-2 text-sm relative z-10 text-center">캡슐처럼 조립하는 나만의 보험</p>

                    {/* 현재 경로에 따라 로그인/회원가입 전환 버튼 표시 가능성을 위해 하위 라우트로 분리된 상태에서는 약간의 트릭이 필요하지만, 여기서는 단순화하여 안내 문구만 두거나 생략할 수 있습니다. Outlet에서 렌더링되는 페이지 내에서 네비게이션을 처리하도록 하였습니다. */}
                </div>

                {/* 콘텐츠/폼 영역 (Outlet을 통해 LoginPage 또는 SignupPage 렌더링) */}
                <div className="p-8 flex-1 w-full bg-white flex flex-col justify-center">
                    <Outlet />
                </div>
            </div>
        </div>
    );
};

export default AuthPage;
