import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth.api';
import logo from '@/assets/logo.png';

const LoginPage = () => {
    const navigate = useNavigate();
    const [loginForm, setLoginForm] = useState({ email: '', password: '' });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleChange = (e) =>
        setLoginForm({ ...loginForm, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await authApi.login(loginForm);
            const { isFirstLogin } = await authApi.checkFirstLogin();
            navigate(isFirstLogin ? '/onboarding' : '/home');
        } catch {
            setError('이메일 또는 비밀번호를 확인해주세요.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex flex-col items-center justify-between px-6 py-10"
            style={{ backgroundColor: 'var(--color-bg)' }}>

            {/* 상단 로고 영역 */}
            <div className="flex-1 flex flex-col items-center justify-center w-full max-w-sm pt-8">
                {/* 로고 이미지 */}
                <div className="mb-5 relative">
                    <img
                        src={logo}
                        alt="CAPSURE 로고"
                        className="w-24 h-24 object-contain drop-shadow-lg"
                    />
                </div>

                {/* 앱 이름 / 슬로건 */}
                <h1 className="text-3xl font-black tracking-widest text-white mb-1">
                    CAPSURE
                </h1>
                <p className="text-sm mb-10" style={{ color: 'var(--color-brand-gray)' }}>
                    당신의 일상을 지키는 캡슐 보험
                </p>

                {/* 폼 */}
                <form onSubmit={handleSubmit} className="w-full space-y-4">
                    {/* 이메일 */}
                    <div>
                        <label className="block text-sm font-medium text-white mb-2">
                            이메일
                        </label>
                        <input
                            type="email"
                            name="email"
                            value={loginForm.email}
                            onChange={handleChange}
                            required
                            placeholder="이메일 주소를 입력해 주세요"
                            className="w-full px-4 py-4 rounded-2xl text-white placeholder-gray-500 text-sm outline-none transition-all
                                focus:ring-2"
                            style={{
                                backgroundColor: '#131B2E',
                                border: '1px solid #1E2A40',
                            }}
                            onFocus={e => e.target.style.boxShadow = '0 0 0 2px var(--color-brand-blue)'}
                            onBlur={e => e.target.style.boxShadow = 'none'}
                        />
                    </div>

                    {/* 비밀번호 */}
                    <div>
                        <label className="block text-sm font-medium text-white mb-2">
                            비밀번호
                        </label>
                        <input
                            type="password"
                            name="password"
                            value={loginForm.password}
                            onChange={handleChange}
                            required
                            placeholder="••••••••"
                            className="w-full px-4 py-4 rounded-2xl text-white placeholder-gray-500 text-sm outline-none transition-all"
                            style={{
                                backgroundColor: '#131B2E',
                                border: '1px solid #1E2A40',
                            }}
                            onFocus={e => e.target.style.boxShadow = '0 0 0 2px var(--color-brand-blue)'}
                            onBlur={e => e.target.style.boxShadow = 'none'}
                        />
                    </div>

                    {/* 비밀번호 찾기 */}
                    <div className="flex justify-end">
                        <button
                            type="button"
                            className="text-sm font-medium transition-opacity hover:opacity-70"
                            style={{ color: 'var(--color-brand-blue)' }}
                        >
                            Find Password
                        </button>
                    </div>

                    {/* 에러 메시지 */}
                    {error && (
                        <p className="text-xs text-red-400 text-center -mt-1">{error}</p>
                    )}

                    {/* 로그인 버튼 */}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95 disabled:opacity-60"
                        style={{
                            backgroundColor: 'var(--color-brand-blue)',
                            color: '#020715',
                        }}
                    >
                        {loading ? '로그인 중...' : '로그인'}
                    </button>
                </form>

                {/* 회원가입 유도 */}
                <p className="mt-6 text-sm" style={{ color: 'var(--color-brand-gray)' }}>
                    계정이 없으신가요?{' '}
                    <button
                        onClick={() => navigate('/signup')}
                        className="font-bold transition-opacity hover:opacity-70"
                        style={{ color: 'var(--color-brand-blue)' }}
                    >
                        회원가입
                    </button>
                </p>
            </div>

            {/* 하단 소셜 로그인 */}
            <div className="w-full max-w-sm pb-4">
                {/* 구분선 */}
                <div className="flex items-center gap-3 mb-5">
                    <div className="flex-1 h-px" style={{ backgroundColor: '#1E2A40' }} />
                    <span className="text-xs" style={{ color: 'var(--color-brand-gray)' }}>
                        또는 다음 계정으로 로그인
                    </span>
                    <div className="flex-1 h-px" style={{ backgroundColor: '#1E2A40' }} />
                </div>

                {/* 소셜 버튼들 */}
                <div className="grid grid-cols-2 gap-3">
                    {/* 구글 */}
                    <button
                        type="button"
                        className="flex items-center justify-center gap-2 py-3 rounded-2xl font-medium text-sm text-white transition-all hover:brightness-110 active:scale-95"
                        style={{ backgroundColor: '#131B2E', border: '1px solid #1E2A40' }}
                    >
                        {/* Google G 아이콘 */}
                        <svg width="18" height="18" viewBox="0 0 24 24">
                            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
                            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                        </svg>
                        구글
                    </button>

                    {/* 애플 */}
                    <button
                        type="button"
                        className="flex items-center justify-center gap-2 py-3 rounded-2xl font-medium text-sm text-white transition-all hover:brightness-110 active:scale-95"
                        style={{ backgroundColor: '#131B2E', border: '1px solid #1E2A40' }}
                    >
                        {/* Apple 아이콘 */}
                        <svg width="16" height="18" viewBox="0 0 814 1000" fill="white">
                            <path d="M788.1 340.9c-5.8 4.5-108.2 62.2-108.2 190.5 0 148.4 130.3 200.9 134.2 202.2-.6 3.2-20.7 71.9-68.7 141.9-42.8 61.6-87.5 123.1-155.5 123.1s-85.5-39.5-164-39.5c-76.5 0-103.7 40.8-165.9 40.8s-105-57.8-155.5-127.4C46 790.7 0 663 0 541.8c0-207.7 134.7-317.9 266.8-317.9 99.8 0 183.3 65.3 244.7 65.3 56.7 0 151.9-69 265.3-69 43.4 0 170.8 4 250.5 109.9zm-285.5-181.3c60.1-71.3 90.8-157.7 90.8-244 0-12.7-.6-25.4-1.9-35.4-86.4 3.2-188.6 57.8-252 132.1-56.7 65.3-99.2 151.6-99.2 238 0 11.5 1.9 23 2.5 26.7 5.7.6 15.3 1.9 24.8 1.9 76.5 0 171.4-51.2 234.7-119.9z"/>
                        </svg>
                        애플
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;
