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

        </div>
    );
};

export default LoginPage;
