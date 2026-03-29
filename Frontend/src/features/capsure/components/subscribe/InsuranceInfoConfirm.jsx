import React from 'react';
import {
    ArrowLeft,
    CalendarDays,
    Pencil,
    X,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';

const formatDate = (date) => {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}.${month}.${day}`;
};

const toWholeWon = (value) => Math.round(Number(value ?? 0));
const formatWon = (value) => `${toWholeWon(value).toLocaleString()}원`;

const PROVIDER_KR_MAP = {
    'TOSS BANK': '토스뱅크',
    TOSSBANK: '토스뱅크',
    TOSS: '토스',
    'KAKAO BANK': '카카오뱅크',
    KAKAOBANK: '카카오뱅크',
    KB: 'KB국민',
    'KB BANK': 'KB국민',
    SHINHAN: '신한',
    WOORI: '우리',
    HANA: '하나',
    NH: 'NH농협',
};

const getKoreanProvider = (provider = '') => {
    const trimmed = provider.trim();
    const upper = trimmed.toUpperCase();
    return PROVIDER_KR_MAP[upper] || trimmed;
};

const InsuranceInfoConfirm = ({
    selectedProducts,
    capsuleName,
    onCapsuleNameChange,
    paymentMethod,
    paymentLoading = false,
    paymentError = '',
    onNext,
    onPrev,
    isSubmitting = false,
}) => {
    const totalPremium = selectedProducts.reduce((sum, product) => sum + product.monthlyPrice, 0);
    const canSubmit = capsuleName.trim() && paymentMethod && !paymentLoading && !isSubmitting;

    const today = React.useMemo(() => new Date(), []);
    const nextMonth = React.useMemo(() => {
        const next = new Date(today);
        next.setDate(next.getDate() + 30);
        return next;
    }, [today]);

    return (
        <div className="min-h-screen bg-[#020715] text-white pb-40 animate-in fade-in slide-in-from-bottom-4">
            <header className="sticky top-0 z-50 flex items-center gap-2 p-4 bg-[#020715] min-h-[56px]">
                <button onClick={onPrev} className="p-2 text-white hover:bg-slate-800 rounded-full transition-colors">
                    <ArrowLeft className="w-5 h-5" />
                </button>
                <h1 className="text-base font-semibold text-white absolute left-1/2 -translate-x-1/2">결제 정보 확인</h1>
                <button onClick={onPrev} className="text-slate-400 ml-auto p-2 hover:bg-slate-800 rounded-full transition-colors">
                    <X className="w-5 h-5" />
                </button>
            </header>

            <section className="px-6 pt-8">
                <h2 className="text-xl font-semibold text-white leading-snug break-keep">결제 전 보험 정보를 최종 확인해 주세요</h2>
                <p className="mt-2 text-sm text-slate-400">선택하신 보호 캡슐 구성과 구독 주기를 확인하세요.</p>
                <div className="mt-3 inline-flex items-center gap-2 text-[#F6CD3C]">
                    <span className="w-4 h-4 rounded-full border border-[#F6CD3C] flex items-center justify-center text-[11px] font-bold leading-none">
                        !
                    </span>
                    <p className="text-sm font-medium">캡슐 이름을 등록해주세요</p>
                </div>
            </section>

            <section className="mx-6 mt-7 rounded-3xl border border-slate-700/80 bg-[#0D1526]/45 px-5 py-5">
                <p className="text-xs font-semibold text-[#9DB3D6] mb-2">캡슐 이름</p>
                <div className="rounded-2xl border border-slate-700/80 bg-[#0B1425]/55 px-4 py-3.5">
                    <label htmlFor="capsule-name" className="flex items-center gap-2 text-xl font-semibold text-white leading-none">
                        <input
                            id="capsule-name"
                            value={capsuleName}
                            onChange={(event) => onCapsuleNameChange(event.target.value)}
                            maxLength={20}
                            placeholder="캡슐 이름을 입력해주세요"
                            className="capsule-name-input w-full bg-transparent appearance-none border-0 outline-none ring-0 focus:ring-0 focus:outline-none p-0 text-white font-semibold placeholder:font-medium placeholder:text-slate-500"
                        />
                        <Pencil className="w-5 h-5 text-slate-400 flex-shrink-0" />
                    </label>
                </div>

                <div className="mt-7 text-center">
                    <p className="text-[#9D9DA4] text-xs font-medium">총 월 보험료</p>
                    <p className="mt-2 text-4xl leading-none font-bold text-white tracking-tight">
                        {toWholeWon(totalPremium).toLocaleString()} <span className="text-2xl">원</span>
                    </p>
                </div>

                <div className="mt-6 flex justify-center">
                    <div className="inline-flex items-center gap-2 rounded-full border border-slate-700/80 bg-[#0B1425]/55 px-4 py-2 text-[#C7D3EA] text-[13px] font-medium">
                        <CalendarDays className="w-3.5 h-3.5" />
                        <span>{formatDate(today)} ~ {formatDate(nextMonth)}</span>
                    </div>
                </div>
            </section>

            <section className="px-6 mt-8">
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-base font-semibold text-white">결제 수단</h3>
                </div>
                <article className="rounded-[26px] border border-slate-700/80 bg-[#0D1526]/45 px-5 py-5">
                    {paymentLoading ? (
                        <p className="text-sm text-slate-400">등록된 결제수단을 불러오는 중...</p>
                    ) : paymentMethod ? (
                        <>
                            <div className="min-w-0">
                                <p className="text-xs font-medium text-slate-400">
                                    {paymentMethod.methodType === 'BANK_ACCOUNT' ? '계좌' : '카드'}
                                </p>
                                <p className="text-base font-semibold text-white mt-1">
                                    {getKoreanProvider(paymentMethod.provider)} {paymentMethod.maskedNumber}
                                </p>
                            </div>
                            <div className="h-px bg-slate-800 my-4" />
                            <div className="flex items-center gap-2 text-sm text-[#9db8d8]">
                                <span className="w-2 h-2 rounded-full bg-brand-blue" />
                                생체인증 결제 활성화됨
                            </div>
                        </>
                    ) : (
                        <p className="text-sm text-slate-400">
                            {paymentError || '등록된 결제수단이 없습니다. 마이페이지에서 먼저 등록해주세요.'}
                        </p>
                    )}
                </article>
            </section>

            <section className="px-6 mt-8">
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-base font-semibold text-white">상세 구성</h3>
                    <p className="text-xs text-slate-400">{selectedProducts.length}개 상품</p>
                </div>

                <div className="space-y-3">
                    {selectedProducts.map((product, idx) => (
                        <article
                            key={product.productSourceId ?? `${product.productName}-${idx}`}
                            className="rounded-2xl border border-slate-700/80 bg-[#0D1526]/45 px-4 py-4"
                        >
                            <h4 className="text-base leading-tight font-semibold break-keep">{product.productName}</h4>
                            <div className="mt-2 flex items-end justify-between gap-3">
                                <p className="text-xs text-[#9D9DA4] truncate">{product.companyName}</p>
                                <p className="text-2xl leading-none font-bold text-white tracking-tight shrink-0">
                                    {formatWon(product.monthlyPrice)}
                                </p>
                            </div>
                        </article>
                    ))}
                </div>
            </section>

            <section className="px-6 mt-8">
                <div className="rounded-3xl border border-slate-700/80 bg-transparent px-5 py-4 flex items-start gap-3 text-sm leading-relaxed text-[#B9C7DF]">
                    <p>
                        구독 시작 시 매월 정해진 날짜에 자동 결제가 진행됩니다.
                        <br />
                        보험 효력은 결제 완료 시점부터 발생하며, 상세 약관은 등록된 메일로 즉시 발송됩니다.
                    </p>
                </div>
            </section>

            <div className="fixed app-fixed-cta left-0 right-0 max-w-[560px] mx-auto px-6 z-40">
                <AppButton onClick={onNext} disabled={!canSubmit}>
                    {isSubmitting ? '구독 처리 중...' : '구독하기'}
                </AppButton>
            </div>
        </div>
    );
};

export default InsuranceInfoConfirm;
