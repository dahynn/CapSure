import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth.api';
import { Eye, EyeOff } from 'lucide-react';

const CustomSelect = ({ value, onChange, options, placeholder, dropUp }) => {
    const [isOpen, setIsOpen] = useState(false);
    const ref = React.useRef(null);

    React.useEffect(() => {
        const handleClickOutside = (e) => {
            if (ref.current && !ref.current.contains(e.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    React.useEffect(() => {
        if (isOpen && value && ref.current) {
            const selectedEl = ref.current.querySelector('[data-selected="true"]');
            if (selectedEl) {
                selectedEl.scrollIntoView({ block: 'center' });
            }
        }
    }, [isOpen, value]);

    return (
        <div className="relative flex-1" ref={ref}>
            <button
                type="button"
                className={`w-full flex items-center justify-between px-3 py-4 rounded-2xl text-sm transition-all focus:outline-none bg-white ${!value ? 'text-slate-400' : 'text-slate-900'}`}
                onClick={() => setIsOpen(!isOpen)}
                style={{ boxShadow: isOpen ? '0 0 0 2px var(--color-brand-blue)' : 'none' }}
            >
                <span>{value ? value + placeholder : placeholder}</span>
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none" className={`transition-transform duration-200 flex-shrink-0 ml-1 ${isOpen ? 'rotate-180' : ''}`}>
                    <path d="M2.5 4.5L6 8L9.5 4.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
            </button>
            
            {isOpen && (
                <div 
                    className={`absolute z-50 w-full left-0 bg-white border border-slate-100 rounded-2xl shadow-2xl overflow-y-auto custom-scrollbar ${dropUp ? 'bottom-full mb-2' : 'top-full mt-2'}`}
                    style={{ maxHeight: '200px' }}
                >
                    <div className="py-2">
                        {options.map((opt) => (
                            <button
                                type="button"
                                key={opt}
                                data-selected={value == opt}
                                className={`w-full text-center px-2 py-3 text-sm transition-colors ${value == opt ? 'font-bold bg-blue-50/50' : 'text-slate-700 hover:bg-slate-50'}`}
                                style={value == opt ? { color: 'var(--color-brand-blue)' } : {}}
                                onClick={() => {
                                    onChange(opt);
                                    setIsOpen(false);
                                }}
                            >
                                {opt}{placeholder}
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
};

const SignupPage = () => {
    const navigate = useNavigate();
    const [view, setView] = useState('signup'); // signup, signup-success

    const [signupForm, setSignupForm] = useState({
        email: '',
        password: '',
        passwordConfirm: '',
        phone: '',
        gender: '', // 'M' | 'F'
    });
    
    const [birthYear, setBirthYear] = useState('');
    const [birthMonth, setBirthMonth] = useState('');
    const [birthDay, setBirthDay] = useState('');

    const [agreed, setAgreed] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);

    const handleChange = (e) => setSignupForm({ ...signupForm, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        
        if (!agreed) {
            setError('서비스 약관 및 개인정보 처리방침에 동의해주세요.');
            return;
        }
        if (signupForm.password !== signupForm.passwordConfirm) {
            setError('비밀번호가 일치하지 않습니다.');
            return;
        }

        if (!birthYear || !birthMonth || !birthDay) {
            setError('생년월일을 모두 선택해주세요.');
            return;
        }

        setLoading(true);
        try {
            const birthDate = `${birthYear}-${String(birthMonth).padStart(2, '0')}-${String(birthDay).padStart(2, '0')}`;
            
            await authApi.signup({
                ...signupForm,
                birthDate,
                fullName: '사용자', // 와이어프레임에 이름 필드가 없어서 기본값 처리
            });
            setView('signup-success');
            setTimeout(() => {
                navigate('/login');
            }, 1500);
        } catch (err) {
            setError('회원가입에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const currentYear = new Date().getFullYear();
    const years = Array.from({ length: 100 }, (_, i) => currentYear - i);
    const months = Array.from({ length: 12 }, (_, i) => i + 1);
    const days = Array.from({ length: 31 }, (_, i) => i + 1);

    if (view === 'signup-success') {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center px-6" style={{ backgroundColor: 'var(--color-bg)' }}>
                {/* 성공 아이콘 */}
                <svg className="w-20 h-20 text-success-500 mb-6 animate-bounce" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <h2 className="text-2xl font-bold text-white mb-2">가입 완료!</h2>
                <p className="text-sm" style={{ color: 'var(--color-brand-gray)' }}>로그인 화면으로 이동합니다...</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col items-center px-6 py-6" style={{ backgroundColor: 'var(--color-bg)' }}>
            <div className="w-full max-w-sm flex-1 flex flex-col">
                
                {/* 상단 뒤로가기 (아이콘 대체) */}
                <button 
                    onClick={() => navigate(-1)} 
                    className="p-2 -ml-2 mb-4 w-fit text-white hover:opacity-70 transition-opacity"
                >
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="19" y1="12" x2="5" y2="12"></line>
                        <polyline points="12 19 5 12 12 5"></polyline>
                    </svg>
                </button>

                {/* 헤더 타이틀 */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-white mb-2">계정 만들기</h1>
                    <p className="text-sm" style={{ color: 'var(--color-brand-gray)' }}>
                        안전한 보장을 위한 첫 걸음을 시작하세요.
                    </p>
                </div>

                {/* 입력 폼 */}
                <form onSubmit={handleSubmit} className="flex-1 flex flex-col space-y-4">
                    
                    {/* 이메일 주소 */}
                    <div>
                        <label className="block text-sm font-medium text-white mb-2">이메일 주소</label>
                        <input
                            type="email" name="email"
                            value={signupForm.email} onChange={handleChange} required
                            placeholder="name@example.com"
                            className="w-full px-4 py-4 rounded-2xl text-slate-900 placeholder-slate-400 text-sm outline-none bg-white transition-all"
                            onFocus={e => e.target.style.boxShadow = '0 0 0 2px var(--color-brand-blue)'}
                            onBlur={e => e.target.style.boxShadow = 'none'}
                        />
                    </div>

                    {/* 비밀번호 */}
                    <div>
                        <label className="block text-sm font-medium text-white mb-2">비밀번호</label>
                        <div className="relative">
                            <input
                                type={showPassword ? 'text' : 'password'} name="password"
                                value={signupForm.password} onChange={handleChange} required
                                placeholder="••••••••"
                                className="w-full px-4 py-4 pr-12 rounded-2xl text-slate-900 placeholder-slate-400 text-sm outline-none bg-white transition-all"
                                onFocus={e => e.target.style.boxShadow = '0 0 0 2px var(--color-brand-blue)'}
                                onBlur={e => e.target.style.boxShadow = 'none'}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword((prev) => !prev)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-slate-500 hover:text-slate-700 transition-colors"
                                aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                            >
                                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                            </button>
                        </div>
                    </div>

                    {/* 비밀번호 확인 */}
                    <div>
                        <label className="block text-sm font-medium text-white mb-2">비밀번호 확인</label>
                        <div className="relative">
                            <input
                                type={showPasswordConfirm ? 'text' : 'password'} name="passwordConfirm"
                                value={signupForm.passwordConfirm} onChange={handleChange} required
                                placeholder="••••••••"
                                className="w-full px-4 py-4 pr-12 rounded-2xl text-slate-900 placeholder-slate-400 text-sm outline-none bg-white transition-all"
                                onFocus={e => e.target.style.boxShadow = '0 0 0 2px var(--color-brand-blue)'}
                                onBlur={e => e.target.style.boxShadow = 'none'}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPasswordConfirm((prev) => !prev)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-slate-500 hover:text-slate-700 transition-colors"
                                aria-label={showPasswordConfirm ? '비밀번호 숨기기' : '비밀번호 보기'}
                            >
                                {showPasswordConfirm ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                            </button>
                        </div>
                    </div>

                    {/* 전화번호 */}
                    <div>
                        <label className="block text-sm font-medium text-white mb-2">전화번호</label>
                        <input
                            type="tel" name="phone"
                            value={signupForm.phone} onChange={handleChange} required
                            placeholder="010-0000-0000"
                            className="w-full px-4 py-4 rounded-2xl text-slate-900 placeholder-slate-400 text-sm outline-none bg-white transition-all"
                            onFocus={e => e.target.style.boxShadow = '0 0 0 2px var(--color-brand-blue)'}
                            onBlur={e => e.target.style.boxShadow = 'none'}
                        />
                    </div>

                    {/* 성별 (토글) */}
                    <div>
                        <label className="block text-sm font-medium text-white mb-2">성별</label>
                        <div className="flex gap-3">
                            <button
                                type="button"
                                onClick={() => setSignupForm({ ...signupForm, gender: 'M' })}
                                className={`flex-1 py-4 rounded-2xl text-sm font-medium transition-all ${
                                    signupForm.gender === 'M' 
                                    ? 'bg-[#1E2A40] text-white border border-[#38bdf8]' 
                                    : 'bg-[#131B2E] text-slate-300 border border-[#1E2A40]'
                                }`}
                            >
                                남성
                            </button>
                            <button
                                type="button"
                                onClick={() => setSignupForm({ ...signupForm, gender: 'F' })}
                                className={`flex-1 py-4 rounded-2xl text-sm font-medium transition-all ${
                                    signupForm.gender === 'F' 
                                    ? 'bg-[#1E2A40] text-white border border-[#38bdf8]' 
                                    : 'bg-[#131B2E] text-slate-300 border border-[#1E2A40]'
                                }`}
                            >
                                여성
                            </button>
                        </div>
                    </div>

                    {/* 생년월일 */}
                    <div>
                        <label className="block text-sm font-medium text-white mb-2">생년월일</label>
                        <div className="flex gap-2">
                            <CustomSelect value={birthYear} onChange={setBirthYear} options={years} placeholder="년" />
                            <CustomSelect value={birthMonth} onChange={setBirthMonth} options={months} placeholder="월" />
                            <CustomSelect value={birthDay} onChange={setBirthDay} options={days} placeholder="일" />
                        </div>
                    </div>

                    {/* 약관 동의 */}
                    <div className="flex items-center gap-3 pt-4">
                        <button
                            type="button"
                            onClick={() => setAgreed(!agreed)}
                            className="w-5 h-5 rounded border flex items-center justify-center transition-all bg-[#131B2E] border-[#1E2A40]"
                            style={agreed ? { backgroundColor: 'var(--color-brand-blue)', borderColor: 'var(--color-brand-blue)' } : {}}
                        >
                            {agreed && (
                                <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="#020715" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <polyline points="2.5 6 5 8.5 9.5 3.5"></polyline>
                                </svg>
                            )}
                        </button>
                        <p className="text-sm" style={{ color: 'var(--color-brand-gray)' }}>
                            <span className="text-white underline underline-offset-2 decoration-slate-400 cursor-pointer hover:text-white">서비스 약관</span>
                            {' '}및{' '}
                            <span className="text-white underline underline-offset-2 decoration-slate-400 cursor-pointer hover:text-white">개인정보 처리방침</span>
                            에 동의합니다.
                        </p>
                    </div>

                    {error && (
                        <p className="text-xs text-red-400 mt-2">{error}</p>
                    )}

                    {/* 하단 영역 (버튼 및 로그인 유도) - 여백으로 밀어내기 위해 margin-top auto 활용 */}
                    <div className="pt-8 mt-auto pb-4">
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95 disabled:opacity-60 mb-6 text-slate-900"
                            style={{ backgroundColor: 'var(--color-brand-purple)' }}
                        >
                            {loading ? '처리 중...' : '회원가입'}
                        </button>

                        <p className="text-center text-sm" style={{ color: 'var(--color-brand-gray)' }}>
                            이미 계정이 있으신가요?{' '}
                            <button
                                type="button"
                                onClick={() => navigate('/login')}
                                className="font-bold transition-opacity hover:opacity-70"
                                style={{ color: 'var(--color-brand-blue)' }}
                            >
                                로그인
                            </button>
                        </p>

                        <p className="text-center mt-12 text-xs" style={{ color: '#1E2A40' }}>
                            신뢰와 보안이 보장됩니다
                        </p>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default SignupPage;
