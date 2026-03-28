import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCapsure } from './context/CapsureContext';
import { ChevronLeft, Sparkles, ChevronRight, Check, FileText } from 'lucide-react';
import { httpClient } from '@/common/api/httpClient';
import { getProductSourceId } from './utils/productSource';

// 개별 상품별 약관 요약을 API에서 가져오는 함수
const fetchTermsSummary = async (productSourceId) => {
    try {
        const res = await httpClient.get(`/insurers/product-sources/${productSourceId}/terms-summary/light`);
        const data = res.data;
        if (data.success) return data.data;
    } catch (e) {
        console.error('terms-summary fetch error', e);
    }
    return null;
};

// 필수 약관 항목 목록
const REQUIRED_TERMS = [
    { id: 'privacy', label: '개인정보 수집 및 이용 동의', required: true },
    { id: 'contract', label: '보험계약 기본 규정 확인', required: true },
    { id: 'duty', label: '고지의무 및 계약해지 안내', required: true },
    { id: 'marketing', label: '마케팅 정보 수신 동의', required: false },
];

const CapsureTermsPage = () => {
    const navigate = useNavigate();
    const { selectedProducts } = useCapsure();

    const [summaries, setSummaries] = useState({}); // { productSourceId: summaryData }
    const [loadingIds, setLoadingIds] = useState(new Set());
    const [checkedTerms, setCheckedTerms] = useState(
        REQUIRED_TERMS.reduce((acc, t) => ({ ...acc, [t.id]: false }), {})
    );
    // 각 상품별 체크 상태 추가
    const [checkedProducts, setCheckedProducts] = useState({});

    const fetchedRef = React.useRef(new Set());

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: 'auto' });
    }, []);

    // 모든 선택 상품의 약관 요약 API 호출 및 초기 체크 상태 설정
    useEffect(() => {
        const initialProducts = {};
        selectedProducts.forEach(async (product) => {
            const pid = getProductSourceId(product);
            initialProducts[pid] = false;
            
            if (fetchedRef.current.has(pid)) return;
            fetchedRef.current.add(pid);

            setLoadingIds(prev => new Set([...prev, pid]));
            const data = await fetchTermsSummary(pid);
            setSummaries(prev => ({ ...prev, [pid]: data || null }));
            setLoadingIds(prev => { const s = new Set(prev); s.delete(pid); return s; });
        });
        setCheckedProducts(prev => ({ ...initialProducts, ...prev }));
    }, [selectedProducts]);

    const allProductsChecked = selectedProducts.every(p => checkedProducts[getProductSourceId(p)]);
    const requiredChecked = REQUIRED_TERMS.filter(t => t.required).every(t => checkedTerms[t.id]) && allProductsChecked;
    const allChecked = REQUIRED_TERMS.every(t => checkedTerms[t.id]) && allProductsChecked;

    const handleToggleAll = () => {
        const newValue = !allChecked;
        setCheckedTerms(REQUIRED_TERMS.reduce((acc, t) => ({ ...acc, [t.id]: newValue }), {}));
        
        const newProductsChecked = {};
        selectedProducts.forEach(p => {
            newProductsChecked[getProductSourceId(p)] = newValue;
        });
        setCheckedProducts(newProductsChecked);
    };

    const handleToggleTerm = (id) => {
        setCheckedTerms(prev => ({ ...prev, [id]: !prev[id] }));
    };

    const handleToggleProduct = (pid) => {
        setCheckedProducts(prev => ({ ...prev, [pid]: !prev[pid] }));
    };

    const handleNext = () => {
        if (!requiredChecked) return;

        navigate('/capsure-insurance/payment-summary');
    };

    if (selectedProducts.length === 0) {
        return (
            <div className="flex-1 flex flex-col items-center justify-center p-6 text-center">
                <p className="text-white mb-4">선택된 상품이 없습니다.</p>
                <button onClick={() => navigate('/capsure-insurance')} className="bg-brand-blue text-[#020715] px-6 py-3 rounded-xl font-bold">
                    캡슐 보험 시작하기
                </button>
            </div>
        );
    }

    return (
        <div className="flex flex-col min-h-screen bg-[#020715] pb-48">
            {/* Header */}
            <header className="sticky top-0 z-50 flex items-center p-4 bg-[#020715]">
                <button onClick={() => navigate(-1)} className="p-2 text-white hover:bg-slate-800 rounded-full transition-colors -ml-2">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-base font-bold text-white absolute left-1/2 -translate-x-1/2">약관 동의 및 가입 확인</h1>
            </header>

            <div className="px-6 pt-4">
                {/* AI 요약 섹션 제목 */}
                <div className="flex items-center gap-2 mb-1">
                    <Sparkles className="w-5 h-5 text-brand-light-purple" />
                    <h2 className="text-white text-xl font-black">AI가 요약한 핵심 약관</h2>
                </div>
                <p className="text-slate-400 text-sm mb-6 pl-7">복잡한 약관, AI가 핵심만 짚어드려요.</p>

                {/* 상품별 약관 카드 */}
                <div className="flex flex-col gap-4 mb-10">
                    {selectedProducts.map(product => {
                        const pid = getProductSourceId(product);
                        const summary = summaries[pid];
                        const isLoading = loadingIds.has(pid);
                        return (
                            <TermsSummaryCard
                                key={pid}
                                product={product}
                                summary={summary}
                                isLoading={isLoading}
                                isChecked={checkedProducts[pid]}
                                onToggle={() => handleToggleProduct(pid)}
                            />
                        );
                    })}
                </div>

                {/* 약관 동의 섹션 */}
                <h2 className="text-white text-xl font-black mb-4">약관 동의</h2>

                {/* 전체 동의 버튼 */}
                <button
                    onClick={handleToggleAll}
                    className="w-full flex items-center gap-3 bg-[#192235] border border-slate-700/60 rounded-2xl px-4 py-4 mb-3"
                >
                    <div className={`w-5 h-5 rounded border-2 flex items-center justify-center flex-shrink-0 transition-all ${allChecked ? 'bg-brand-blue border-brand-blue' : 'border-slate-600'}`}>
                        {allChecked && <Check className="w-3 h-3 text-[#020715]" strokeWidth={3} />}
                    </div>
                    <span className="text-white font-black text-base">필수 약관 전체 동의</span>
                </button>

                {/* 개별 약관 항목들 */}
                <div className="flex flex-col divide-y divide-slate-800/50">
                    {REQUIRED_TERMS.map(term => (
                        <button
                            key={term.id}
                            onClick={() => handleToggleTerm(term.id)}
                            className="w-full flex items-center gap-3 py-3.5 text-left"
                        >
                            <div className={`w-4 h-4 rounded border flex items-center justify-center flex-shrink-0 transition-all ${checkedTerms[term.id] ? 'bg-brand-blue border-brand-blue' : 'border-slate-600'}`}>
                                {checkedTerms[term.id] && <Check className="w-2.5 h-2.5 text-[#020715]" strokeWidth={3} />}
                            </div>
                            <span className="flex-1 text-slate-300 text-sm font-medium">
                                <span className={`font-bold mr-1 ${term.required ? 'text-brand-blue' : 'text-slate-500'}`}>
                                    [{term.required ? '필수' : '선택'}]
                                </span>
                                {term.label}
                            </span>
                            <ChevronRight className="w-4 h-4 text-slate-600 flex-shrink-0" />
                        </button>
                    ))}
                </div>
            </div>

            {/* 하단 CTA */}
            <div className="fixed app-fixed-cta left-0 right-0 max-w-[560px] mx-auto px-6 pb-8 pt-6 bg-gradient-to-t from-[#020715] via-[#020715] to-transparent z-40">
                <button
                    onClick={handleNext}
                    disabled={!requiredChecked}
                    className={`w-full py-4 rounded-2xl font-black text-lg flex items-center justify-center gap-2 transition-all ${requiredChecked ? 'bg-brand-blue text-[#020715] hover:opacity-80 active:scale-[0.98]' : 'bg-slate-800 text-slate-500 cursor-not-allowed'}`}
                >
                    결제 정보 확인하기
                    {requiredChecked && <Check className="w-5 h-5" strokeWidth={3} />}
                </button>
                <p className="text-center text-slate-600 text-xs mt-3">다음 단계에서 총 결제 금액을 확인한 뒤 결제를 진행합니다.</p>
            </div>
        </div>
    );
};

// ─── 상품별 약관 요약 카드 컴포넌트 ────────────────────────────────────────────
const TermsSummaryCard = ({ product, summary, isLoading, isChecked, onToggle }) => {
    const productName = product.productName;
    const companyName = product.companyName;
    const termsUri = product.termsUri;

    const bulletPoints = summary ? [
        summary.coverageSummary && { label: '핵심 보장', value: summary.coverageSummary },
        summary.featureSummary && { label: '특징', value: summary.featureSummary },
        summary.paymentSummary && { label: '보험료', value: summary.paymentSummary },
    ].filter(Boolean) : [];

    return (
        <div className={`bg-[#0D1526]/80 backdrop-blur-sm border rounded-3xl overflow-hidden transition-all hover:bg-[#111A2C] group ${isChecked ? 'border-brand-blue/50' : 'border-slate-700/40'}`}>
            {/* 카드 헤더 */}
            <div className="flex items-start justify-between px-6 pt-6 pb-4">
                <div className="flex flex-col flex-1 min-w-0 pr-2">
                    <span className="text-brand-blue font-bold text-[10px] tracking-wider uppercase mb-1 opacity-80">{companyName}</span>
                    <h3 className="text-white font-black text-lg leading-tight break-keep">
                        {productName}
                    </h3>
                </div>

                {/* 작고 이쁜 PDF 버튼 */}
                <a 
                    href={termsUri}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-1.5 bg-slate-800/80 border border-slate-700/50 hover:border-brand-blue/50 hover:bg-brand-blue/5 py-1.5 px-2.5 rounded-xl transition-all flex-shrink-0 group/btn"
                >
                    <div className="relative w-3.5 h-4.5 bg-[#1D2B44] rounded-[2px] border-[1px] border-slate-600 group-hover/btn:border-brand-blue/50 transition-colors overflow-hidden">
                        <div className="absolute top-0 right-0 w-1.5 h-1.5 bg-[#0D1526] border-b border-l border-slate-600 rounded-bl-sm" />
                        <div className="flex flex-col gap-[1px] mt-1.5 px-[2px]">
                            <div className="w-full h-[1px] bg-slate-600" />
                            <div className="w-full h-[1px] bg-slate-600" />
                            <div className="w-2/3 h-[1px] bg-slate-600" />
                        </div>
                    </div>
                    <span className="text-slate-400 font-bold text-[10px] group-hover/btn:text-white transition-colors">PDF</span>
                </a>
            </div>

            {/* 구분선 */}
            <div className="h-[1px] bg-slate-800/80 mx-5" />

            {/* 약관 요약 본문 */}
            <div className="px-5 py-4">
                {isLoading ? (
                    <div className="flex flex-col gap-2 animate-pulse">
                        <div className="h-3 bg-slate-700 rounded w-3/4" />
                        <div className="h-3 bg-slate-700 rounded w-full" />
                        <div className="h-3 bg-slate-700 rounded w-5/6" />
                    </div>
                ) : bulletPoints.length > 0 ? (
                    <ul className="flex flex-col gap-3">
                        {bulletPoints.map((point, idx) => (
                            <li key={idx} className="flex items-start gap-2.5">
                                <span className="mt-[2px] w-1.5 h-1.5 rounded-full bg-brand-blue flex-shrink-0" />
                                <p className="text-slate-300 text-sm leading-snug break-keep">
                                    <span className="text-slate-500 font-bold mr-1">{point.label}:</span>
                                    {point.value}
                                </p>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <p className="text-slate-500 text-sm">약관 요약 정보가 없습니다. 원본 PDF를 확인해주세요.</p>
                )}
            </div>

            {/* 면책 사항 (로딩 중이 아니며 정보가 있을 때만 표시) */}
            {!isLoading && summary?.disclaimer && (
                <div className="mx-5 mb-1 p-3 bg-slate-800/20 rounded-xl">
                    <p className="text-slate-500 text-[11px] leading-relaxed">{summary.disclaimer}</p>
                </div>
            )}

            {/* 카드 하단 개별 동의 체크박스 */}
            <div 
                onClick={onToggle}
                className={`mx-5 mb-4 mt-2 p-4 rounded-2xl flex items-center gap-3 cursor-pointer transition-all border ${isChecked ? 'bg-brand-blue/10 border-brand-blue/30' : 'bg-slate-800/40 border-slate-700/50 hover:bg-slate-800/60'}`}
            >
                <div className={`w-5 h-5 rounded border flex items-center justify-center flex-shrink-0 transition-all ${isChecked ? 'bg-brand-blue border-brand-blue' : 'border-slate-600'}`}>
                    {isChecked && <Check className="w-3.5 h-3.5 text-[#020715]" strokeWidth={3} />}
                </div>
                <span className={`text-sm font-bold ${isChecked ? 'text-brand-blue' : 'text-slate-400'}`}>
                    위 상품 약관을 모두 확인했습니다
                </span>
            </div>
        </div>
    );
};

export default CapsureTermsPage;
