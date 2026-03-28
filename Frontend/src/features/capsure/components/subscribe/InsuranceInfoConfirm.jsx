import React from 'react';
import {
    ArrowLeft,
    ArrowRight,
    Banknote,
    CalendarDays,
    CircleHelp,
    CreditCard,
    Cross,
    HeartPulse,
    Info,
    Pencil,
    Shield,
    ShieldAlert,
} from 'lucide-react';

const CATEGORY_STYLES = {
    실손: {
        chip: 'bg-[#1A3B50] text-[#82D8FC]',
        iconBg: 'bg-[#0E2538]',
        icon: Cross,
    },
    암: {
        chip: 'bg-[#3B2447] text-[#F2BEF7]',
        iconBg: 'bg-[#291A37]',
        icon: ShieldAlert,
    },
    '뇌/심장': {
        chip: 'bg-[#3A331C] text-[#F6CD3C]',
        iconBg: 'bg-[#2A2516]',
        icon: HeartPulse,
    },
    상해: {
        chip: 'bg-[#3A2534] text-[#FFB4C8]',
        iconBg: 'bg-[#2A1B26]',
        icon: ShieldAlert,
    },
    default: {
        chip: 'bg-[#2A3345] text-[#A7B6D8]',
        iconBg: 'bg-[#1C2434]',
        icon: Shield,
    },
};

const formatDate = (date) => {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}.${month}.${day}`;
};

const toWholeWon = (value) => Math.round(Number(value ?? 0));
const formatWon = (value) => `${toWholeWon(value).toLocaleString()}원`;

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
            <header className="sticky top-0 z-50 flex items-center p-4 border-b border-slate-800/70 bg-[#020715] min-h-[56px]">
                <button onClick={onPrev} className="p-2 text-white hover:bg-slate-800 rounded-full transition-colors">
                    <ArrowLeft className="w-6 h-6" />
                </button>
                <h1 className="text-base font-bold text-white absolute left-1/2 -translate-x-1/2">결제 정보 확인</h1>
                <button className="text-slate-400 ml-auto p-2">
                    <CircleHelp className="w-6 h-6" />
                </button>
            </header>

            <section className="px-6 pt-7">
                <h2 className="text-lg font-black text-white leading-snug">결제 전 보험 정보 확인</h2>
                <p className="mt-2 text-xs text-slate-400">선택하신 상품과 총 결제 금액을 확인해주세요.</p>
            </section>

            <section className="mx-6 mt-8 rounded-3xl border border-slate-800 bg-[radial-gradient(circle_at_85%_10%,rgba(130,216,252,0.12),transparent_50%),#0C1628] px-6 py-6 shadow-sm">
                <div className="flex items-start gap-4">
                    <div className="w-16 h-16 rounded-2xl bg-[#82D8FC] text-[#020715] flex items-center justify-center shadow-[0_6px_22px_rgba(130,216,252,0.45)]">
                        <Shield className="w-8 h-8" />
                    </div>
                    <div className="flex-1">
                        <label htmlFor="capsule-name" className="mt-2 flex items-center gap-2 text-xl font-black text-white">
                            <input
                                id="capsule-name"
                                value={capsuleName}
                                onChange={(event) => onCapsuleNameChange(event.target.value)}
                                maxLength={20}
                                placeholder="캡슐 이름을 입력해주세요"
                                className="w-full bg-transparent border-none outline-none p-0 placeholder:text-slate-500"
                            />
                            <Pencil className="w-5 h-5 text-slate-400 flex-shrink-0" />
                        </label>
                    </div>
                </div>

                <div className="mt-8 text-right">
                    <p className="text-[#9D9DA4] text-sm font-semibold">총 월 보험료</p>
                    <p className="mt-1 text-3xl leading-none font-black text-[#82D8FC] tracking-tight">
                        {formatWon(totalPremium)}
                    </p>
                </div>

                <div className="mt-6 flex justify-end">
                    <div className="inline-flex items-center gap-2 rounded-full border border-slate-700 bg-[#111B2D] px-3 py-1.5 text-[#C7D3EA] text-[13px]">
                        <CalendarDays className="w-4 h-4" />
                        <span>{formatDate(today)} ~ {formatDate(nextMonth)}</span>
                    </div>
                </div>
            </section>

            <section className="px-6 mt-8">
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-base font-bold text-white">결제 수단</h3>
                </div>
                <article className="rounded-3xl border border-slate-800 bg-[#0C1628] px-5 py-5">
                    {paymentLoading ? (
                        <p className="text-sm text-slate-400">등록된 결제수단을 불러오는 중...</p>
                    ) : paymentMethod ? (
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded-2xl bg-[#1C2434] flex items-center justify-center text-[#82D8FC]">
                                {paymentMethod.methodType === 'BANK_ACCOUNT' ? (
                                    <Banknote className="w-6 h-6" />
                                ) : (
                                    <CreditCard className="w-6 h-6" />
                                )}
                            </div>
                            <div className="min-w-0">
                                <p className="text-base font-bold text-white truncate">
                                    {paymentMethod.provider} {paymentMethod.methodType === 'BANK_ACCOUNT' ? '계좌' : '카드'}
                                </p>
                                <p className="text-sm text-[#9D9DA4] mt-1">{paymentMethod.maskedNumber}</p>
                            </div>
                        </div>
                    ) : (
                        <p className="text-sm text-slate-400">
                            {paymentError || '등록된 결제수단이 없습니다. 마이페이지에서 먼저 등록해주세요.'}
                        </p>
                    )}
                </article>
            </section>

            <section className="px-6 mt-8">
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-base font-bold text-white">구성 상품 리스트</h3>
                    <p className="text-xs text-slate-400">{selectedProducts.length}개 상품 포함</p>
                </div>

                <div className="space-y-4">
                    {selectedProducts.map((product) => {
                        const style = CATEGORY_STYLES[product.categoryLabel] ?? CATEGORY_STYLES.default;
                        const Icon = style.icon;

                        return (
                            <article
                                key={product.productSourceId}
                                className="rounded-3xl border border-slate-800 bg-[#0C1628] px-5 py-5"
                            >
                                <div className="flex items-center gap-4">
                                    <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${style.iconBg}`}>
                                        <Icon className="w-7 h-7 text-[#82D8FC]" />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-start justify-between gap-3">
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-center gap-2 mb-1">
                                                    <span className={`text-xs leading-none font-black rounded-md px-2 py-1 ${style.chip}`}>
                                                        {product.categoryLabel}
                                                    </span>
                                                </div>
                                                <h4 className="text-base leading-tight font-bold break-keep">{product.productName}</h4>
                                                <p className="text-xs text-[#9D9DA4] mt-1">{product.companyName}</p>
                                            </div>
                                            <p className="text-xl leading-tight font-black text-right">
                                                {formatWon(product.monthlyPrice)}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </article>
                        );
                    })}
                </div>
            </section>

            <section className="px-6 mt-8">
                <div className="rounded-3xl border border-slate-800 bg-[#071226] px-5 py-4 flex items-start gap-3 text-sm leading-relaxed text-[#B9C7DF]">
                    <Info className="w-5 h-5 mt-1 text-[#82D8FC] flex-shrink-0" />
                    <p>
                        구독 시작 시 매월 정해진 날짜에 자동 결제가 진행됩니다. 보험 효력은 결제 완료 시점부터 발생하며,
                        상세 약관은 등록된 메일로 즉시 발송됩니다.
                    </p>
                </div>
            </section>

            <div className="fixed app-fixed-cta left-0 right-0 max-w-[560px] mx-auto px-6 z-40">
                <button
                    onClick={onNext}
                    disabled={!canSubmit}
                    className={`w-full rounded-2xl py-3.5 text-[15px] font-bold transition-all ${
                        canSubmit
                            ? 'bg-[#82D8FC] text-[#020715] border-2 border-[#D9F2FF] shadow-[0_0_0_2px_#1D5EC7] hover:opacity-95'
                            : 'bg-slate-800 text-slate-500 cursor-not-allowed'
                    }`}
                >
                    <span className="relative inline-flex items-center gap-2">
                        <span className="absolute left-0 top-[1px] text-[#0b2746]/25 select-none">
                            {isSubmitting ? '결제 처리 중...' : '이름 등록 및 결제하기'}
                        </span>
                        <span className="relative">{isSubmitting ? '결제 처리 중...' : '이름 등록 및 결제하기'}</span>
                        <ArrowRight className="w-5 h-5 relative" strokeWidth={3} />
                    </span>
                </button>
            </div>
        </div>
    );
};

export default InsuranceInfoConfirm;
