import React, { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Circle, ShieldCheck, ShieldX } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
    getDashboardProductDetail,
    getDiagnosisReport,
} from './api/dashboard.api';
import DashboardProductDetailModal from './components/DashboardProductDetailModal';

const CATEGORY_ORDER = ['DEATH', 'CANCER', 'BRAIN_HEART', 'ACTUAL_LOSS', 'SURGERY', 'ACCIDENT', 'LIABILITY'];

const CATEGORY_LABELS = {
    DEATH: '사망',
    CANCER: '암',
    BRAIN_HEART: '뇌/심장',
    ACTUAL_LOSS: '실손',
    SURGERY: '수술',
    ACCIDENT: '상해',
    LIABILITY: '배상책임',
    ETC: '기타',
};

const formatCurrency = (value) => {
    const amount = Number(value || 0);
    return new Intl.NumberFormat('ko-KR').format(amount);
};

const DiagnosisReportPage = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const [isLoading, setIsLoading] = useState(!location.state?.diagnosisReport);
    const [errorMessage, setErrorMessage] = useState('');
    const [diagnosisReport, setDiagnosisReport] = useState(location.state?.diagnosisReport || null);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState(null);
    const [isModalLoading, setIsModalLoading] = useState(false);
    const [modalErrorMessage, setModalErrorMessage] = useState('');

    useEffect(() => {
        if (location.state?.diagnosisReport) {
            return;
        }

        let isMounted = true;

        const fetchReport = async () => {
            setIsLoading(true);
            setErrorMessage('');
            try {
                const response = await getDiagnosisReport();
                if (!isMounted) {
                    return;
                }
                setDiagnosisReport(response);
            } catch (error) {
                if (!isMounted) {
                    return;
                }
                console.error('Failed to fetch diagnosis report:', error);
                setErrorMessage('정밀 진단 리포트를 불러오지 못했습니다.');
            } finally {
                if (isMounted) {
                    setIsLoading(false);
                }
            }
        };

        fetchReport();

        return () => {
            isMounted = false;
        };
    }, [location.state]);

    const sortedDiagnoses = useMemo(() => {
        const diagnoses = diagnosisReport?.diagnoses || [];
        return [...diagnoses].sort((a, b) => {
            const aIndex = CATEGORY_ORDER.indexOf(a.categoryCode);
            const bIndex = CATEGORY_ORDER.indexOf(b.categoryCode);
            return (aIndex === -1 ? Number.MAX_SAFE_INTEGER : aIndex)
                - (bIndex === -1 ? Number.MAX_SAFE_INTEGER : bIndex);
        });
    }, [diagnosisReport]);

    const handleOpenProductModal = async (productSourceId) => {
        if (!productSourceId) {
            return;
        }
        setIsModalOpen(true);
        setIsModalLoading(true);
        setModalErrorMessage('');
        setSelectedProduct(null);

        try {
            const detail = await getDashboardProductDetail(productSourceId);
            setSelectedProduct(detail);
        } catch (error) {
            console.error('Failed to fetch dashboard product detail:', error);
            setModalErrorMessage('추천 상품 상세 정보를 불러오지 못했습니다.');
        } finally {
            setIsModalLoading(false);
        }
    };

    return (
        <div className="mx-auto min-h-screen w-full max-w-[560px] px-6 pt-6 pb-10 md:px-8 md:pt-7">
            <div className="mb-5 flex items-center gap-2">
                <button
                    type="button"
                    onClick={() => navigate('/dashboard')}
                    className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-slate-700 bg-[#111A2D] text-slate-200 transition hover:border-slate-500 hover:text-white"
                    aria-label="대시보드로 돌아가기"
                >
                    <ArrowLeft className="h-4 w-4" />
                </button>
                <h2 className="text-[18px] font-semibold text-[#DDE4F2]">정밀 진단 리포트</h2>
            </div>

            <div className="mb-4 rounded-2xl border border-slate-800 bg-[#0B1324] px-4 py-3 text-[13px] text-slate-300">
                {diagnosisReport?.description || '카테고리별 보장 현황과 미가입 항목 추천 상품을 확인할 수 있습니다.'}
            </div>

            {isLoading ? (
                <div className="rounded-2xl border border-slate-800 bg-[#0B1324] px-4 py-12 text-center text-sm text-slate-300">
                    리포트를 불러오는 중입니다.
                </div>
            ) : null}

            {!isLoading && errorMessage ? (
                <div className="rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-10 text-center text-sm text-red-100">
                    {errorMessage}
                </div>
            ) : null}

            {!isLoading && !errorMessage ? (
                <div className="space-y-3">
                    {sortedDiagnoses.map((item) => {
                        const isInsured = Boolean(item.insured);
                        const label = CATEGORY_LABELS[item.categoryCode] || item.categoryName || item.categoryCode;
                        const coverageText = item.coverageNames?.length
                            ? item.coverageNames.join(', ')
                            : '등록된 보장 정보가 없습니다.';
                        const recommended = item.recommendedProduct;

                        return (
                            <article
                                key={item.categoryCode}
                                className="rounded-[20px] border border-slate-800 bg-[#0E172C] p-4 shadow-[0_6px_18px_rgba(0,0,0,0.2)]"
                            >
                                <div className="mb-2 flex items-start justify-between gap-3">
                                    <div className="flex items-center gap-2">
                                        <Circle
                                            className={`h-3 w-3 fill-current ${isInsured ? 'text-emerald-300' : 'text-rose-300'}`}
                                        />
                                        <p className="text-[20px] font-semibold leading-none text-[#DDE4F2]">{label}</p>
                                        <span
                                            className={`rounded-full px-2.5 py-1 text-[12px] font-semibold ${
                                                isInsured
                                                    ? 'bg-emerald-500/20 text-emerald-300'
                                                    : 'bg-rose-500/20 text-rose-300'
                                            }`}
                                        >
                                            {isInsured ? '가입됨' : '미가입'}
                                        </span>
                                    </div>

                                    <div
                                        className={`rounded-full border p-1.5 ${
                                            isInsured
                                                ? 'border-emerald-400/40 text-emerald-300'
                                                : 'border-rose-400/40 text-rose-300'
                                        }`}
                                    >
                                        {isInsured ? <ShieldCheck className="h-4 w-4" /> : <ShieldX className="h-4 w-4" />}
                                    </div>
                                </div>

                                {isInsured ? (
                                    <p className="text-[13px] leading-relaxed text-slate-300">
                                        <span className="text-slate-400">보유 보장: </span>
                                        {coverageText}
                                    </p>
                                ) : (
                                    <div className="rounded-xl border border-dashed border-slate-600/60 bg-[#0B1324] px-3 py-3">
                                        <p className="text-[12px] font-semibold text-[#82D8FC]">추천 상품</p>
                                        {recommended ? (
                                            <>
                                                <p className="mt-1 text-[14px] font-semibold text-[#DDE4F2]">
                                                    {recommended.productName}
                                                </p>
                                                <p className="mt-0.5 text-[12px] text-slate-400">
                                                    {recommended.companyName} · {formatCurrency(recommended.monthlyPrice)}원/월
                                                </p>
                                                <div className="mt-2 flex justify-end">
                                                    <button
                                                        type="button"
                                                        onClick={() => handleOpenProductModal(recommended.productSourceId)}
                                                        className="rounded-full bg-[#17304F] px-3 py-1.5 text-[12px] font-semibold text-[#82D8FC] transition hover:brightness-110"
                                                    >
                                                        상세보기
                                                    </button>
                                                </div>
                                            </>
                                        ) : (
                                            <p className="mt-1 text-[13px] text-slate-400">
                                                현재 추천 가능한 상품이 없습니다.
                                            </p>
                                        )}
                                    </div>
                                )}
                            </article>
                        );
                    })}
                </div>
            ) : null}

            <DashboardProductDetailModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                product={selectedProduct}
                isLoading={isModalLoading}
                errorMessage={modalErrorMessage}
            />
        </div>
    );
};

export default DiagnosisReportPage;
