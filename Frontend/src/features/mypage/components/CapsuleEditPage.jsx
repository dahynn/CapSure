import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
    ChevronLeft, ShieldCheck, Plus, Trash2, CheckCircle2,
    Loader2, CalendarClock, Package, AlertCircle, Sparkles
} from 'lucide-react';
import { getNextItems, reserveNextItem, cancelNextItem, confirmNext } from '@/features/mypage/api/mypage.api';

/* ───────────────── 스켈레톤 카드 ───────────────── */
const SkeletonCard = () => (
    <div className="p-4 bg-[#141925] rounded-[24px] border border-slate-800/60 flex gap-4 items-center animate-pulse">
        <div className="w-10 h-10 rounded-full bg-slate-800 shrink-0" />
        <div className="flex-1 space-y-2">
            <div className="h-3 w-24 bg-slate-800 rounded-full" />
            <div className="h-2.5 w-36 bg-slate-800/60 rounded-full" />
        </div>
        <div className="h-3 w-12 bg-slate-800 rounded-full" />
    </div>
);

/* ───────────────── 메인 컴포넌트 ───────────────── */
const CapsuleEditPage = () => {
    const { id } = useParams();          // subscriptionId
    const navigate = useNavigate();

    const [data, setData]         = useState(null);
    const [loading, setLoading]   = useState(true);
    const [error, setError]       = useState(null);
    const [confirming, setConfirming] = useState(false);
    const [confirmSuccess, setConfirmSuccess] = useState(false);

    /* ── 조회 ── */
    const fetchData = async () => {
        try {
            setLoading(true);
            const res = await getNextItems(id);
            setData(res);
        } catch (e) {
            console.error(e);
            setError('익월 변경 정보를 불러오는 데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, [id]);

    /* ── 예약 취소 ── */
    const handleCancel = async (subscriptionItemId) => {
        try {
            await cancelNextItem(id, subscriptionItemId);
            setData(prev => ({
                ...prev,
                nextItems: prev.nextItems.filter(i => i.subscriptionItemId !== subscriptionItemId)
            }));
        } catch {
            alert('취소에 실패했습니다. 다시 시도해주세요.');
        }
    };

    /* ── 변경 확정 ── */
    const handleConfirm = async () => {
        if (!data?.nextItems?.length) return;
        setConfirming(true);
        try {
            await confirmNext(id);
            setConfirmSuccess(true);
            setTimeout(() => navigate(-1), 1800);
        } catch {
            alert('변경 확정에 실패했습니다. 다시 시도해주세요.');
        } finally {
            setConfirming(false);
        }
    };

    /* ── 로딩 ── */
    if (loading) {
        return (
            <div className="w-full min-h-screen bg-[#0B0E14] flex flex-col items-center justify-center gap-4">
                <Loader2 className="w-8 h-8 text-[#82D8FC] animate-spin" />
                <p className="text-[#9D9DA4] text-sm">익월 변경 정보를 불러오는 중...</p>
            </div>
        );
    }

    /* ── 에러 ── */
    if (error || !data) {
        return (
            <div className="w-full min-h-screen bg-[#0B0E14] flex flex-col items-center justify-center gap-4 px-8">
                <AlertCircle className="w-10 h-10 text-red-400" />
                <p className="text-red-400 text-sm text-center">{error || '데이터를 찾을 수 없습니다.'}</p>
                <button
                    onClick={() => navigate(-1)}
                    className="px-6 py-2.5 bg-[#1C212E] rounded-xl text-white text-sm hover:bg-[#1E2535] transition-colors"
                >돌아가기</button>
            </div>
        );
    }

    /* ── 확정 성공 ── */
    if (confirmSuccess) {
        return (
            <div className="w-full min-h-screen bg-[#0B0E14] flex flex-col items-center justify-center gap-5 px-8 animate-in fade-in duration-500">
                <div className="w-20 h-20 rounded-full bg-[#82D8FC]/10 border border-[#82D8FC]/30 flex items-center justify-center">
                    <CheckCircle2 className="w-10 h-10 text-[#82D8FC]" />
                </div>
                <h2 className="text-white text-xl font-bold">변경이 확정되었습니다!</h2>
                <p className="text-[#9D9DA4] text-sm text-center">
                    {data.nextBillingAt} 결제일부터<br />변경된 보장 내역이 적용됩니다.
                </p>
            </div>
        );
    }

    const hasNextItems = data.nextItems && data.nextItems.length > 0;

    return (
        <div className="w-full min-h-screen bg-[#0B0E14]">
            <div className="px-6 py-8 max-w-[560px] mx-auto w-full pb-40 animate-in fade-in slide-in-from-bottom-4 duration-500">

                {/* ── 헤더 ── */}
                <div className="flex items-center justify-between mb-8">
                    <button
                        onClick={() => navigate(-1)}
                        className="p-2 hover:bg-[#1E2535] rounded-full transition-colors text-white -ml-2"
                    >
                        <ChevronLeft className="w-6 h-6" />
                    </button>
                    <h1 className="text-white text-[17px] font-bold tracking-tight">익월 캡슐 변경</h1>
                    <div className="w-10" />
                </div>

                {/* ── 결제일 배너 ── */}
                <div className="flex items-center gap-3 px-5 py-4 bg-[#141925] border border-[#82D8FC]/20 rounded-[20px] mb-8 shadow-lg">
                    <CalendarClock className="w-5 h-5 text-[#82D8FC] shrink-0" />
                    <div>
                        <p className="text-[#9D9DA4] text-[11px] font-medium">다음 결제일 / 변경 적용일</p>
                        <p className="text-white text-sm font-bold mt-0.5">{data.nextBillingAt}</p>
                    </div>
                </div>

                {/* ── 이번 달 보장 중인 상품 ── */}
                <section className="mb-8">
                    <h2 className="text-white text-[15px] font-bold mb-3 flex items-center gap-2 px-1 opacity-90">
                        <ShieldCheck className="w-4 h-4 text-[#82D8FC]" />
                        이번 달 보장 중인 상품
                    </h2>

                    {data.currentItems.length === 0 ? (
                        <div className="flex flex-col items-center justify-center py-8 bg-[#141925]/50 rounded-[24px] border border-slate-800/40">
                            <Package className="w-7 h-7 text-slate-700 mb-2" />
                            <p className="text-slate-500 text-[13px]">현재 보장 중인 상품이 없습니다.</p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {data.currentItems.map((item) => (
                                <div
                                    key={item.subscriptionItemId}
                                    className="flex items-center gap-3 p-4 bg-[#141925] rounded-[20px] border border-slate-800/60 shadow-md"
                                >
                                    <div className="w-9 h-9 rounded-full bg-[#0B0E14] border border-slate-700/80 flex items-center justify-center shrink-0">
                                        <ShieldCheck className="w-4 h-4 text-[#82D8FC]" />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <p className="text-white text-[13px] font-bold truncate">{item.productName}</p>
                                        <p className="text-[#9D9DA4] text-[11px] mt-0.5">{item.companyName}</p>
                                    </div>
                                    <span className="text-[#82D8FC] text-[13px] font-bold shrink-0">
                                        {item.monthlyPrice.toLocaleString()}원
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}
                </section>

                {/* ── 다음 달 예약 상품 ── */}
                <section className="mb-8">
                    <div className="flex items-center justify-between mb-3 px-1">
                        <h2 className="text-white text-[15px] font-bold flex items-center gap-2 opacity-90">
                            <Sparkles className="w-4 h-4 text-[#B08EFF]" />
                            다음 달 예약 상품
                        </h2>
                        <button
                            onClick={() => navigate(`/capsure-insurance`)}
                            className="flex items-center gap-1 px-3 py-1.5 bg-[#B08EFF]/10 border border-[#B08EFF]/30 rounded-full text-[#B08EFF] text-[11px] font-bold hover:bg-[#B08EFF]/20 transition-all active:scale-95"
                        >
                            <Plus className="w-3.5 h-3.5" />
                            상품 추가
                        </button>
                    </div>

                    {!hasNextItems ? (
                        <div className="flex flex-col items-center justify-center py-10 bg-[#141925]/50 rounded-[24px] border border-slate-800/40 border-dashed">
                            <Plus className="w-8 h-8 text-slate-700 mb-2" />
                            <p className="text-slate-500 text-[13px] font-medium">예약된 변경 상품이 없습니다.</p>
                            <p className="text-slate-600 text-[11px] mt-1">위 버튼으로 상품을 추가하세요.</p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {data.nextItems.map((item) => (
                                <div
                                    key={item.subscriptionItemId}
                                    className="flex items-center gap-3 p-4 bg-[#1A1527] rounded-[20px] border border-[#B08EFF]/20 shadow-md animate-in slide-in-from-bottom-2 fade-in duration-300"
                                >
                                    <div className="w-9 h-9 rounded-full bg-[#0B0E14] border border-[#B08EFF]/30 flex items-center justify-center shrink-0">
                                        <Sparkles className="w-4 h-4 text-[#B08EFF]" />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <p className="text-white text-[13px] font-bold truncate">{item.productName}</p>
                                        <p className="text-[#9D9DA4] text-[11px] mt-0.5">{item.companyName}</p>
                                    </div>
                                    <span className="text-[#B08EFF] text-[13px] font-bold shrink-0 mr-2">
                                        {item.monthlyPrice.toLocaleString()}원
                                    </span>
                                    <button
                                        onClick={() => handleCancel(item.subscriptionItemId)}
                                        className="w-8 h-8 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-400 hover:bg-red-500/20 transition-all active:scale-95 shrink-0"
                                    >
                                        <Trash2 className="w-3.5 h-3.5" />
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </section>

                {/* ── 변경 안내 ── */}
                <div className="flex gap-2.5 px-4 py-3.5 bg-[#82D8FC]/5 border border-[#82D8FC]/15 rounded-[16px] mb-8">
                    <AlertCircle className="w-4 h-4 text-[#82D8FC]/60 shrink-0 mt-0.5" />
                    <p className="text-[#9D9DA4] text-[11px] leading-relaxed">
                        변경 확정 후에는 다음 결제일({data.nextBillingAt})부터 적용됩니다.
                        확정 전까지는 언제든지 수정 가능합니다.
                    </p>
                </div>

            </div>

            {/* ── 하단 고정 변경 확정 버튼 ── */}
            <div className="fixed bottom-0 left-0 right-0 px-6 pb-8 pt-4 bg-gradient-to-t from-[#0B0E14] via-[#0B0E14]/95 to-transparent">
                <div className="max-w-[560px] mx-auto">
                    <button
                        onClick={handleConfirm}
                        disabled={!hasNextItems || confirming}
                        className={`w-full py-4 rounded-[18px] text-base font-bold transition-all active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg ${
                            hasNextItems
                                ? 'bg-gradient-to-r from-[#82D8FC] to-[#B08EFF] text-[#0B0E14] shadow-[0_0_24px_rgba(130,216,252,0.25)]'
                                : 'bg-[#1C212E] text-slate-500'
                        }`}
                    >
                        {confirming ? (
                            <><Loader2 className="w-5 h-5 animate-spin" /> 처리 중...</>
                        ) : (
                            <><CheckCircle2 className="w-5 h-5" /> 익월 변경 확정</>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default CapsuleEditPage;
