import React, { useState } from 'react';
import { Zap, Car, Map, Dumbbell, ShieldAlert, ChevronRight, CheckCircle2, Navigation, Compass, HeartPulse } from 'lucide-react';
import DailyInsuranceQuickJoinItem from './components/DailyInsuranceQuickJoinItem';
import DailyInsuranceTabPanel from './components/DailyInsuranceTabPanel';

const DailyInsurancePage = () => {
    const [view, setView] = useState('list'); // list, quick-join-1, quick-join-2, quick-join-3, success
    const [activeTab, setActiveTab] = useState('원데이 자동차');

    // Dummy Data
    const favoriteInsurances = [
        { title: '엄마차 자동차 일일보험', type: '자동차 • 즐겨찾기', icon: Car, bg: 'bg-blue-100' },
    ];

    const recentInsurances = [
        { title: '제주도 2박3일 여행보험', type: '여행 • 최근 가입', icon: Map, bg: 'bg-emerald-100' },
        { title: '자전거 국토종주 보험', type: '레저 • 최근 가입', icon: Dumbbell, bg: 'bg-purple-100' },
    ];

    const categoryTabs = ['원데이 자동차', '레저/스포츠', '산책/외출 펫', '단기 여행자', '미니 상해'];

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
                        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-8">
                            <div className="flex items-center gap-2 mb-6">
                                <Zap className="w-6 h-6 text-amber-500 fill-amber-500" />
                                <h2 className="text-2xl font-bold text-slate-800">원클릭 보험 가입</h2>
                            </div>

                            <div>
                                <h3 className="text-lg font-bold text-slate-800 mb-4 text-left">내가 즐겨찾기한 일일 보험</h3>
                                <div className="flex flex-col gap-3">
                                    {favoriteInsurances.map((item, idx) => (
                                        <DailyInsuranceQuickJoinItem key={idx} item={item} onClick={handleQuickJoin} />
                                    ))}
                                </div>
                            </div>

                            <div className="h-px bg-slate-100 w-full" />

                            <div>
                                <h3 className="text-lg font-bold text-slate-800 mb-4 text-left">최근에 가입한 일일보험</h3>
                                <div className="flex flex-col gap-3">
                                    {recentInsurances.map((item, idx) => (
                                        <DailyInsuranceQuickJoinItem key={idx} item={item} onClick={handleQuickJoin} />
                                    ))}
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* Daily Insurance Categories */}
                    <section>
                        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6">
                            <div className="flex items-center gap-2 mb-6">
                                <Compass className="w-6 h-6 text-primary-500" />
                                <h2 className="text-2xl font-bold text-slate-800">일일 보험 둘러보기</h2>
                            </div>

                            {/* Grouping container for tabs and panel */}
                            <div className="rounded-2xl border border-slate-200 overflow-hidden">
                                <div className="flex bg-slate-50 p-1.5 overflow-x-auto scrollbar-hide border-b border-slate-200">
                                    {categoryTabs.map((tab) => (
                                        <button
                                            key={tab}
                                            onClick={() => setActiveTab(tab)}
                                            className={`flex-1 min-w-[120px] py-2.5 text-sm rounded-xl transition-all ${activeTab === tab
                                                ? 'bg-white text-primary-600 shadow-sm font-bold border border-slate-100'
                                                : 'text-slate-500 hover:text-slate-700 font-medium'
                                                }`}
                                        >
                                            <h3 className="m-0 text-inherit font-inherit">{tab}</h3>
                                        </button>
                                    ))}
                                </div>

                                <DailyInsuranceTabPanel tabName={activeTab} />
                            </div>
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
                            <span className="font-bold text-slate-800">김캡슐</span>
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
