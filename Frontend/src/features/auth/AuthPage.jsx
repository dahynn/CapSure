import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert, ArrowRight, UserPlus, LogIn, ChevronRight, CheckCircle2 } from 'lucide-react';

const AuthPage = () => {
    const navigate = useNavigate();
    const [view, setView] = useState('onboarding'); // onboarding, login, signup

    const handleOAuthLogin = (provider) => {
        // Dummy OAuth login
        console.log(`Logging in with ${provider}`);
        navigate('/home');
    };

    const handleGuestLogin = () => {
        navigate('/home');
    };

    const handleSignup = (e) => {
        e.preventDefault();
        // Dummy signup
        setView('signup-success');
        setTimeout(() => {
            navigate('/home');
        }, 1500);
    };

    return (
        <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
            <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden glass-panel relative">

                {/* Header Area */}
                <div className="bg-primary-600 p-8 text-center relative overflow-hidden">
                    <div className="absolute top-0 left-0 w-full h-full opacity-10 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')]"></div>
                    <ShieldAlert className="w-16 h-16 text-white mx-auto mb-4 relative z-10" />
                    <h1 className="text-3xl font-black text-white tracking-tight relative z-10">BlockCover</h1>
                    <p className="text-primary-100 mt-2 text-sm relative z-10">블록으로 조립하는 나만의 보험</p>
                </div>

                {/* Content Area */}
                <div className="p-8">

                    {/* View: Onboarding */}
                    {view === 'onboarding' && (
                        <div className="space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
                            <button
                                onClick={() => setView('login')}
                                className="w-full flex items-center justify-between p-4 rounded-xl border-2 border-slate-100 hover:border-primary-500 bg-white hover:bg-slate-50 transition-all group"
                            >
                                <div className="flex items-center gap-4">
                                    <div className="p-2 bg-primary-50 text-primary-600 rounded-lg group-hover:bg-primary-100 transition-colors">
                                        <LogIn className="w-6 h-6" />
                                    </div>
                                    <div className="text-left">
                                        <h3 className="font-bold text-slate-800">기존 회원 로그인</h3>
                                        <p className="text-sm text-slate-500">소셜 계정으로 빠르게 로그인하세요</p>
                                    </div>
                                </div>
                                <ChevronRight className="w-5 h-5 text-slate-300 group-hover:text-primary-500 transition-colors" />
                            </button>

                            <button
                                onClick={() => setView('signup')}
                                className="w-full flex items-center justify-between p-4 rounded-xl border-2 border-slate-100 hover:border-success-500 bg-white hover:bg-slate-50 transition-all group"
                            >
                                <div className="flex items-center gap-4">
                                    <div className="p-2 bg-success-50 text-success-600 rounded-lg group-hover:bg-success-100 transition-colors">
                                        <UserPlus className="w-6 h-6" />
                                    </div>
                                    <div className="text-left">
                                        <h3 className="font-bold text-slate-800">신규 가입하기</h3>
                                        <p className="text-sm text-slate-500">간단한 정보 입력으로 시작하세요</p>
                                    </div>
                                </div>
                                <ChevronRight className="w-5 h-5 text-slate-300 group-hover:text-success-500 transition-colors" />
                            </button>
                        </div>
                    )}

                    {/* View: Login */}
                    {view === 'login' && (
                        <div className="space-y-4 animate-in fade-in slide-in-from-right-8 duration-300">
                            <div className="text-center mb-6">
                                <h2 className="text-xl font-bold text-slate-800">로그인</h2>
                                <p className="text-sm text-slate-500 mt-1">이용할 방식을 선택해주세요</p>
                            </div>

                            <button
                                onClick={() => handleOAuthLogin('Kakao')}
                                className="w-full py-4 rounded-xl font-bold text-[#391B1B] bg-[#FEE500] hover:bg-[#E5CD00] transition-colors flex justify-center items-center gap-2"
                            >
                                카카오로 시작하기
                            </button>

                            <button
                                onClick={() => handleOAuthLogin('Google')}
                                className="w-full py-4 rounded-xl font-bold text-slate-600 bg-white border border-slate-200 hover:bg-slate-50 transition-colors flex justify-center items-center gap-2"
                            >
                                Google로 시작하기
                            </button>

                            <div className="relative py-4">
                                <div className="absolute inset-0 flex items-center">
                                    <div className="w-full border-t border-slate-200"></div>
                                </div>
                                <div className="relative flex justify-center text-sm">
                                    <span className="px-2 bg-white text-slate-400">또는</span>
                                </div>
                            </div>

                            <button
                                onClick={handleGuestLogin}
                                className="w-full py-4 rounded-xl font-bold text-white bg-slate-800 hover:bg-slate-900 transition-colors flex justify-center items-center"
                            >
                                게스트로 둘러보기 (테스트용)
                            </button>

                            <button
                                onClick={() => setView('onboarding')}
                                className="w-full mt-4 text-sm text-slate-500 hover:text-slate-800 font-medium transition-colors"
                            >
                                이전으로 돌아가기
                            </button>
                        </div>
                    )}

                    {/* View: Signup */}
                    {view === 'signup' && (
                        <div className="animate-in fade-in slide-in-from-right-8 duration-300">
                            <div className="text-center mb-6">
                                <h2 className="text-xl font-bold text-slate-800">기본 정보 입력</h2>
                                <p className="text-sm text-slate-500 mt-1">서비스 이용을 위한 정보를 입력해주세요.</p>
                            </div>

                            <form onSubmit={handleSignup} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1">이름</label>
                                    <input
                                        type="text"
                                        required
                                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                        placeholder="홍길동"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1">주민번호 앞 7자리</label>
                                    <div className="flex items-center gap-2">
                                        <input
                                            type="text"
                                            required
                                            maxLength="6"
                                            className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium text-center tracking-widest text-lg"
                                            placeholder="YYMMDD"
                                        />
                                        <span className="text-slate-400 font-bold">-</span>
                                        <input
                                            type="text"
                                            required
                                            maxLength="1"
                                            className="w-16 px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium text-center text-lg"
                                            placeholder="*"
                                        />
                                        <span className="text-slate-400 tracking-widest">******</span>
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1">휴대폰 번호</label>
                                    <div className="flex gap-2">
                                        <input
                                            type="tel"
                                            required
                                            className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                            placeholder="010-0000-0000"
                                        />
                                        <button type="button" className="px-4 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl whitespace-nowrap hover:bg-slate-200 transition-colors">
                                            인증요청
                                        </button>
                                    </div>
                                </div>

                                <div className="pt-4">
                                    <button
                                        type="submit"
                                        className="w-full py-4 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 shadow-lg shadow-primary-200 transition-all flex justify-center items-center gap-2 group"
                                    >
                                        가입 완료하기
                                        <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                                    </button>
                                </div>
                            </form>

                            <button
                                onClick={() => setView('onboarding')}
                                className="w-full mt-4 text-sm text-slate-500 hover:text-slate-800 font-medium transition-colors"
                            >
                                이전으로 돌아가기
                            </button>
                        </div>
                    )}

                    {/* View: Signup Success */}
                    {view === 'signup-success' && (
                        <div className="text-center py-8 animate-in zoom-in-95 duration-300">
                            <CheckCircle2 className="w-20 h-20 text-success-500 mx-auto mb-4 animate-bounce" />
                            <h2 className="text-2xl font-bold text-slate-800 mb-2">가입 완료!</h2>
                            <p className="text-slate-500">홈 화면으로 이동합니다...</p>
                        </div>
                    )}

                </div>
            </div>
        </div>
    );
};

export default AuthPage;
