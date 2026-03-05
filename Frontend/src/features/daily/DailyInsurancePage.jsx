import React, { useState } from 'react';
import { Zap, Car, Map, Dumbbell, ShieldAlert, ChevronRight, CheckCircle2, Navigation } from 'lucide-react';

const DailyInsurancePage = () => {
    const [view, setView] = useState('list'); // list, quick-join-1, quick-join-2, quick-join-3, success
    const [activeTab, setActiveTab] = useState('1만원'); // 5천원, 1만원, 3만원

    // Dummy Data
    const quickJoins = [
        { title: '엄마차 자동차 일일보험', type: '자동차', icon: Car, bg: 'bg-blue-100' },
        { title: '제주도 2박3일 여행보험', type: '여행', icon: Map, bg: 'bg-emerald-100' }
    ];

    const categories = [
        { title: '자동차 일일 보험', icon: Car },
        { title: '여행자 보험', icon: Map },
        { title: '스포츠 보험', icon: Dumbbell },
    ];

    const handleQuickJoin = () => {
        setView('quick-join-1');
    };

    return (
        <div className="p-6 md:p-8 pb-24 min-h-screen bg-slate-50">

            {/* ----------------- LIST VIEW ----------------- */}
            {view === 'list' && (
                <div className="max-w-4xl mx-auto space-y-10 animate-in fade-in duration-500">

                    {/* Quick Join Section */}
                    <section>
                        <div className="flex items-center gap-2 mb-4">
                            <Zap className="w-6 h-6 text-amber-500 fill-amber-500" />
                            <h2 className="text-xl font-bold text-slate-800">나의 퀵 가입 보험</h2>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {quickJoins.map((item, idx) => (
                                <div key={idx} className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between group hover:shadow-md transition-shadow">
                                    <div className="flex items-center gap-4">
                                        <div className={`p-3 rounded-xl ${item.bg}`}>
                                            <item.icon className="w-6 h-6 text-slate-700" />
                                        </div>
                                        <div>
                                            <h3 className="font-bold text-slate-800">{item.title}</h3>
                                            <p className="text-sm text-slate-500">{item.type} • 즐겨찾기</p>
                                        </div>
                                    </div>
                                    <button
                                        onClick={handleQuickJoin}
                                        className="px-4 py-2 bg-slate-900 text-white text-sm font-bold rounded-lg hover:bg-slate-800 transition-colors"
                                    >
                                        퀵 가입
                                    </button>
                                </div>
                            ))}
                        </div>
                    </section>

                    {/* Daily Insurance Categories */}
                    <section>
                        <h2 className="text-xl font-bold text-slate-800 mb-4">일일 보험 둘러보기</h2>
                        <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-hide">
                            {categories.map((cat, idx) => (
                                <button
                                    key={idx}
                                    className="min-w-[140px] flex-shrink-0 bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex flex-col items-center gap-3 hover:border-primary-500 transition-colors"
                                >
                                    <cat.icon className="w-8 h-8 text-primary-500" />
                                    <span className="font-bold text-slate-700 text-sm">{cat.title}</span>
                                </button>
                            ))}
                        </div>
                    </section>

                    {/* Block Daily Products by Price Tabs */}
                    <section className="bg-white rounded-3xl p-6 shadow-md border border-slate-100">
                        <h2 className="text-lg font-bold text-slate-800 mb-6">금액대별 맞춤 블록 일일보험</h2>

                        <div className="flex bg-slate-100 p-1.5 rounded-xl mb-6">
                            {['5천원', '1만원', '3만원'].map(tab => (
                                <button
                                    key={tab}
                                    onClick={() => setActiveTab(tab)}
                                    className={`flex-1 py-2.5 text-sm font-bold rounded-lg transition-all ${activeTab === tab ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                                >
                                    {tab}
                                </button>
                            ))}
                        </div>

                        <div className="space-y-4 animate-in slide-in-from-right-4">
                            {[1, 2, 3].map(i => (
                                <div key={i} className="flex flex-col md:flex-row justify-between items-start md:items-center p-5 rounded-2xl border border-slate-100 hover:border-primary-200 transition-colors gap-4">
                                    <div className="flex items-center gap-4">
                                        <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center font-black text-slate-400">
                                            사
                                        </div>
                                        <div>
                                            <h4 className="font-bold text-slate-800">{activeTab} 안심 보장팩 {i}</h4>
                                            <p className="text-sm text-slate-500 line-clamp-1">교통사고 처리 1억 • 자동차 손해 1억 • 대인/대물</p>
                                        </div>
                                    </div>
                                    <button className="w-full md:w-auto px-6 py-2 bg-primary-50 text-primary-600 font-bold rounded-xl hover:bg-primary-100 transition-colors">
                                        상세보기
                                    </button>
                                </div>
                            ))}
                        </div>
                    </section>
                </div>
            )}

            {/* ----------------- QUICK JOIN FLOW ----------------- */}

            {/* Step 1: Verification */}
            {view === 'quick-join-1' && (
                <div className="max-w-md mx-auto mt-12 bg-white p-8 rounded-3xl shadow-xl border border-slate-100 animate-in slide-in-from-right-8 text-center">
                    <ShieldAlert className="w-16 h-16 text-primary-500 mx-auto mb-6" />
                    <h2 className="text-2xl font-bold text-slate-800 mb-2">본인 인증</h2>
                    <p className="text-slate-500 mb-8">안전한 가입을 위해 간편 본인인증을 진행합니다.</p>
                    <button
                        onClick={() => setView('quick-join-2')}
                        className="w-full py-4 bg-primary-600 text-white font-bold rounded-xl shadow-lg hover:bg-primary-700 transition-colors"
                    >
                        PASS로 인증하기
                    </button>
                    <button
                        onClick={() => setView('list')}
                        className="w-full mt-4 py-4 text-slate-500 font-bold hover:text-slate-700"
                    >
                        취소
                    </button>
                </div>
            )}

            {/* Step 2: Terms */}
            {view === 'quick-join-2' && (
                <div className="max-w-md mx-auto mt-12 bg-white p-8 rounded-3xl shadow-xl border border-slate-100 animate-in slide-in-from-right-8">
                    <h2 className="text-2xl font-bold text-slate-800 mb-6">가입 약관 동의</h2>

                    <div className="space-y-4 mb-8">
                        {[1, 2, 3].map(i => (
                            <label key={i} className="flex items-start gap-3 p-4 bg-slate-50 rounded-xl cursor-pointer">
                                <input type="checkbox" defaultChecked className="mt-1 w-5 h-5 accent-primary-600" />
                                <span className="text-sm text-slate-700">
                                    <span className="font-bold">[필수]</span> 제 {i}조 자동차 일일보험 특별약관 동의
                                </span>
                            </label>
                        ))}
                    </div>

                    <button
                        onClick={() => setView('quick-join-3')}
                        className="w-full py-4 bg-primary-600 text-white font-bold rounded-xl shadow-lg hover:bg-primary-700 transition-colors"
                    >
                        모두 동의하고 다음으로
                    </button>
                </div>
            )}

            {/* Step 3: Confirm & Pay */}
            {view === 'quick-join-3' && (
                <div className="max-w-md mx-auto mt-12 bg-white p-8 rounded-3xl shadow-xl border border-slate-100 animate-in slide-in-from-right-8">
                    <h2 className="text-2xl font-bold text-slate-800 mb-6">가입 정보 확인</h2>

                    <div className="bg-slate-50 rounded-2xl p-6 mb-8 space-y-4">
                        <div className="flex justify-between">
                            <span className="text-slate-500 font-medium">자동차 정보</span>
                            <span className="font-bold text-slate-800">12가 3456</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-slate-500 font-medium">피보험자</span>
                            <span className="font-bold text-slate-800">김블록</span>
                        </div>
                        <div className="h-px bg-slate-200" />
                        <div className="flex justify-between text-lg">
                            <span className="font-bold text-slate-800">총 결제 금액</span>
                            <span className="font-black text-primary-600">10,000원</span>
                        </div>
                    </div>

                    <button
                        onClick={() => {
                            if (window.confirm("2026년 02월 27일 14시 00분부터 보험이 적용됩니다. 진행하시겠습니까?")) {
                                setView('success');
                            }
                        }}
                        className="w-full py-4 bg-slate-900 text-white font-bold rounded-xl shadow-lg hover:bg-slate-800 transition-colors"
                    >
                        결제하기
                    </button>
                </div>
            )}

            {/* Step 4: Success */}
            {view === 'success' && (
                <div className="max-w-md mx-auto mt-20 text-center animate-in zoom-in duration-500">
                    <div className="w-24 h-24 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-6">
                        <CheckCircle2 className="w-12 h-12 text-emerald-500" />
                    </div>
                    <h2 className="text-3xl font-black text-slate-800 mb-2">결제 완료</h2>
                    <p className="text-slate-500 mb-8">보험 가입이 정상적으로 완료되었습니다.</p>

                    <button
                        onClick={() => setView('list')}
                        className="w-full py-4 bg-slate-100 text-slate-700 font-bold rounded-xl hover:bg-slate-200 transition-colors"
                    >
                        목록으로 돌아가기
                    </button>
                </div>
            )}

        </div>
    );
};

export default DailyInsurancePage;
