import React, { useState, useEffect } from 'react';
import { 
    Pill, 
    CreditCard, 
    ReceiptText, 
    Megaphone, 
    Headphones, 
    LogOut, 
    ChevronRight,
    ChevronLeft,
    CheckCircle2,
    CircleAlert,
    Landmark,
    ShieldCheck
} from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { getPaymentHistory, getCurrentPaymentMethod, getMyCapsules, registerPaymentMethod } from './api/mypage.api';
import { getLatestCapsureSubscription } from '@/features/capsure/utils/capsuleStorage';

const MyPage = ({ initialView = 'main' }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const [view, setView] = useState(initialView);
    const [history, setHistory] = useState([]);
    const [showToast, setShowToast] = useState(false);
    const [paymentMethod, setPaymentMethod] = useState(null);
    const [paymentLoading, setPaymentLoading] = useState(false);
    const [paymentSaving, setPaymentSaving] = useState(false);
    const [paymentError, setPaymentError] = useState('');
    const [capsules, setCapsules] = useState([]);
    const [capsulesLoading, setCapsulesLoading] = useState(false);
    const [paymentForm, setPaymentForm] = useState({
        provider: 'TOSS',
        methodType: 'BANK_ACCOUNT',
        maskedNumber: '',
    });
    
    const [user, setUser] = useState({
        name: '고객',
        email: 'capsure_user@email.com',
        subscriptionCount: 0
    });

    useEffect(() => {
        import('./api/mypage.api')
            .then(({ getUserProfile }) => getUserProfile())
            .then(data => {
                if (data) setUser(prev => ({ ...prev, name: data.name || '고객', email: data.email || prev.email }));
            })
            .catch(() => {});
    }, []);

    useEffect(() => {
        const mergeLatestCapsule = (baseCapsules = []) => {
            const latestCapsule = getLatestCapsureSubscription();
            if (!latestCapsule?.subscriptionId) {
                return baseCapsules;
            }

            const latestCapsuleItem = {
                id: latestCapsule.subscriptionId,
                subscriptionId: latestCapsule.subscriptionId,
                title: latestCapsule.capsuleName?.trim() || '나만의 캡슐',
            };

            return [
                latestCapsuleItem,
                ...baseCapsules.filter(
                    (capsule) =>
                        String(capsule.subscriptionId ?? capsule.id) !== String(latestCapsule.subscriptionId)
                ),
            ];
        };

        const fetchCapsules = async () => {
            setCapsulesLoading(true);

            try {
                const capsuleSummaries = await getMyCapsules();
                const mappedCapsules = (capsuleSummaries ?? []).map((capsule, index) => ({
                    id: capsule.subscriptionId ?? `capsule-${index}`,
                    subscriptionId: capsule.subscriptionId,
                    title: capsule.capsuleName?.trim() || '나만의 캡슐',
                }));

                setCapsules(mappedCapsules);
                setUser(prev => ({ ...prev, subscriptionCount: mappedCapsules.length }));
            } catch (error) {
                const latestCapsule = getLatestCapsureSubscription();
                if (latestCapsule?.subscriptionId) {
                    const fallbackCapsules = [{
                        id: latestCapsule.subscriptionId,
                        subscriptionId: latestCapsule.subscriptionId,
                        title: latestCapsule.capsuleName?.trim() || '?ì„Žì­”??ï§¦â‰ªë’“',
                    }];
                    setCapsules(fallbackCapsules);
                    setUser(prev => ({ ...prev, subscriptionCount: fallbackCapsules.length }));
                } else {
                    setCapsules([]);
                    setUser(prev => ({ ...prev, subscriptionCount: 0 }));
                }
            } finally {
                setCapsulesLoading(false);
            }
        };

        fetchCapsules();
    }, []);

    useEffect(() => {
        setView(initialView);
    }, [initialView]);

    useEffect(() => {
        if (location.state?.profileUpdated) {
            setShowToast(true);
            window.history.replaceState({}, document.title);
            const timer = setTimeout(() => setShowToast(false), 2500);
            return () => clearTimeout(timer);
        }
    }, [location]);

    useEffect(() => {
        if (view === 'history') {
            const fetchHistory = async () => {
                const data = await getPaymentHistory();
                setHistory(data);
            };
            fetchHistory();
        }
    }, [view]);

    useEffect(() => {
        if (view !== 'payment') {
            return;
        }

        const fetchPaymentMethod = async () => {
            setPaymentLoading(true);
            setPaymentError('');
            try {
                const data = await getCurrentPaymentMethod();
                setPaymentMethod(data);
                if (data) {
                    setPaymentForm({
                        provider: data.provider || 'TOSS',
                        methodType: data.methodType || 'BANK_ACCOUNT',
                        maskedNumber: data.maskedNumber || '',
                    });
                }
            } catch (error) {
                console.error('Payment method load error', error);
                setPaymentError('결제수단 정보를 불러오지 못했습니다.');
            } finally {
                setPaymentLoading(false);
            }
        };

        fetchPaymentMethod();
    }, [view]);

    const getMethodLabel = (methodType) => {
        if (methodType === 'BANK_ACCOUNT') return '계좌';
        if (methodType === 'CARD') return '카드';
        return methodType || '결제수단';
    };

    const handlePaymentFormChange = (key, value) => {
        setPaymentForm(prev => ({ ...prev, [key]: value }));
    };

    const showSavedToast = () => {
        setShowToast(true);
        window.setTimeout(() => setShowToast(false), 2500);
    };

    const handlePaymentSave = async () => {
        if (paymentSaving) {
            return;
        }

        const trimmedMaskedNumber = paymentForm.maskedNumber.trim();
        if (!trimmedMaskedNumber) {
            setPaymentError('카드 또는 계좌 정보를 입력해 주세요.');
            return;
        }

        setPaymentSaving(true);
        setPaymentError('');
        try {
            await registerPaymentMethod({
                provider: paymentForm.provider,
                methodType: paymentForm.methodType,
                maskedNumber: trimmedMaskedNumber,
            });

            const savedPaymentMethod = {
                provider: paymentForm.provider,
                methodType: paymentForm.methodType,
                maskedNumber: trimmedMaskedNumber,
                active: true,
            };

            setPaymentMethod(savedPaymentMethod);
            setPaymentForm({
                provider: savedPaymentMethod.provider,
                methodType: savedPaymentMethod.methodType,
                maskedNumber: savedPaymentMethod.maskedNumber,
            });
            showSavedToast();
        } catch (error) {
            console.error('Payment method save error', error);
            setPaymentError('결제수단 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.');
        } finally {
            setPaymentSaving(false);
        }
    };

    const renderMain = () => (
        <div className="flex flex-col animate-in fade-in slide-in-from-bottom-4 duration-500 space-y-12">
            {/* Header */}
            <div className="flex justify-between items-center mb-0">
                <div></div> {/* Spacer */}
            </div>

            {/* Profile Section */}
            <div className="flex justify-between items-center mb-0">
                <div>
                    <h2 className="text-[28px] md:text-3xl font-bold text-white leading-tight tracking-tight mb-1">{user.name} 님</h2>
                    <p className="text-[#9D9DA4] text-sm">{user.email}</p>
                </div>
                <button 
                    onClick={() => navigate('/mypage/edit')}
                    className="px-4 py-2 bg-[#1C212E] hover:bg-[#2A3142] rounded-xl text-sm font-medium text-white transition-colors"
                >
                    프로필 수정
                </button>
            </div>

            {/* Subscription Info Section */}
            <div className="mb-10">
                <h3 className="text-[#9D9DA4] text-sm font-medium mb-4">나의 구독 정보</h3>
                <button 
                    onClick={() => navigate('/mypage/capsure')}
                    className="w-full flex items-center justify-between p-5 bg-[#141925] hover:bg-[#1E2535] rounded-[24px] transition-all group border border-slate-800/30"
                >
                    <div className="flex items-center gap-4">
                        <div className="w-10 h-10 bg-[#1C212E] rounded-full flex items-center justify-center text-[#82D8FC]">
                            <Pill className="w-6 h-6" />
                        </div>
                        <span className="font-bold text-lg text-white">나의 구독 캡슐</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <span className="text-[#82D8FC] font-medium">{user.subscriptionCount}개 사용 중</span>
                        <ChevronRight className="w-5 h-5 text-[#4E5669] group-hover:translate-x-1 transition-transform" />
                    </div>
                </button>
            </div>

            {/* Account Settings Section */}
            <div className="flex-1">
                <h3 className="text-[#9D9DA4] text-sm font-medium mb-4">계정 설정</h3>
                <div className="space-y-3">
                    {[
                        { icon: CreditCard, label: '결제 수단 관리', id: 'payment' },
                        { icon: ReceiptText, label: '구독 내역', id: 'history' },
                        { icon: Megaphone, label: '공지사항', id: 'notice' },
                        { icon: Headphones, label: '고객센터', id: 'support' },
                    ].map((item, idx) => {
                        const Icon = item.icon;
                        return (
                            <button 
                                key={idx}
                                onClick={() => {
                                    if (item.id === 'history') navigate('/mypage/history');
                                    if (item.id === 'payment') navigate('/mypage/payment-methods');
                                }}
                                className="w-full flex items-center justify-between p-5 bg-[#141925] hover:bg-[#1E2535] rounded-[24px] transition-all group border border-slate-800/30"
                            >
                                <div className="flex items-center gap-4">
                                    <div className="flex items-center justify-center text-[#82D8FC]">
                                        <Icon className="w-6 h-6" />
                                    </div>
                                    <span className="font-bold text-lg text-white">{item.label}</span>
                                </div>
                                <ChevronRight className="w-5 h-5 text-[#4E5669] group-hover:translate-x-1 transition-transform" />
                            </button>
                        );
                    })}
                </div>
            </div>

            {/* Logout Button */}
            <div className="mt-12 mb-8 flex justify-center">
                <button className="flex items-center gap-2 text-[#9D9DA4] hover:text-white transition-colors py-2 px-4 rounded-lg">
                    <LogOut className="w-5 h-5" />
                    <span className="font-medium">로그아웃</span>
                </button>
            </div>
        </div>
    );

    const renderPayment = () => (
        <div className="flex flex-col animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="flex items-center gap-4 mb-8">
                <button onClick={() => navigate('/mypage')} className="p-2 hover:bg-slate-800 rounded-full transition-colors text-white">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-2xl font-bold text-white">결제 수단 관리</h1>
            </div>

            <div className="space-y-5">
                <div className="p-5 bg-[#141925] rounded-[24px] border border-slate-800/30">
                    <div className="flex items-start justify-between gap-4">
                        <div className="flex items-start gap-4">
                            <div className="w-11 h-11 rounded-2xl bg-[#1C212E] text-[#82D8FC] flex items-center justify-center">
                                <CreditCard className="w-5 h-5" />
                            </div>
                            <div>
                                <p className="text-sm text-[#9D9DA4] mb-1">현재 연결된 결제수단</p>
                                {paymentLoading ? (
                                    <p className="text-white font-bold">불러오는 중...</p>
                                ) : paymentMethod ? (
                                    <>
                                        <p className="text-white font-bold text-lg">{paymentMethod.provider} {getMethodLabel(paymentMethod.methodType)}</p>
                                        <p className="text-[#9D9DA4] text-sm mt-1">{paymentMethod.maskedNumber}</p>
                                    </>
                                ) : (
                                    <p className="text-white font-bold">등록된 결제수단이 없습니다</p>
                                )}
                            </div>
                        </div>
                        {paymentMethod ? (
                            <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-500/10 text-emerald-300 text-xs font-bold">
                                <ShieldCheck className="w-4 h-4" />
                                활성
                            </div>
                        ) : (
                            <div className="px-3 py-1.5 rounded-full bg-slate-700/40 text-slate-300 text-xs font-bold">
                                미등록
                            </div>
                        )}
                    </div>
                </div>

                <div className="p-6 bg-[#141925] rounded-[24px] border border-slate-800/30 space-y-5">
                    <div>
                        <h2 className="text-white text-lg font-bold">결제수단 등록 및 변경</h2>
                        <p className="text-[#9D9DA4] text-sm mt-1">마스킹된 카드번호 또는 계좌번호 형식으로 입력해 주세요.</p>
                    </div>

                    <div className="grid grid-cols-1 gap-4">
                        <label className="space-y-2">
                            <span className="text-sm text-[#9D9DA4] font-medium">결제사</span>
                            <select
                                value={paymentForm.provider}
                                onChange={(e) => handlePaymentFormChange('provider', e.target.value)}
                                className="w-full rounded-2xl bg-[#1C212E] border border-slate-700 px-4 py-3 text-white outline-none focus:border-[#82D8FC]"
                            >
                                <option value="TOSS">토스페이</option>
                                <option value="KAKAO_PAY">카카오페이</option>
                                <option value="NAVER_PAY">네이버페이</option>
                            </select>
                        </label>

                        <label className="space-y-2">
                            <span className="text-sm text-[#9D9DA4] font-medium">수단 유형</span>
                            <div className="grid grid-cols-2 gap-3">
                                <button
                                    type="button"
                                    onClick={() => handlePaymentFormChange('methodType', 'BANK_ACCOUNT')}
                                    className={`rounded-2xl border px-4 py-3 text-sm font-bold transition-colors ${paymentForm.methodType === 'BANK_ACCOUNT' ? 'border-[#82D8FC] bg-[#82D8FC]/10 text-[#82D8FC]' : 'border-slate-700 bg-[#1C212E] text-white'}`}
                                >
                                    <span className="flex items-center justify-center gap-2">
                                        <Landmark className="w-4 h-4" />
                                        계좌
                                    </span>
                                </button>
                                <button
                                    type="button"
                                    onClick={() => handlePaymentFormChange('methodType', 'CARD')}
                                    className={`rounded-2xl border px-4 py-3 text-sm font-bold transition-colors ${paymentForm.methodType === 'CARD' ? 'border-[#82D8FC] bg-[#82D8FC]/10 text-[#82D8FC]' : 'border-slate-700 bg-[#1C212E] text-white'}`}
                                >
                                    <span className="flex items-center justify-center gap-2">
                                        <CreditCard className="w-4 h-4" />
                                        카드
                                    </span>
                                </button>
                            </div>
                        </label>

                        <label className="space-y-2">
                            <span className="text-sm text-[#9D9DA4] font-medium">{paymentForm.methodType === 'CARD' ? '카드 번호' : '계좌 번호'}</span>
                            <input
                                value={paymentForm.maskedNumber}
                                onChange={(e) => handlePaymentFormChange('maskedNumber', e.target.value)}
                                placeholder={paymentForm.methodType === 'CARD' ? '예: 1234-****-****-5678' : '예: 신한 ****-****-1234'}
                                className="w-full rounded-2xl bg-[#1C212E] border border-slate-700 px-4 py-3 text-white placeholder:text-slate-500 outline-none focus:border-[#82D8FC]"
                            />
                        </label>
                    </div>

                    {paymentError && (
                        <div className="flex items-center gap-2 rounded-2xl border border-rose-400/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                            <CircleAlert className="w-4 h-4 shrink-0" />
                            <span>{paymentError}</span>
                        </div>
                    )}

                    <button
                        type="button"
                        onClick={handlePaymentSave}
                        disabled={paymentSaving}
                        className="w-full rounded-2xl bg-[#82D8FC] hover:bg-[#6fcaef] disabled:bg-[#82D8FC]/50 text-[#09111D] font-black py-4 transition-colors"
                    >
                        {paymentSaving ? '저장 중...' : paymentMethod ? '결제수단 변경하기' : '결제수단 등록하기'}
                    </button>
                </div>
            </div>
        </div>
    );

    const renderHistory = () => (
        <div className="flex flex-col animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="flex items-center gap-4 mb-8">
                <button onClick={() => navigate('/mypage')} className="p-2 hover:bg-slate-800 rounded-full transition-colors text-white">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-2xl font-bold text-white">구독 내역</h1>
            </div>

            <div className="space-y-4">
                {history.length > 0 ? history.map((item) => (
                    <div key={item.id} className="p-5 bg-[#141925] rounded-[24px] border border-slate-800/30">
                        <div className="flex justify-between items-start mb-2">
                            <span className="text-[#82D8FC] text-xs font-bold px-2 py-1 bg-[#82D8FC]/10 rounded-md">{item.status}</span>
                            <span className="text-[#9D9DA4] text-xs">{item.date}</span>
                        </div>
                        <h4 className="text-white font-bold text-lg mb-1">{item.items[0].name} {item.items.length > 1 && `외 ${item.items.length-1}건`}</h4>
                        <div className="flex justify-between items-end">
                            <p className="text-[#9D9DA4] text-sm">{item.items[0].company}</p>
                            <p className="text-white font-black text-xl">{item.totalAmount.toLocaleString()}원</p>
                        </div>
                    </div>
                )) : (
                    <div className="py-20 text-center text-[#9D9DA4]">내역이 없습니다.</div>
                )}
            </div>
        </div>
    );

    const renderCapsule = () => (
        <div className="flex flex-col animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="flex items-center gap-4 mb-8">
                <button onClick={() => navigate('/mypage')} className="p-2 hover:bg-slate-800 rounded-full transition-colors text-white">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-2xl font-bold text-white">나의 캡슐</h1>
            </div>
            
            <div className="p-8 bg-[#141925] rounded-[32px] border border-slate-800/30 text-center">
                <div className="w-20 h-20 bg-[#1C212E] rounded-full flex items-center justify-center text-[#82D8FC] mx-auto mb-6">
                    <Pill className="w-10 h-10" />
                </div>
                <h3 className="text-white text-xl font-bold mb-2">현재 {capsules.length}개의 캡슐 사용 중</h3>
                <p className="text-[#9D9DA4] text-sm mb-8">각 캡슐을 클릭하여 상세 보장 내용을 확인하세요.</p>
                
                <div className="grid grid-cols-3 gap-4">
                    {Array.from({ length: 9 }).map((_, index) => {
                        const capsule = capsules[index];

                        if (!capsule) {
                            return (
                                <div
                                    key={`empty-${index}`}
                                    className="aspect-square bg-[#141925]/50 rounded-2xl border border-slate-800/20"
                                />
                            );
                        }

                        return (
                            <button
                                key={capsule.subscriptionId ?? capsule.id}
                                onClick={() => navigate(`/mypage/capsule/${capsule.subscriptionId ?? capsule.id}`)}
                                className="aspect-square bg-[#1C212E] hover:bg-slate-800 transition-colors rounded-2xl flex flex-col items-center justify-center gap-3 border border-slate-700/50 cursor-pointer shadow-lg hover:shadow-cyan-900/20 px-3 py-4"
                                title={capsule.title}
                            >
                                <div className="w-9 h-9 rounded-full bg-[#82D8FC]/20 text-[#82D8FC] flex items-center justify-center shrink-0">
                                    <Pill className="w-4 h-4" />
                                </div>
                                <span className="text-white text-[12px] leading-[1.35] font-semibold break-keep line-clamp-2">
                                    {capsule.title}
                                </span>
                            </button>
                        );
                    })}
                </div>

                {!capsulesLoading && capsules.length === 0 && (
                    <p className="text-[#9D9DA4] text-sm mt-6">아직 사용 중인 캡슐이 없습니다.</p>
                )}
            </div>
        </div>
    );

    return (
        <div className="px-8 py-8 md:px-12 md:py-10 space-y-12 max-w-[560px] mx-auto w-full transition-all min-h-screen relative">
            {/* Toast Notification */}
            {showToast && (
                <div className="fixed top-8 left-1/2 -translate-x-1/2 z-50 animate-in slide-in-from-top-4 fade-in duration-300">
                    <div className="bg-[#1C212E]/90 backdrop-blur-md text-white px-5 py-3 rounded-full shadow-2xl border border-slate-700 flex items-center gap-3">
                        <CheckCircle2 className="w-5 h-5 text-[#82D8FC]" />
                        <span className="text-sm font-bold whitespace-nowrap">
                            {view === 'payment' ? '결제수단이 성공적으로 저장되었습니다' : '프로필이 성공적으로 수정되었습니다'}
                        </span>
                    </div>
                </div>
            )}
            
            {view === 'main' && renderMain()}
            {view === 'history' && renderHistory()}
            {view === 'capsule' && renderCapsule()}
            {view === 'payment' && renderPayment()}
        </div>
    );
};

export default MyPage;
