import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth.api';
import { Eye, EyeOff } from 'lucide-react';
import logo from '@/assets/logo.png';
import capsureLogo from '@/assets/capsure_logo.png';

const LoginPage = () => {
  const navigate = useNavigate();
  const [loginForm, setLoginForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleChange = (e) => setLoginForm({ ...loginForm, [e.target.name]: e.target.value });

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
    <div
      className="flex min-h-screen flex-col items-center justify-between px-6 py-10"
      style={{ backgroundColor: 'var(--color-bg)' }}
    >
      {/* 상단 로고 영역 */}
      <div className="flex w-full max-w-sm flex-1 flex-col items-center justify-center pt-8">
        {/* 로고 이미지 */}
        <div className="relative mb-5 flex justify-center">
          <img src={logo} alt="CAPSURE 로고" className="h-[120px] w-[120px] object-contain drop-shadow-lg" />
        </div>

        {/* 앱 이름 / 타이포 타이틀 */}
        <h1 className="mb-2 flex w-full justify-center">
          <img src={capsureLogo} alt="CAPSURE" className="h-[28px] object-contain drop-shadow-md" />
        </h1>
        <p className="mb-10 text-sm" style={{ color: 'var(--color-brand-gray)' }}>
          당신의 일상을 지키는 캡슐 보험
        </p>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="w-full space-y-4">
          {/* 이메일 */}
          <div>
            <label className="mb-2 block text-sm font-medium text-white">이메일</label>
            <input
              type="email"
              name="email"
              value={loginForm.email}
              onChange={handleChange}
              required
              placeholder="이메일을 입력해주세요."
              className="w-full rounded-2xl px-4 py-4 text-sm text-white placeholder-gray-500 outline-none transition-all bg-[#131B2E] border border-[#1E2A40] focus:ring-2 focus:ring-[#82D8FC] autofill:!bg-[#131B2E] autofill:!shadow-[inset_0_0_0px_1000px_#131B2E] autofill:[-webkit-text-fill-color:white]"
            />
          </div>

          {/* 비밀번호 */}
          <div>
            <label className="mb-2 block text-sm font-medium text-white">비밀번호</label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                name="password"
                value={loginForm.password}
                onChange={handleChange}
                required
                placeholder="암호를 입력해주세요."
                className="w-full rounded-2xl px-4 py-4 pr-12 text-sm text-white placeholder-gray-500 outline-none transition-all bg-[#131B2E] border border-[#1E2A40] focus:ring-2 focus:ring-[#82D8FC] autofill:!bg-[#131B2E] autofill:!shadow-[inset_0_0_0px_1000px_#131B2E] autofill:[-webkit-text-fill-color:white]"
              />
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-white transition-colors"
                aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
              >
                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {/* 비밀번호 찾기 */}
          <div className="flex justify-end">
            <button
              type="button"
              className="text-sm font-medium transition-opacity hover:opacity-70"
              style={{ color: 'var(--color-brand-blue)' }}
            >
              비밀번호 찾기
            </button>
          </div>

          {/* 에러 메시지 */}
          {error && <p className="-mt-1 text-center text-xs text-red-400">{error}</p>}

          {/* 로그인 버튼 */}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-2xl py-4 text-base font-bold transition-all active:scale-95 disabled:opacity-60"
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
