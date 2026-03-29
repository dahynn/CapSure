import React, { useEffect } from 'react';
import {
    Building2,
    Phone,
    ShieldCheck,
    Info,
    StickyNote,
    X,
} from 'lucide-react';

const CATEGORY_LABEL_MAP = {
    DEATH: '사망',
    CANCER: '암',
    BRAIN_HEART: '뇌/심장',
    ACTUAL_LOSS: '실손',
    SURGERY: '수술',
    DENTAL: '치아',
    ETC: '기타',
    ACCIDENT: '상해',
    LIABILITY: '배상책임',
};

const formatPrice = (value) => {
    const amount = Number(value || 0);
    return new Intl.NumberFormat('ko-KR').format(amount);
};

const sectorLabel = (sector) => (sector === 'LIFE' ? '생명보험' : '손해보험');

const FieldBlock = ({ label, value, tone = 'default' }) => {
    if (!value) {
        return null;
    }

    return (
        <div className="rounded-2xl border border-slate-800 bg-[#10141D] p-4">
            <p className="text-[12px] font-semibold text-slate-400">{label}</p>
            <p
                className={`mt-2 whitespace-pre-wrap text-[14px] leading-relaxed ${
                    tone === 'accent' ? 'font-bold text-[#82D8FC]' : 'text-white'
                }`}
            >
                {value}
            </p>
        </div>
    );
};

const DashboardProductDetailModal = ({ isOpen, onClose, product, isLoading, errorMessage }) => {
    useEffect(() => {
        if (!isOpen) {
            return undefined;
        }

        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };

        window.addEventListener('keydown', handleEscape);

        return () => {
            document.body.style.overflow = originalOverflow;
            window.removeEventListener('keydown', handleEscape);
        };
    }, [isOpen, onClose]);

    if (!isOpen) {
        return null;
    }

    return (
        <div className="fixed inset-0 z-[70] flex items-end justify-center bg-black/70 backdrop-blur-sm md:items-center">
            <div
                className="absolute inset-0"
                onClick={onClose}
                aria-hidden="true"
            />

            <div className="relative flex max-h-[90vh] w-full max-w-[560px] flex-col overflow-hidden rounded-t-[28px] border border-slate-800 bg-[#020715] shadow-2xl md:rounded-[28px]">
                <div className="flex items-center justify-between border-b border-slate-800 px-5 py-4">
                    <div>
                        <p className="text-[12px] font-semibold uppercase tracking-[0.18em] text-[#82D8FC]">
                            Recommended Detail
                        </p>
                        <h3 className="mt-1 text-[18px] font-bold text-white">추천 상품 상세보기</h3>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-full border border-slate-700 bg-[#121826] p-2 text-slate-300 transition-colors hover:border-slate-500 hover:text-white"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                <div className="overflow-y-auto px-5 py-5">
                    {isLoading ? (
                        <div className="rounded-2xl border border-slate-800 bg-[#10141D] px-4 py-12 text-center text-sm text-slate-300">
                            상품 상세 정보를 불러오는 중입니다.
                        </div>
                    ) : null}

                    {!isLoading && errorMessage ? (
                        <div className="rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-12 text-center text-sm text-red-100">
                            {errorMessage}
                        </div>
                    ) : null}

                    {!isLoading && !errorMessage && product ? (
                        <div className="space-y-4">
                            <div className="rounded-3xl border border-slate-800 bg-[#10141D] p-5">
                                <div className="flex flex-wrap items-center gap-2">
                                    <span className="rounded-full border border-[#294968] bg-[#182F48] px-3 py-1 text-[12px] font-bold text-[#82D8FC]">
                                        {CATEGORY_LABEL_MAP[product.coverageCategoryCode] || '보험'}
                                    </span>
                                    <span className="rounded-full border border-slate-700 bg-[#171E2B] px-3 py-1 text-[12px] font-bold text-slate-300">
                                        {sectorLabel(product.insurerSector)}
                                    </span>
                                    {product.saleChannel ? (
                                        <span className="rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-[12px] font-bold text-emerald-300">
                                            {product.saleChannel}
                                        </span>
                                    ) : null}
                                </div>

                                <div className="mt-4 flex items-center gap-2 text-[13px] font-semibold text-slate-400">
                                    <Building2 className="h-4 w-4" />
                                    <span>{product.companyName || '보험사 정보 없음'}</span>
                                </div>

                                <h4 className="mt-2 text-[24px] font-black leading-tight tracking-tight text-white">
                                    {product.productName}
                                </h4>

                                <div className="mt-4 flex items-end gap-2">
                                    <span className="text-[28px] font-black tracking-tight text-[#82D8FC]">
                                        {formatPrice(product.monthlyPrice)}
                                    </span>
                                    <span className="pb-1 text-[14px] font-semibold text-slate-400">원 / 월</span>
                                </div>
                            </div>

                            <div className="grid gap-4">
                                <FieldBlock
                                    label="보장명"
                                    value={product.coverageName || '보장 정보 없음'}
                                />
                                <FieldBlock
                                    label="보험금 지급 사유"
                                    value={product.claimReason || '지급 사유 정보 없음'}
                                />
                                <FieldBlock
                                    label="보험금 정보"
                                    value={product.payoutAmount || product.joinAmount || '보장 금액 정보 없음'}
                                    tone="accent"
                                />
                                <FieldBlock label="상품 요약" value={product.productSummary} />
                                <FieldBlock label="상품 특징" value={product.productFeature} />
                                <FieldBlock label="특이사항" value={product.specialNote} />
                            </div>

                            {product.contactPhone ? (
                                <div className="rounded-2xl border border-slate-800 bg-[#10141D] p-4">
                                    <div className="flex items-center gap-2 text-[12px] font-semibold text-slate-400">
                                        <Phone className="h-4 w-4" />
                                        <span>문의 전화</span>
                                    </div>
                                    <p className="mt-2 text-[14px] font-bold text-white">{product.contactPhone}</p>
                                </div>
                            ) : null}

                            <div className="rounded-2xl border border-slate-800 bg-[#10141D] p-4">
                                <div className="flex items-start gap-3">
                                    <ShieldCheck className="mt-0.5 h-5 w-5 flex-shrink-0 text-[#82D8FC]" />
                                    <div>
                                        <p className="text-[13px] font-bold text-white">대시보드 전용 읽기 모드</p>
                                        <p className="mt-1 text-[12px] leading-relaxed text-slate-400">
                                            이 상세보기는 추천 상품 정보를 확인하는 용도이며, 캡슐에 담기나 구매 플로우로는 이어지지 않습니다.
                                        </p>
                                    </div>
                                </div>
                            </div>

                            <div className="rounded-2xl border border-slate-800 bg-[#10141D] p-4">
                                <div className="flex items-start gap-3">
                                    <Info className="mt-0.5 h-5 w-5 flex-shrink-0 text-[#F6CD3C]" />
                                    <div>
                                        <p className="text-[13px] font-bold text-white">추가 정보</p>
                                        <p className="mt-1 text-[12px] leading-relaxed text-slate-400">
                                            실제 가입 전에는 약관, 보장 범위, 가입 조건을 다시 확인해 주세요.
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {(product.paymentCycle || product.paymentTerm || product.coverageTerm) ? (
                                <div className="rounded-2xl border border-slate-800 bg-[#10141D] p-4">
                                    <div className="flex items-start gap-3">
                                        <StickyNote className="mt-0.5 h-5 w-5 flex-shrink-0 text-[#F2BEF7]" />
                                        <div className="space-y-1 text-[12px] text-slate-400">
                                            {product.paymentCycle ? <p>납입 주기: {product.paymentCycle}</p> : null}
                                            {product.paymentTerm ? <p>납입 기간: {product.paymentTerm}</p> : null}
                                            {product.coverageTerm ? <p>보장 기간: {product.coverageTerm}</p> : null}
                                        </div>
                                    </div>
                                </div>
                            ) : null}
                        </div>
                    ) : null}
                </div>

                <div className="border-t border-slate-800 px-5 py-4">
                    <button
                        type="button"
                        onClick={onClose}
                        className="w-full rounded-2xl bg-[#82D8FC] px-4 py-3 text-[15px] font-bold text-[#020715] transition-colors hover:bg-[#6CCDF2]"
                    >
                        닫기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DashboardProductDetailModal;
