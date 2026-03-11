import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, CheckCircle2, CheckSquare, Square } from 'lucide-react';
import { authApi } from '../api/auth.api';
import TextModal from '@/common/components/ui/modal/TextModal';

const SignupPage = () => {
    const navigate = useNavigate();
    const [view, setView] = useState('signup'); // signup, signup-success
    const [isMyDataModalOpen, setIsMyDataModalOpen] = useState(false);

    // Form States
    const [signupForm, setSignupForm] = useState({
        name: '', email: '', password: '', passwordConfirm: '',
        phone: '', rrnFront: '', rrnBack: '', address: '', myDataConsent: false
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

    const handleMyDataClick = () => {
        setIsMyDataModalOpen(true);
    };

    const handleMyDataConfirm = () => {
        setSignupForm(prev => ({ ...prev, myDataConsent: true }));
        setIsMyDataModalOpen(false);
    };

    const handleMyDataCancel = () => {
        setIsMyDataModalOpen(false);
    };

    const handleSignupSubmit = async (e) => {
        e.preventDefault();
        if (!isEmailChecked) return alert('이메일 중복 확인을 해주세요.');
        if (signupForm.password !== signupForm.passwordConfirm) return alert('비밀번호가 일치하지 않습니다.');
        if (!signupForm.myDataConsent) return alert('마이데이터 활용 동의가 필요합니다.');

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

    // 더미 약관 텍스트
    const dummyTermsFull = `제 1 조(목적)
본 약관은 CapsuleCover(이하 "회사"라 합니다)가 제공하는 마이데이터 서비스의 이용과 관련하여 회사와 사용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.

    제 2 조(용어의 정의)
1. "마이데이터 서비스"란 사용자의 동의를 기반으로 하여 여러 금융 기관 등에 분산된 정보를 수집, 분석하여 제공하는 서비스를 말합니다.
2. "사용자"란 본 약관에 동의하고 회사가 제공하는 마이데이터 서비스를 이용하는 개인을 말합니다.

    제 3 조(동의의 범위 및 철회)
1. 사용자는 회사가 본인의 개인(신용)정보를 관련 기관으로부터 수집 및 이용하는 것에 동의합니다.
2. 사용자는 언제든지 마이데이터 서비스 설정 메뉴를 통해 기존에 한 동의를 철회할 수 있으며, 이 경우 회사는 즉시 해당 정보의 수집을 중단하고 파기합니다.

    제 4 조(회사의 의무)
1. 회사는 사용자의 정보를 관련 법령에 따라 안전하게 보호해야 할 의무가 있습니다.
2. 회사는 사용자의 동의 범위를 초과하여 정보를 이용하거나 제3자에게 제공할 수 없습니다.

    제 5 조(면책조항)
회사는 천재지변, 파업, 관련 기관의 시스템 오류 등 회사의 통제 범위를 벗어난 사유로 인하여 서비스 제공이 지연되거나 불가능한 경우에는 그에 대한 책임을 지지 않습니다.

    부칙
본 약관은 가입일로부터 효력이 발생합니다.기타 자세한 내용은 홈페이지의 전체 약관을 참조하시기 바랍니다.`;

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
                        {/* 2단 그리드 레이아웃 적용 */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">
                                    이름 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="text" name="name" required
                                    value={signupForm.name} onChange={handleSignupChange}
                                    className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                    placeholder="홍길동"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">
                                    이메일 <span className="text-red-500">*</span>
                                </label>
                                <div className="flex gap-2">
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

                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">
                                    비밀번호 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="password" name="password" required
                                    value={signupForm.password} onChange={handleSignupChange}
                                    className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                    placeholder="비밀번호 입력"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">
                                    비밀번호 확인 <span className="text-red-500">*</span>
                                </label>
                                <input
                                    type="password" name="passwordConfirm" required
                                    value={signupForm.passwordConfirm} onChange={handleSignupChange}
                                    className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                    placeholder="비밀번호 재입력"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">
                                    휴대폰 번호 <span className="text-red-500">*</span>
                                </label>
                                <div className="flex gap-2">
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

                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">
                                    주민등록번호 <span className="text-red-500">*</span>
                                </label>
                                <div className="flex items-center gap-2">
                                    <input
                                        type="text" name="rrnFront" required maxLength="6"
                                        value={signupForm.rrnFront} onChange={handleSignupChange}
                                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium text-center tracking-widest text-lg"
                                        placeholder="YYMMDD"
                                    />
                                    <span className="text-slate-400 font-bold">-</span>
                                    <input
                                        type="text" name="rrnBack" required maxLength="1"
                                        value={signupForm.rrnBack} onChange={handleSignupChange}
                                        className="w-16 px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium text-center text-lg"
                                        placeholder="*"
                                    />
                                    <span className="text-slate-400 tracking-widest">******</span>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">주소 (선택)</label>
                                <input
                                    type="text" name="address"
                                    value={signupForm.address} onChange={handleSignupChange}
                                    className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-200 transition-all font-medium"
                                    placeholder="주소 입력"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-semibold text-slate-700 mb-1">
                                    계좌 본인인증 <span className="text-red-500">*</span>
                                </label>
                                <button type="button" className="w-full px-4 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl hover:bg-slate-200 transition-colors">
                                    1원 송금 본인인증
                                </button>
                            </div>
                        </div>

                        {/* MyData Consent Checkbox */}
                        <div className="pt-2 border-t border-slate-100 mt-6">
                            <label className="flex items-start gap-3 p-4 rounded-xl border border-slate-200 bg-slate-50 cursor-pointer hover:bg-slate-100 transition-colors">
                                <div className="pt-0.5" onClick={(e) => {
                                    e.preventDefault();
                                    if (!signupForm.myDataConsent) handleMyDataClick();
                                    else setSignupForm(prev => ({ ...prev, myDataConsent: false }));
                                }}>
                                    {signupForm.myDataConsent ? (
                                        <CheckSquare className="w-6 h-6 text-primary-600" />
                                    ) : (
                                        <Square className="w-6 h-6 text-slate-400" />
                                    )}
                                </div>
                                <div className="flex-1">
                                    <div onClick={(e) => {
                                        e.preventDefault();
                                        if (!signupForm.myDataConsent) handleMyDataClick();
                                    }}>
                                        <h4 className="font-bold text-slate-800 flex items-center gap-1">
                                            마이데이터 활용 동의 <span className="text-red-500">*</span>
                                        </h4>
                                        <p className="text-sm text-slate-500 mt-1">
                                            서비스의 사용을 위해서는 마이데이터 활용 동의가 필요해요!
                                        </p>
                                    </div>

                                    {/* Scrollable Terms Block */}
                                    <div className="mt-3 p-3 bg-white border border-slate-200 rounded-lg h-32 overflow-y-auto scrollbar-thin text-xs text-slate-500 whitespace-pre-wrap leading-relaxed">
                                        {dummyTermsFull}
                                    </div>
                                </div>
                            </label>
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

            {/* MyData Consent Modal */}
            <TextModal
                isOpen={isMyDataModalOpen}
                onClose={handleMyDataCancel}
                onConfirm={handleMyDataConfirm}
                title="마이데이터 활용 동의"
                contents="마이데이터 활용을 동의하시겠습니까?"
                confirmText="동의합니다"
                cancelText="취소"
            />
        </>
    );
};

export default SignupPage;
