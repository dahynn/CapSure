import React from 'react';
import { CalendarDays, Info, RefreshCw, Shield } from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';
import logoImg from '@/assets/logo.png';

const formatWon = (value) => `${Math.round(Number(value ?? 0)).toLocaleString()}원`;

const formatDate = (date) => {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}.${month}.${day}`;
};

const SubscribeComplete = ({ selectedProducts, totalPremium, capsuleName, onNext }) => {
    const today = React.useMemo(() => new Date(), []);
    const nextMonth = React.useMemo(() => {
        const next = new Date(today);
        next.setDate(next.getDate() + 30);
        return next;
    }, [today]);

    return (
        <div
            className="min-h-screen bg-[#020715] text-white animate-in fade-in slide-in-from-bottom-4"
            style={{ paddingBottom: 'calc(var(--app-bottom-nav-height) + env(safe-area-inset-bottom) + 180px)' }}
        >
            <section className="px-6 pt-12 text-center">
                <h2 className="text-[30px] leading-[1.18] tracking-[-0.025em] font-bold text-[#E7EDF8] break-keep">
                    결제가 완료되었습니다
                </h2>
                <p className="mt-3 text-base leading-relaxed text-[#A8B4C6]">보험 캡슐이 성공적으로 활성화되었습니다.</p>
            </section>

            <section className="px-6 mt-6">
                <article className="relative overflow-hidden rounded-[34px] bg-[#1A2231] px-7 py-8 shadow-[inset_0_1px_0_rgba(255,255,255,0.025)]">
                    <div className="pointer-events-none absolute inset-y-0 right-0 w-[42%] bg-gradient-to-l from-[#202838]/70 to-transparent" />
                    <img
                        src={logoImg}
                        alt=""
                        aria-hidden="true"
                        className="absolute right-7 top-8 w-[38px] h-[38px] object-contain pointer-events-none select-none"
                        style={{
                            opacity: 0.12,
                            filter: 'blur(0.8px) saturate(0.9)',
                        }}
                    />

                    <div className="relative z-10 max-w-[74%]">
                        <p className="text-[12px] font-bold tracking-[0.24em] text-[#83CEFB] uppercase">CAPSULE NAME</p>
                        <h3 className="mt-4 text-[34px] leading-[1.1] tracking-[-0.03em] font-semibold text-[#EDF2FB] break-keep">
                            {capsuleName}
                        </h3>
                        <div className="mt-16">
                            <p className="text-[#A4ADBC] text-[14px] font-medium">월 보험료</p>
                            <div className="mt-3 flex items-end gap-3">
                                <p className="text-[52px] leading-none tracking-[-0.04em] font-bold text-brand-blue">
                                    {Math.round(Number(totalPremium ?? 0)).toLocaleString()}
                                </p>
                                <span className="text-[20px] leading-none font-medium text-white/70 mb-1">원</span>
                            </div>
                        </div>
                    </div>
                    <div className="pointer-events-none absolute inset-x-0 bottom-0 h-20 bg-gradient-to-t from-[#141C2A]/65 to-transparent" />
                </article>
            </section>

            <section className="px-6 mt-4 grid grid-cols-2 gap-3">
                <article className="rounded-3xl bg-[#0D1526] px-4 py-4">
                    <CalendarDays className="w-5 h-5 text-brand-blue mb-3" />
                    <p className="text-[11px] text-slate-400 font-semibold">구독 기간</p>
                    <p className="mt-2 text-lg leading-snug font-semibold">{formatDate(today)} ~ {formatDate(nextMonth)}</p>
                </article>
                <article className="rounded-3xl bg-[#0D1526] px-4 py-4">
                    <RefreshCw className="w-5 h-5 text-brand-yellow mb-3" />
                    <p className="text-[11px] text-slate-400 font-semibold">갱신 유형</p>
                    <p className="mt-2 text-lg font-semibold">자동 갱신 대기</p>
                    <p className="mt-1 text-sm text-slate-400">만료일 3일 전 안내</p>
                </article>
            </section>

            <section className="px-6 mt-4">
                <article className="rounded-3xl bg-[#0A111F] px-5 py-5">
                    <div className="flex items-center gap-3 mb-2">
                        <div className="w-10 h-10 rounded-xl bg-[#15243D] flex items-center justify-center">
                            <Info className="w-5 h-5 text-brand-blue" />
                        </div>
                        <h4 className="text-xl leading-none font-semibold">안내 사항</h4>
                    </div>
                    <p className="text-sm leading-relaxed text-[#B7C3D8]">
                        본 캡슐은 한 달 뒤 자동으로 만료되도록 설정되어 있습니다.
                        구독 유지를 원하실 경우 자동 갱신 옵션을 활성화해 주세요.
                    </p>
                </article>
            </section>

            <section className="px-6 mt-4">
                <div className="rounded-3xl bg-[#0D1526]/60 px-5 py-4 flex items-start gap-3 text-sm leading-relaxed text-[#B9C7DF]">
                    <Shield className="w-5 h-5 mt-1 text-brand-blue flex-shrink-0" />
                    <p>
                        구독 시작 시 매월 정해진 날짜에 자동 결제가 진행됩니다.
                        <br />
                        보험 효력은 결제 완료 시점부터 발생하며, 상세 약관은 등록된 메일로 즉시 발송됩니다.
                    </p>
                </div>
            </section>

            <section className="px-6 mt-4">
                <h3 className="text-base font-semibold mb-3">상세 구성</h3>
                <div className="space-y-3">
                    {selectedProducts.map((item, idx) => (
                        <article
                            key={`${item.productName}-${idx}`}
                            className="rounded-2xl bg-[#0D1526] px-4 py-3.5 flex items-end justify-between gap-3"
                        >
                            <div className="min-w-0">
                                <p className="text-base font-semibold break-keep">{item.productName}</p>
                                <p className="text-xs text-slate-400 mt-1 truncate">{item.companyName}</p>
                            </div>
                            <p className="text-xl font-semibold shrink-0">{formatWon(item.monthlyPrice)}</p>
                        </article>
                    ))}
                </div>
            </section>

            <div className="fixed app-fixed-cta left-0 right-0 max-w-[560px] mx-auto px-6 z-40">
                <AppButton onClick={onNext}>홈으로 이동</AppButton>
            </div>
        </div>
    );
};

export default SubscribeComplete;
