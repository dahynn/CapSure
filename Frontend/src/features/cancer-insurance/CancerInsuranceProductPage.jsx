import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
    AlertCircle,
    CalendarClock,
    Check,
    ChevronLeft,
    FileCheck2,
    FileText,
    Loader2,
    LockKeyhole,
    RefreshCw,
    ShieldCheck,
    Sparkles,
    X,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';
import {
    getCancerProduct,
    getCancerProducts,
    getCancerProductTerms,
    getTermsClause,
    issueCancerQuote,
} from './api/cancerInsurance.api';
import { useCancerInsurance } from './context/CancerInsuranceContext';

const currency = new Intl.NumberFormat('ko-KR');

const formatWon = (value) => `${currency.format(Number(value || 0))}원`;

const hasAccessToken = () => Boolean(
    localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken')
);

const conditionText = (coverage) => {
    const parts = [];
    if (coverage.waitingPeriodDays > 0) {
        parts.push(`면책 ${coverage.waitingPeriodDays}일`);
    }
    if (coverage.reductionPeriodDays > 0) {
        parts.push(`감액 ${coverage.reductionPeriodDays}일`);
    }
    return parts.length > 0 ? parts.join(' · ') : '가입 즉시 보장';
};

const CancerInsuranceProductPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const {
        flowIds,
        updateFlowIds,
        product,
        setProduct,
        terms,
        setTerms,
        quote,
        setQuote,
        setApplication,
        setPayment,
        setPolicy,
    } = useCancerInsurance();
    const [selectedCoverageIds, setSelectedCoverageIds] = useState([]);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [clause, setClause] = useState(null);
    const [clauseLoading, setClauseLoading] = useState(false);
    const resumeAttemptedRef = useRef(false);

    const loadProduct = async () => {
        setLoading(true);
        setError('');
        try {
            const products = await getCancerProducts();
            const selected = products?.[0];
            if (!selected) {
                throw new Error('현재 조회 가능한 암보험 상품이 없습니다.');
            }
            const [detail, termsSummary] = await Promise.all([
                getCancerProduct(selected.productVersionId),
                getCancerProductTerms(selected.productVersionId),
            ]);
            setProduct(detail);
            setTerms(termsSummary);
            setSelectedCoverageIds(detail.coverages.map((item) => item.productCoverageId));
            updateFlowIds({ productVersionId: selected.productVersionId });
        } catch (requestError) {
            setError(requestError.message || '상품 정보를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
        if (!product || !terms) {
            loadProduct();
            return;
        }
        setSelectedCoverageIds(product.coverages.map((item) => item.productCoverageId));
        setLoading(false);
    }, []);

    const selectedCoverages = useMemo(() => (
        product?.coverages?.filter((coverage) => selectedCoverageIds.includes(coverage.productCoverageId)) || []
    ), [product, selectedCoverageIds]);

    const toggleCoverage = (coverageId) => {
        setSelectedCoverageIds((previous) => (
            previous.includes(coverageId)
                ? previous.filter((id) => id !== coverageId)
                : [...previous, coverageId]
        ));
    };

    const openClause = async (termsClauseId) => {
        setClauseLoading(true);
        try {
            setClause(await getTermsClause(termsClauseId));
        } catch (requestError) {
            setError(requestError.message || '약관 조항을 불러오지 못했습니다.');
        } finally {
            setClauseLoading(false);
        }
    };

    const moveToLogin = useCallback(() => {
        navigate('/login', {
            state: {
                returnTo: '/cancer-insurance',
                returnState: { resumeCancerQuote: true },
            },
        });
    }, [navigate]);

    const handleQuote = useCallback(async () => {
        if (!product || selectedCoverageIds.length === 0 || submitting) {
            return;
        }

        if (!hasAccessToken()) {
            moveToLogin();
            return;
        }

        setSubmitting(true);
        setError('');
        try {
            const issued = await issueCancerQuote(
                product.product.productVersionId,
                selectedCoverageIds,
            );
            setApplication(null);
            setPayment(null);
            setPolicy(null);
            setQuote(issued);
            updateFlowIds({
                quoteId: issued.quoteId,
                applicationId: null,
                paymentOrderId: null,
                policyId: null,
            });
            navigate('/cancer-insurance/application');
        } catch (requestError) {
            if ([401, 403].includes(requestError.response?.status)) {
                moveToLogin();
                return;
            }
            setError(requestError.message || '견적을 만들지 못했습니다.');
        } finally {
            setSubmitting(false);
        }
    }, [
        moveToLogin,
        navigate,
        product,
        selectedCoverageIds,
        setApplication,
        setPayment,
        setPolicy,
        setQuote,
        submitting,
        updateFlowIds,
    ]);

    useEffect(() => {
        const shouldResume = location.state?.resumeCancerQuote === true;
        if (
            !shouldResume
            || resumeAttemptedRef.current
            || loading
            || !product
            || selectedCoverageIds.length === 0
            || submitting
        ) {
            return;
        }

        resumeAttemptedRef.current = true;
        navigate('/cancer-insurance', { replace: true, state: null });
        handleQuote();
    }, [
        handleQuote,
        loading,
        location.state,
        navigate,
        product,
        selectedCoverageIds.length,
        submitting,
    ]);

    if (loading) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center gap-4 px-6 text-center">
                <Loader2 className="h-9 w-9 animate-spin text-[#82D8FC]" />
                <div>
                    <p className="font-bold text-white">상품과 약관을 불러오고 있습니다</p>
                    <p className="mt-1 text-sm text-slate-500">판매 버전과 담보 조건을 함께 확인합니다.</p>
                </div>
            </div>
        );
    }

    if (error && !product) {
        return (
            <div className="flex min-h-[520px] flex-col items-center justify-center px-8 text-center">
                <AlertCircle className="h-10 w-10 text-rose-300" />
                <p className="mt-4 font-bold text-white">상품을 불러오지 못했습니다</p>
                <p className="mt-2 text-sm leading-6 text-slate-400">{error}</p>
                <AppButton onClick={loadProduct} fullWidth={false} className="mt-6 px-6">
                    <RefreshCw className="h-4 w-4" /> 다시 시도
                </AppButton>
            </div>
        );
    }

    const summary = product.product;

    return (
        <div className="pb-44">
            <header className="flex items-center px-5 py-5">
                <button
                    type="button"
                    onClick={() => navigate('/home')}
                    className="-ml-2 rounded-full p-2 text-white transition-colors hover:bg-slate-800"
                    aria-label="홈으로 돌아가기"
                >
                    <ChevronLeft className="h-6 w-6" />
                </button>
                <div className="ml-2">
                    <p className="text-xs font-bold text-[#82D8FC]">STEP 1 · 상품과 약관</p>
                    <h1 className="mt-0.5 text-xl font-black text-white">가입할 보장을 확인해요</h1>
                </div>
            </header>

            <main className="space-y-6 px-6">
                <section className="relative overflow-hidden rounded-[30px] border border-[#82D8FC]/25 bg-gradient-to-br from-[#172942] via-[#0E192B] to-[#17152A] p-6 shadow-2xl">
                    <div className="absolute -right-16 -top-20 h-48 w-48 rounded-full bg-[#82D8FC]/15 blur-3xl" />
                    <div className="relative">
                        <div className="flex items-center justify-between gap-3">
                            <span className="rounded-full border border-[#82D8FC]/30 bg-[#82D8FC]/10 px-3 py-1 text-[11px] font-black text-[#82D8FC]">
                                교육용 가상 상품
                            </span>
                            <span className="text-xs font-bold text-slate-400">{summary.version}</span>
                        </div>
                        <div className="mt-7 flex items-start gap-4">
                            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#82D8FC] text-[#020715]">
                                <ShieldCheck className="h-7 w-7" />
                            </span>
                            <div>
                                <p className="text-xs font-bold text-slate-400">{summary.insurerName}</p>
                                <h2 className="mt-1 text-2xl font-black tracking-[-0.04em] text-white">{summary.productName}</h2>
                            </div>
                        </div>
                        <div className="mt-7 flex items-end justify-between border-t border-white/10 pt-5">
                            <div>
                                <p className="text-xs text-slate-500">월 보험료</p>
                                <p className="mt-1 text-2xl font-black text-white">{formatWon(summary.baseMonthlyPremium)}</p>
                            </div>
                            <div className="text-right">
                                <p className="text-xs text-slate-500">가입 담보</p>
                                <p className="mt-1 font-black text-[#82D8FC]">{selectedCoverages.length}개 선택</p>
                            </div>
                        </div>
                    </div>
                </section>

                <section>
                    <div className="mb-4 flex items-end justify-between">
                        <div>
                            <p className="text-xs font-bold text-[#82D8FC]">보장 구성</p>
                            <h2 className="mt-1 text-xl font-black text-white">필요한 담보를 선택하세요</h2>
                        </div>
                        <span className="text-xs text-slate-500">최소 1개</span>
                    </div>
                    <div className="space-y-3">
                        {product.coverages.map((coverage) => {
                            const checked = selectedCoverageIds.includes(coverage.productCoverageId);
                            return (
                                <button
                                    key={coverage.productCoverageId}
                                    type="button"
                                    onClick={() => toggleCoverage(coverage.productCoverageId)}
                                    className={`w-full rounded-2xl border p-5 text-left transition-all ${checked ? 'border-[#82D8FC]/50 bg-[#82D8FC]/10' : 'border-slate-800 bg-[#0B1220]'}`}
                                >
                                    <div className="flex items-start gap-3">
                                        <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-md border ${checked ? 'border-[#82D8FC] bg-[#82D8FC] text-[#020715]' : 'border-slate-600 text-transparent'}`}>
                                            <Check className="h-3.5 w-3.5" strokeWidth={3} />
                                        </span>
                                        <div className="min-w-0 flex-1">
                                            <div className="flex items-start justify-between gap-3">
                                                <h3 className="font-black text-white">{coverage.coverageName}</h3>
                                                <span className="shrink-0 text-sm font-black text-[#82D8FC]">{formatWon(coverage.insuredAmount)}</span>
                                            </div>
                                            <p className="mt-2 text-sm leading-5 text-slate-400">{coverage.description}</p>
                                            <div className="mt-3 flex items-center gap-1.5 text-xs font-bold text-amber-200/80">
                                                <CalendarClock className="h-3.5 w-3.5" />
                                                {conditionText(coverage)}
                                            </div>
                                        </div>
                                    </div>
                                </button>
                            );
                        })}
                    </div>
                </section>

                <section className="rounded-[26px] border border-slate-800 bg-[#09111F] p-5">
                    <div className="flex items-start gap-3">
                        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-[#F2BEF7]/10 text-[#F2BEF7]">
                            <Sparkles className="h-5 w-5" />
                        </span>
                        <div>
                            <p className="text-xs font-bold text-[#F2BEF7]">30초 약관 이해</p>
                            <h2 className="mt-1 text-lg font-black text-white">가입 전에 이것만은 확인하세요</h2>
                        </div>
                    </div>
                    <div className="mt-5 space-y-3">
                        {terms.highlights.map((highlight) => (
                            <button
                                key={`${highlight.category}-${highlight.termsClauseId}`}
                                type="button"
                                onClick={() => openClause(highlight.termsClauseId)}
                                className="w-full rounded-2xl bg-slate-900/80 p-4 text-left transition-colors hover:bg-slate-800"
                            >
                                <div className="flex items-center justify-between gap-3">
                                    <div className="flex items-center gap-2">
                                        <FileCheck2 className="h-4 w-4 text-[#82D8FC]" />
                                        <span className="text-sm font-black text-white">{highlight.title}</span>
                                    </div>
                                    <FileText className="h-4 w-4 shrink-0 text-slate-500" />
                                </div>
                                <p className="mt-2 line-clamp-2 text-xs leading-5 text-slate-400">{highlight.content}</p>
                                <span className="mt-2 inline-block text-[11px] font-bold text-[#82D8FC]">원문 근거 보기</span>
                            </button>
                        ))}
                    </div>
                    <p className="mt-4 text-[11px] leading-5 text-slate-600">{terms.disclaimer}</p>
                </section>

                <section className="flex items-start gap-3 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
                    <LockKeyhole className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" />
                    <p className="text-xs leading-5 text-slate-500">
                        다음 단계에서 선택한 상품 버전·담보·보험료·약관 해시를 견적 Snapshot으로 고정합니다.
                    </p>
                </section>

                {error && (
                    <div className="flex items-start gap-2 rounded-2xl border border-rose-400/20 bg-rose-400/10 p-4 text-sm text-rose-200">
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                        {error}
                    </div>
                )}
            </main>

            <div className="fixed app-fixed-cta left-1/2 z-40 w-full max-w-[560px] -translate-x-1/2 bg-gradient-to-t from-[#020715] via-[#020715] to-transparent px-6 pb-6 pt-8">
                <AppButton
                    onClick={handleQuote}
                    disabled={selectedCoverageIds.length === 0 || submitting}
                    className="text-base font-black"
                >
                    {submitting ? <Loader2 className="h-5 w-5 animate-spin" /> : <ShieldCheck className="h-5 w-5" />}
                    {submitting
                        ? '견적을 고정하고 있어요'
                        : `${hasAccessToken() ? '' : '로그인하고 '}${formatWon(summary.baseMonthlyPremium)}으로 견적 받기`}
                </AppButton>
                {quote?.quoteNo && (
                    <p className="mt-2 text-center text-[11px] text-slate-600">최근 견적 {quote.quoteNo}</p>
                )}
            </div>

            {(clause || clauseLoading) && (
                <div className="fixed inset-0 z-[120] flex items-end justify-center bg-black/70 p-0 backdrop-blur-sm sm:items-center sm:p-6">
                    <div className="max-h-[78vh] w-full max-w-[520px] overflow-y-auto rounded-t-[30px] border border-slate-700 bg-[#0B1322] p-6 shadow-2xl sm:rounded-[30px]">
                        <div className="flex items-start justify-between gap-4">
                            <div>
                                <p className="text-xs font-bold text-[#82D8FC]">약관 원문 근거</p>
                                <h3 className="mt-1 text-lg font-black text-white">{clause?.title || '조항을 불러오는 중입니다'}</h3>
                            </div>
                            <button
                                type="button"
                                onClick={() => setClause(null)}
                                className="rounded-full bg-slate-800 p-2 text-slate-300"
                                aria-label="약관 원문 닫기"
                            >
                                <X className="h-5 w-5" />
                            </button>
                        </div>
                        {clauseLoading ? (
                            <div className="flex min-h-48 items-center justify-center">
                                <Loader2 className="h-7 w-7 animate-spin text-[#82D8FC]" />
                            </div>
                        ) : (
                            <>
                                <div className="mt-5 rounded-2xl bg-slate-950/60 p-5 text-sm leading-7 text-slate-300 whitespace-pre-wrap">
                                    {clause?.content}
                                </div>
                                <div className="mt-4 flex flex-wrap gap-2 text-[11px] text-slate-500">
                                    <span className="rounded-full bg-slate-900 px-3 py-1">{clause?.clauseCode}</span>
                                    <span className="rounded-full bg-slate-900 px-3 py-1">약관 {clause?.documentVersion}</span>
                                    <span className="rounded-full bg-slate-900 px-3 py-1">교육용 시뮬레이션</span>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default CancerInsuranceProductPage;
