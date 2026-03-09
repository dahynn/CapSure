import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCancelTermsAndRefund, executeCancel } from './api/capsuleCancel.api';
import { getMyCapsuleInsurance } from '../capsule/api/capsuleInsurance.api';
import { ShieldAlert, AlertTriangle, Loader2, CheckSquare, Square, ShieldCheck, ArrowRight, CheckCircle } from 'lucide-react';
import TextModal from '@/common/components/ui/modal/TextModal';

const CapsuleCancelPage = () => {
    const navigate = useNavigate();

    // View states: 'step-select' -> 'step-terms' -> 'step-complete'
    const [view, setView] = useState('step-select');

    // Current Subscribe Data
    const [myCapsules, setMyCapsules] = useState([]);
    const [isLoadingCapsules, setIsLoadingCapsules] = useState(true);

    // Cancel selection
    const [selectedCancelId, setSelectedCancelId] = useState(null);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

    // Terms & Refund Date
    const [cancelData, setCancelData] = useState(null);
    const [isLoadingTerms, setIsLoadingTerms] = useState(false);
    const [checkedTerms, setCheckedTerms] = useState({});

    // Final Execute
    const [isCanceling, setIsCanceling] = useState(false);

    useEffect(() => {
        // Fetch current subscribed items to show on 'step-select'
        const fetchSubscribed = async () => {
            setIsLoadingCapsules(true);
            try {
                const data = await getMyCapsuleInsurance();
                setMyCapsules(data.selectedCells || []);
            } catch (e) {
                console.error(e);
            } finally {
                setIsLoadingCapsules(false);
            }
        };
        fetchSubscribed();
    }, []);

    const handleSelectCell = (cell) => {
        if (!cell) return;
        setSelectedCancelId(cell.groupId);
        setIsConfirmModalOpen(true);
    };

    const confirmSelectionToTerms = async () => {
        setIsConfirmModalOpen(false);
        setIsLoadingTerms(true);
        setView('step-terms');

        try {
            // Find unique selected item to get a mock ID
            // Here just pass [selectedCancelId] for mock purposes
            const data = await getCancelTermsAndRefund([selectedCancelId]);
            setCancelData(data);

            const checks = {};
            data.termsList.forEach(t => { checks[t.id] = false; });
            setCheckedTerms(checks);

        } catch (e) {
            console.error(e);
        } finally {
            setIsLoadingTerms(false);
        }
    };

    const toggleTerm = (id) => {
        setCheckedTerms(prev => ({ ...prev, [id]: !prev[id] }));
    };

    const isAllTermsAgreed = cancelData && Object.values(checkedTerms).every(v => v === true);

    const handleExecuteCancel = async () => {
        setIsCanceling(true);
        try {
            await executeCancel([selectedCancelId]);
            setView('step-complete');
        } catch (e) {
            console.error(e);
        } finally {
            setIsCanceling(false);
        }
    };

    return (
        <div className="p-6 md:p-8 pb-24 min-h-screen bg-slate-50">
            {/* ----------------- STEP SELECT ----------------- */}
            {view === 'step-select' && (
                <div className="max-w-4xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 py-8">
                    <div className="flex justify-between items-center bg-white p-6 rounded-3xl border border-slate-200 shadow-sm">
                        <div className="flex items-center gap-4">
                            <div className="w-12 h-12 bg-red-50 text-red-500 rounded-xl flex items-center justify-center">
                                <AlertTriangle className="w-6 h-6" />
                            </div>
                            <div>
                                <h1 className="text-2xl font-black text-slate-900">캡슐 보험 중도 취소</h1>
                                <p className="text-slate-500 mt-1">취소할 보장 캡슐을 <strong>선택</strong>해 주세요.</p>
                            </div>
                        </div>
                        <button onClick={() => navigate(-1)} className="px-4 py-2 font-medium text-slate-500 hover:bg-slate-100 rounded-lg transition-colors">
                            돌아가기
                        </button>
                    </div>

                    <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-200">
                        {isLoadingCapsules ? (
                            <div className="flex justify-center items-center py-20">
                                <Loader2 className="w-8 h-8 animate-spin text-slate-400" />
                            </div>
                        ) : (
                            <div className="flex flex-col items-center">
                                <div className="bg-slate-100 rounded-2xl border-2 border-slate-200 p-8 grid grid-cols-5 gap-2 w-full max-w-2xl place-content-center">
                                    {myCapsules.map((cell, i) => (
                                        <div
                                            key={i}
                                            onClick={() => handleSelectCell(cell)}
                                            className={`aspect-square sm:aspect-auto sm:h-24 rounded-xl border flex flex-col items-center justify-center shadow-sm cursor-pointer transition-transform hover:-translate-y-1 hover:shadow-md ${cell ? cell.category.color : 'bg-white/50 border-slate-200 text-slate-300 pointer-events-none'
                                                }`}
                                        >
                                            {cell ? (
                                                <span className="font-bold text-[10px] sm:text-xs text-center px-1 leading-tight">{cell.name}</span>
                                            ) : (
                                                <span className="text-xs font-bold opacity-50">빈 칸</span>
                                            )}
                                        </div>
                                    ))}
                                </div>
                                <p className="mt-8 text-sm text-slate-400 font-medium">※ 취소 시 약관에 따라 위약금이 발생할 수 있습니다.</p>
                            </div>
                        )}
                    </div>

                    <TextModal
                        isOpen={isConfirmModalOpen}
                        onClose={() => setIsConfirmModalOpen(false)}
                        onConfirm={confirmSelectionToTerms}
                        confirmText="확인"
                        cancelText="취소"
                        contents="해당 보험을 취소하시겠습니까? 약관에 따른 불이익과 위약금이 있을 수 있습니다."
                    />
                </div>
            )}

            {/* ----------------- STEP TERMS & CALCULATION ----------------- */}
            {view === 'step-terms' && (
                <div className="max-w-5xl mx-auto animate-in fade-in slide-in-from-right-4">
                    <div className="mb-8">
                        <button onClick={() => setView('step-select')} className="text-slate-500 font-medium hover:text-slate-800 transition-colors mb-4">
                            &larr; 뒤로 가기
                        </button>
                        <h2 className="text-3xl font-black text-slate-900">취소 약관 확인</h2>
                        <p className="text-slate-500 mt-2">안전한 해지를 위해 아래 약관을 확인하고 동의해주세요.</p>
                    </div>

                    {isLoadingTerms || !cancelData ? (
                        <div className="flex justify-center py-20">
                            <Loader2 className="w-10 h-10 animate-spin text-slate-400" />
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                            {/* Left: Terms List */}
                            <div className="lg:col-span-2 space-y-6">
                                {cancelData.termsList.map((term) => (
                                    <div key={term.id} className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-slate-300 transition-colors">
                                        <div
                                            onClick={() => toggleTerm(term.id)}
                                            className="flex items-center gap-3 cursor-pointer group mb-4"
                                        >
                                            {checkedTerms[term.id] ? (
                                                <CheckSquare className="w-6 h-6 text-primary-500" />
                                            ) : (
                                                <Square className="w-6 h-6 text-slate-300 group-hover:text-primary-400 transition-colors" />
                                            )}
                                            <span className="font-bold text-lg text-slate-800 select-none">동의합니다 <span className="text-slate-500 font-medium text-sm ml-2">({term.title})</span></span>
                                        </div>
                                        <div className="bg-slate-50 border border-slate-100 p-4 rounded-xl h-40 overflow-y-auto text-sm text-slate-600 leading-relaxed custom-scrollbar">
                                            {term.content.split('\n').map((line, idx) => (
                                                <React.Fragment key={idx}>
                                                    {line}<br />
                                                </React.Fragment>
                                            ))}
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {/* Right: Calculation & Execute */}
                            <div className="lg:col-span-1">
                                <div className="bg-white p-6 rounded-3xl shadow-lg border border-red-100 flex flex-col items-center sticky top-24">
                                    <h3 className="font-bold text-slate-800 text-lg w-full mb-6 border-b border-slate-100 pb-4">예상 환불 금액</h3>

                                    <div className="w-full space-y-4 mb-8">
                                        <div className="flex justify-between items-center text-slate-600">
                                            <span>결제 금액</span>
                                            <span className="font-medium">{cancelData.originalAmount.toLocaleString()}원</span>
                                        </div>
                                        <div className="flex justify-between items-center text-red-500 font-bold">
                                            <span>위약금 (차감)</span>
                                            <span>- {cancelData.penaltyAmount.toLocaleString()}원</span>
                                        </div>
                                        <div className="h-px bg-slate-200 my-2" />
                                        <div className="flex justify-between items-center">
                                            <span className="font-bold text-slate-800">환불 예정 금액</span>
                                            <span className="text-2xl font-black text-primary-600">
                                                {cancelData.refundAmount.toLocaleString()}원
                                            </span>
                                        </div>
                                    </div>

                                    <button
                                        disabled={!isAllTermsAgreed || isCanceling}
                                        onClick={handleExecuteCancel}
                                        className={`w-full py-4 font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-2 ${isAllTermsAgreed
                                            ? 'bg-red-500 text-white hover:bg-red-600'
                                            : 'bg-slate-200 text-slate-400 cursor-not-allowed shadow-none'
                                            }`}
                                    >
                                        {isCanceling ? (
                                            <>
                                                <Loader2 className="w-5 h-5 animate-spin" />
                                                처리 중...
                                            </>
                                        ) : (
                                            <>
                                                취소하기
                                                <ArrowRight className="w-5 h-5" />
                                            </>
                                        )}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* ----------------- STEP COMPLETE ----------------- */}
            {view === 'step-complete' && (
                <div className="max-w-2xl mx-auto flex flex-col items-center justify-center min-h-[60vh] text-center animate-in fade-in zoom-in-95 duration-500">
                    <div className="w-24 h-24 bg-emerald-100 text-emerald-500 rounded-full flex items-center justify-center mb-6">
                        <CheckCircle className="w-12 h-12" />
                    </div>
                    <h2 className="text-3xl font-black text-slate-900 mb-4">취소 완료</h2>
                    <p className="text-slate-500 text-lg mb-8 max-w-sm">
                        선택하신 보험 내역이 정상적으로 취소 처리되었습니다.<br />
                        환불 금액은 영업일 기준 2~3일 내에 입금될 예정입니다.
                    </p>
                    <button
                        onClick={() => navigate('/my-capsule')}
                        className="px-8 py-3 bg-slate-800 text-white font-bold rounded-xl hover:bg-slate-900 transition-colors shadow-lg"
                    >
                        내 구독 보러가기
                    </button>
                </div>
            )}
        </div>
    );
};

export default CapsuleCancelPage;
