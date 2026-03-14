import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth.api';
import logo from '@/assets/logo.png';

const LoginPage = () => {
    const navigate = useNavigate();
    const [loginForm, setLoginForm] = useState({ email: '', password: '' });

    const handleLoginChange = (e) => setLoginForm({ ...loginForm, [e.target.name]: e.target.value });

    const handleLoginSubmit = async (e) => {
        e.preventDefault();
        try {
            await authApi.login(loginForm);
            
            // 로그인 완료 후 온보딩 대상인지 확인
            const { isFirstLogin } = await authApi.checkFirstLogin();
            if (isFirstLogin) {
                navigate('/onboarding');
            } else {
                navigate('/home');
            }
        } catch (error) {
            alert('로그인에 실패했습니다.');
        }
    };

    return (
        <div className="space-y-6 animate-in fade-in slide-in-from-right-8 duration-300 w-full max-w-sm mx-auto">
            <div className="text-center mb-6">
                <h2 className="text-xl font-bold text-slate-800">로그인</h2>
                <p className="text-sm text-slate-500 mt-1">서비스 이용을 위해 로그인해주세요</p>
            </div>

            <form onSubmit={handleLoginSubmit} className="space-y-4">
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1">이메일</label>
                    <input
                        type="email" name="email"
                        required value={loginForm.email} onChange={handleLoginChange}
                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                        placeholder="이메일 입력"
                    />
                </div>
                <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1">비밀번호</label>
                    <input
                        type="password" name="password"
                        required value={loginForm.password} onChange={handleLoginChange}
                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                        placeholder="비밀번호 입력"
                    />
                </div>

                <button
                    type="submit"
                    className="w-full py-4 mt-2 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 transition-all flex justify-center items-center"
                >
                    로그인
                </button>
            </form>

            <div className="flex items-center justify-center gap-4 text-sm font-medium text-slate-500 pt-4">
                <button type="button" className="hover:text-slate-800 transition-colors">비밀번호 찾기</button>
                <div className="w-px h-3 bg-slate-300"></div>
                <button type="button" onClick={() => navigate('/signup')} className="hover:text-primary-600 transition-colors">회원가입</button>
            </div>
        </div>
    );
};

export default LoginPage;
