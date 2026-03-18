import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, CheckCircle2, CheckSquare, Square } from 'lucide-react';
import { authApi } from '../api/auth.api';
import TextModal from '@/common/components/ui/modal/TextModal';

const SignupPage = () => {
    const navigate = useNavigate();
    const [view, setView] = useState('signup'); // signup, signup-success

    // Form States
    const [signupForm, setSignupForm] = useState({
        email: '', password: '', passwordConfirm: '',
        name: '', gender: '', birthDate: '', phone: ''
    });

    const [isEmailChecked, setIsEmailChecked] = useState(false);

    const handleSignupChange = (e) => setSignupForm({ ...signupForm, [e.target.name]: e.target.value });

    const handleCheckEmail = async () => {
        if (!signupForm.email) return alert('이메일을 입력해주세요.');
        try {
            const res = await authApi.checkEmail(signupForm.email);
            setIsEmailChecked(true);
            alert(res.message);
        } catch (error) {
            alert('중복 확인에 실패했습니다.');
        }
    };

    const handleSignupSubmit = async (e) => {
        e.preventDefault();
        if (!isEmailChecked) return alert('이메일 중복 확인을 해주세요.');
        if (signupForm.password !== signupForm.passwordConfirm) return alert('비밀번호가 일치하지 않습니다.');

        try {
            await authApi.signup(signupForm);
            setView('signup-success');
            setTimeout(() => {
                navigate('/login');
            }, 1500);
        } catch (error) {
            alert('회원가입에 실패했습니다.');
        }
    };



    return (
        <>
            {/* View: Signup */}
            {view === 'signup' && (
                <div className="animate-in fade-in slide-in-from-right-8 duration-300 w-full">
                    <div className="text-left mb-8">
                        <h2 className="text-2xl font-bold text-slate-800">회원가입</h2>
                        <p className="text-sm text-slate-500 mt-1">간단한 정보 입력으로 시작하세요</p>
                    </div>

                    <form onSubmit={handleSignupSubmit} className="space-y-6">
                        {/* 1열 레이아웃 적용 */}
                        <div className="grid grid-cols-1 gap-6">
                            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                                <label className="w-full sm:w-32 text-sm font-semibold text-slate-700 shrink-0">
                                    이메일 <span className="text-red-500">*</span>
                                </label>
                                <div className="flex flex-1 gap-2">
                                    <input
                                        type="email" name="email" required
                                        value={signupForm.email} onChange={handleSignupChange}
                                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                        placeholder="example@email.com"
                                    />
                                    <button
                                        type="button"
                                        onClick={handleCheckEmail}
                                        className="px-4 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl whitespace-nowrap hover:bg-slate-200 transition-colors"
                                    >
                                        중복 확인
                                    </button>
                                </div>
                            </div>

                            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                                <label className="w-full sm:w-32 text-sm font-semibold text-slate-700 shrink-0">
                                    비밀번호 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="password" name="password" required
                                    value={signupForm.password} onChange={handleSignupChange}
                                    className="flex-1 px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                    placeholder="비밀번호 입력"
                                />
                            </div>

                            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                                <label className="w-full sm:w-32 text-sm font-semibold text-slate-700 shrink-0">
                                    비밀번호 확인 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="password" name="passwordConfirm" required
                                    value={signupForm.passwordConfirm} onChange={handleSignupChange}
                                    className="flex-1 px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                    placeholder="비밀번호 재입력"
                                />
                            </div>

                            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                                <label className="w-full sm:w-32 text-sm font-semibold text-slate-700 shrink-0">
                                    이름 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="text" name="name" required
                                    value={signupForm.name} onChange={handleSignupChange}
                                    className="flex-1 px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                    placeholder="홍길동"
                                />
                            </div>

                            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                                <label className="w-full sm:w-32 text-sm font-semibold text-slate-700 shrink-0">
                                    성별 <span className="text-red-500">*</span>
                                </label>
                                <select
                                    name="gender" required
                                    value={signupForm.gender} onChange={handleSignupChange}
                                    className="flex-1 px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium bg-white"
                                >
                                    <option value="" disabled>성별 선택</option>
                                    <option value="male">남성</option>
                                    <option value="female">여성</option>
                                </select>
                            </div>

                            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                                <label className="w-full sm:w-32 text-sm font-semibold text-slate-700 shrink-0">
                                    생년월일 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="date" name="birthDate" required
                                    value={signupForm.birthDate} onChange={handleSignupChange}
                                    className="flex-1 px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                />
                            </div>

                            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                                <label className="w-full sm:w-32 text-sm font-semibold text-slate-700 shrink-0">
                                    휴대폰 번호 <span className="text-red-500">*</span>
                                </label>
                                <div className="flex flex-1 gap-2">
                                    <input
                                        type="tel" name="phone" required
                                        value={signupForm.phone} onChange={handleSignupChange}
                                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                        placeholder="010-0000-0000"
                                    />
                                    <button type="button" className="px-4 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl whitespace-nowrap hover:bg-slate-200 transition-colors">
                                        인증요청
                                    </button>
                                </div>
                            </div>
                        </div>



                        <div className="pt-6">
                            <button
                                type="submit"
                                className="w-full md:w-auto md:px-12 py-4 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 shadow-lg shadow-primary-200 transition-all flex justify-center items-center gap-2 group mx-auto"
                            >
                                가입 완료하기
                                <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                            </button>
                        </div>
                    </form>

                    {/* Mobile version login link */}
                    <button
                        onClick={() => navigate('/login')}
                        className="w-full mt-6 text-sm text-slate-500 hover:text-slate-800 font-medium transition-colors cursor-pointer md:hidden py-4 border-t border-slate-100"
                    >
                        이미 계정이 있으신가요? 로그인하기
                    </button>
                </div>
            )}

            {/* View: Signup Success */}
            {view === 'signup-success' && (
                <div className="w-full h-full flex flex-col items-center justify-center py-16 animate-in zoom-in-95 duration-300">
                    <CheckCircle2 className="w-24 h-24 text-success-500 mx-auto mb-6 animate-bounce" />
                    <h2 className="text-3xl font-bold text-slate-800 mb-3">가입 완료!</h2>
                    <p className="text-lg text-slate-500">로그인 화면으로 이동합니다...</p>
                </div>
            )}


        </>
    );
};

export default SignupPage;
