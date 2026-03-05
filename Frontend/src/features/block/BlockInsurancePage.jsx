import React, { useState } from 'react';
import { ShieldCheck, Plus, Check, ArrowRight, Info, GripHorizontal } from 'lucide-react';

const BlockInsurancePage = () => {
    const [hasSubscription, setHasSubscription] = useState(false); // Toggle for dummy logic
    const [view, setView] = useState('prologue'); // prologue, create, grid-maker, subscribed-this, subscribed-next
    const [targetAmount, setTargetAmount] = useState('');

    // Dummy Block Categories
    const categories = [
        { id: 'disease', name: '질병 블록', color: 'bg-rose-100 border-rose-300 text-rose-700' },
        { id: 'liability', name: '배상 블록', color: 'bg-blue-100 border-blue-300 text-blue-700' },
        { id: 'pet', name: '펫 블록', color: 'bg-amber-100 border-amber-300 text-amber-700' },
    ];

    const handleCreateBlock = (e) => {
        e.preventDefault();
        if (targetAmount) {
            setView('grid-maker');
        }
    };

    return (
        <div className="p-6 md:p-8 pb-24 min-h-screen bg-slate-50">

            {/* ----------------- PROLOGUE VIEW ----------------- */}
            {!hasSubscription && view === 'prologue' && (
                <div className="max-w-2xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4">
                    <div className="text-center space-y-4 py-8">
                        <div className="w-20 h-20 bg-primary-100 text-primary-600 rounded-full flex items-center justify-center mx-auto mb-6">
                            <ShieldCheck className="w-10 h-10" />
                        </div>
                        <h1 className="text-3xl font-black text-slate-900">구독형 블록 보험이란?</h1>
                        <p className="text-lg text-slate-600 leading-relaxed max-w-lg mx-auto">
                            레고 블록을 조립하듯, 나에게 필요한 보장만 골라 담아
                            매월 결제하는 신개념 맞춤형 보험 서비스입니다.
                        </p>
                    </div>

                    <div className="bg-white p-8 rounded-3xl shadow-xl glass-panel space-y-6">
                        <div className="flex items-start gap-4">
                            <div className="mt-1 p-2 bg-emerald-100 text-emerald-600 rounded-lg"><Check className="w-5 h-5" /></div>
                            <div>
                                <h3 className="font-bold text-slate-800 text-lg">필요한 보장만 선택</h3>
                                <p className="text-slate-500 mt-1">불필요한 특약 없이 딱 필요한 것만 가입하세요.</p>
                            </div>
                        </div>
                        <div className="flex items-start gap-4">
                            <div className="mt-1 p-2 bg-blue-100 text-blue-600 rounded-lg"><Check className="w-5 h-5" /></div>
                            <div>
                                <h3 className="font-bold text-slate-800 text-lg">매월 자유로운 변경</h3>
                                <p className="text-slate-500 mt-1">상황에 따라 다음 달 보장 내역을 쉽게 바꿀 수 있어요.</p>
                            </div>
                        </div>

                        <button
                            onClick={() => setView('create')}
                            className="w-full mt-8 py-4 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 shadow-lg shadow-primary-200 transition-all flex justify-center items-center gap-2 group"
                        >
                            "블록 보험" 만들기
                            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                        </button>
                    </div>
                </div>
            )}

            {/* ----------------- CREATE VIEW ----------------- */}
            {!hasSubscription && view === 'create' && (
                <div className="max-w-xl mx-auto pt-12 animate-in zoom-in-95 duration-300">
                    <div className="bg-white p-8 rounded-3xl shadow-2xl glass-panel text-center">
                        <h2 className="text-2xl font-bold text-slate-800 mb-2">얼마만큼 혜택을 받고 싶으신가요?</h2>
                        <p className="text-slate-500 mb-8">목표 금액을 설정하면 그에 맞는 블록 판이 생성됩니다.</p>

                        <form onSubmit={handleCreateBlock} className="space-y-6">
                            <div className="flex items-center justify-center gap-4 text-3xl font-black text-slate-800">
                                <span>저는 보험을</span>
                                <div className="relative w-32 border-b-4 border-slate-200 focus-within:border-primary-500 transition-colors">
                                    <input
                                        type="number"
                                        required
                                        value={targetAmount}
                                        onChange={(e) => setTargetAmount(e.target.value)}
                                        className="w-full text-center bg-transparent outline-none pb-1 text-primary-600"
                                        placeholder="0"
                                    />
                                </div>
                                <span>만원만큼</span>
                            </div>
                            <p className="text-xl font-bold text-slate-800">혜택 받고 싶어요.</p>

                            <button
                                type="submit"
                                className="w-full mt-8 py-4 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 transition-all"
                            >
                                확인
                            </button>

                            <button
                                type="button"
                                onClick={() => setView('prologue')}
                                className="text-slate-400 hover:text-slate-600 text-sm font-medium"
                            >
                                취소
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* ----------------- GRID MAKER VIEW ----------------- */}
            {!hasSubscription && view === 'grid-maker' && (
                <div className="space-y-6 animate-in fade-in duration-500 h-full flex flex-col">
                    <div className="flex justify-between items-end">
                        <div>
                            <h2 className="text-2xl font-bold text-slate-800">블록 맞추기</h2>
                            <p className="text-slate-500">원하는 카테고리의 블록을 드래그하여 채워주세요.</p>
                        </div>
                        <div className="flex items-center gap-2 bg-white px-4 py-2 rounded-xl shadow-sm border border-slate-200">
                            <span className="text-sm font-bold text-slate-600">목표:</span>
                            <span className="text-xl font-black text-primary-600">{targetAmount}만원</span>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                        {/* Grid Area */}
                        <div className="lg:col-span-2 bg-white p-6 rounded-3xl shadow-sm border border-slate-200 min-h-[500px] flex flex-col">
                            <div className="bg-slate-100 rounded-2xl flex-1 border-2 border-dashed border-slate-300 p-8 grid grid-cols-5 gap-2 content-start">
                                {/* Dummy Grid Cells based on targetAmount (e.g. 5 cells) */}
                                {Array.from({ length: Math.min(Number(targetAmount) || 5, 20) }).map((_, i) => (
                                    <div key={i} className="aspect-square bg-white/50 rounded-xl border border-slate-200 flex items-center justify-center">
                                        <Plus className="w-6 h-6 text-slate-300" />
                                    </div>
                                ))}
                            </div>

                            <div className="mt-6 flex justify-between items-center">
                                <div className="flex items-center gap-2 text-sm text-slate-500 font-medium">
                                    <Info className="w-4 h-4" /> 남은 칸을 모두 채워주세요
                                </div>
                                <button
                                    onClick={() => setHasSubscription(true)}
                                    className="px-8 py-3 bg-primary-600 text-white font-bold rounded-xl shadow-lg hover:bg-primary-700 transition-colors"
                                >
                                    블록 보험 구독하기
                                </button>
                            </div>
                        </div>

                        {/* Selection Area */}
                        <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 flex flex-col gap-6">
                            <h3 className="font-bold text-slate-800 text-lg flex items-center gap-2">
                                보험 카테고리 <span className="text-xs font-normal px-2 py-0.5 bg-slate-100 rounded-full text-slate-500">선택</span>
                            </h3>

                            <div className="space-y-4">
                                {categories.map((cat) => (
                                    <details key={cat.id} className="group">
                                        <summary className="flex items-center justify-between p-4 bg-slate-50 rounded-xl cursor-pointer hover:bg-slate-100 transition-colors list-none font-bold text-slate-700">
                                            {cat.name}
                                            <ArrowRight className="w-4 h-4 text-slate-400 group-open:rotate-90 transition-transform" />
                                        </summary>
                                        <div className="p-4 space-y-3">
                                            {[1, 3, 5].map((price) => (
                                                <div key={price} className={`p-3 border-2 rounded-xl flex items-center justify-between cursor-grab active:cursor-grabbing ${cat.color}`}>
                                                    <div className="flex items-center gap-3">
                                                        <GripHorizontal className="w-4 h-4 opacity-50" />
                                                        <span className="font-bold">{price}만원 상품</span>
                                                    </div>
                                                    <span className="text-xs font-bold bg-white/50 px-2 py-1 rounded-md">{price}칸 차지</span>
                                                </div>
                                            ))}
                                        </div>
                                    </details>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* ----------------- SUBSCRIBED VIEW ----------------- */}
            {hasSubscription && (
                <div className="space-y-6 animate-in slide-in-from-bottom-4">
                    <div className="flex justify-between items-end mb-8">
                        <div>
                            <h2 className="text-2xl font-bold text-slate-800">나의 블록 보험</h2>
                            <p className="text-slate-500 text-sm mt-1">현재 적용 중인 맞춤형 보장 내역입니다.</p>
                        </div>
                        {/* Tabs */}
                        <div className="flex bg-slate-200/60 p-1.5 rounded-xl">
                            <button
                                onClick={() => setView('subscribed-this')}
                                className={`px-4 py-2 text-sm font-bold rounded-lg transition-all ${view !== 'subscribed-next' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                이번달 적용 블록
                            </button>
                            <button
                                onClick={() => setView('subscribed-next')}
                                className={`px-4 py-2 text-sm font-bold rounded-lg transition-all ${view === 'subscribed-next' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                다음달 예약 변경
                            </button>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                        {/* Left Box - Blocks Display */}
                        <div className="bg-white p-6 rounded-3xl shadow-md border border-slate-100 min-h-[400px]">
                            <h3 className="font-bold text-slate-800 mb-6 flex items-center justify-between">
                                <span>내 블록 판</span>
                                <span className="text-sm font-normal text-slate-400">총 5만원</span>
                            </h3>

                            {/* Custom Grid Layout Simulation */}
                            <div className="grid grid-cols-5 grid-rows-2 gap-3 h-48">
                                <div className="col-span-3 bg-rose-100 border border-rose-300 rounded-xl p-3 flex flex-col justify-between text-rose-700">
                                    <span className="font-bold text-sm">질병 (3만)</span>
                                    <ShieldCheck className="w-5 h-5 opacity-50" />
                                </div>
                                <div className="col-span-2 bg-blue-100 border border-blue-300 rounded-xl p-3 flex flex-col justify-between text-blue-700">
                                    <span className="font-bold text-sm">배상 (2만)</span>
                                    <ShieldCheck className="w-5 h-5 opacity-50" />
                                </div>
                            </div>
                        </div>

                        {/* Right Box - Details / Actions */}
                        <div className="bg-white p-6 rounded-3xl shadow-md border border-slate-100">
                            {view !== 'subscribed-next' ? (
                                // This Month View
                                <div>
                                    <h3 className="font-bold text-slate-800 mb-6">보장 상세 내역</h3>
                                    <ul className="space-y-4">
                                        <li className="flex justify-between items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                                            <div className="flex items-center gap-3">
                                                <div className="w-2 h-2 rounded-full bg-rose-500" />
                                                <span className="font-bold text-slate-700">질병 입원 일당 (3만)</span>
                                            </div>
                                            <span className="text-slate-500 font-medium text-sm">최대 3,000만원</span>
                                        </li>
                                        <li className="flex justify-between items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                                            <div className="flex items-center gap-3">
                                                <div className="w-2 h-2 rounded-full bg-blue-500" />
                                                <span className="font-bold text-slate-700">생활 배상 책임 (2만)</span>
                                            </div>
                                            <span className="text-slate-500 font-medium text-sm">최대 1억원</span>
                                        </li>
                                    </ul>
                                </div>
                            ) : (
                                // Next Month Modify View
                                <div className="flex flex-col h-full">
                                    <h3 className="font-bold text-slate-800 mb-4">다음달 보험 수정하기</h3>
                                    <p className="text-sm text-slate-500 mb-6 leading-relaxed">
                                        오른쪽의 카테고리에서 원하는 블록을 드래그하거나 클릭하여 추가하세요.
                                        왼쪽의 적용된 블록을 클릭하면 제거할 수 있습니다.
                                    </p>

                                    {/* Category Selection Mini */}
                                    <div className="flex-1 space-y-3 overflow-y-auto pr-2">
                                        {categories.map((cat) => (
                                            <div key={cat.id} className={`p-3 border rounded-xl flex items-center justify-between cursor-pointer hover:opacity-80 transition-opacity ${cat.color}`}>
                                                <span className="font-bold text-sm">{cat.name}</span>
                                                <Plus className="w-4 h-4" />
                                            </div>
                                        ))}
                                    </div>

                                    <div className="pt-6 mt-4 border-t border-slate-100">
                                        <button
                                            onClick={() => alert("다음달 약관 동의 페이지로 이동합니다.")}
                                            className="w-full py-4 bg-slate-800 text-white font-bold rounded-xl shadow-md hover:bg-slate-900 transition-colors"
                                        >
                                            수정 완료 및 약관 확인
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

        </div>
    );
};

export default BlockInsurancePage;
