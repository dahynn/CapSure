import React, { useState } from 'react';
import { User, ShieldAlert, CreditCard, LogOut, FileText, Settings, HeartPulse, ChevronRight, Activity } from 'lucide-react';

const Profile = () => {
    // Dummy state for user info
    const [userInfo, setUserInfo] = useState({
        name: '캡슐러',
        email: 'capsure@cover.com',
        phone: '010-1234-5678'
    });

    // MyData agreement toggle
    const [myDataAgreed, setMyDataAgreed] = useState(true);

    return (
        <div className="max-w-4xl mx-auto px-6 py-12 animate-in fade-in slide-in-from-bottom-4">
            <h1 className="text-3xl font-black text-slate-900 mb-8">마이페이지</h1>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                {/* Left Section - Quick Info */}
                <div className="md:col-span-1 space-y-6">
                    <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 flex flex-col items-center text-center">
                        <div className="w-24 h-24 bg-primary-100 text-primary-600 rounded-full flex items-center justify-center mb-4">
                            <User className="w-12 h-12" />
                        </div>
                        <h2 className="text-xl font-bold text-slate-800">{userInfo.name}님</h2>
                        <p className="text-sm text-slate-500 mt-1">{userInfo.email}</p>
                        <p className="text-sm text-slate-500">{userInfo.phone}</p>

                        <button className="mt-6 w-full py-2.5 bg-slate-100 text-slate-600 font-bold rounded-xl hover:bg-slate-200 transition-colors">
                            개인정보 수정
                        </button>
                    </div>

                    <div className="bg-primary-50 p-6 rounded-3xl border border-primary-100 flex items-center gap-4">
                        <div className="w-12 h-12 bg-white rounded-xl flex items-center justify-center text-primary-500 shadow-sm">
                            <HeartPulse className="w-6 h-6" />
                        </div>
                        <div>
                            <p className="text-sm font-semibold text-primary-600">건강 점수 연동 중</p>
                            <p className="text-xs text-slate-500 mt-0.5">최근 업데이트: 오늘</p>
                        </div>
                    </div>
                </div>

                {/* Right Section - Settings & Menus */}
                <div className="md:col-span-2 space-y-6">

                    {/* MyData Toggle Setting */}
                    <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200">
                        <h3 className="text-lg font-bold text-slate-800 mb-4 flex items-center gap-2">
                            <Activity className="w-5 h-5 text-primary-500" />
                            마이데이터 활용 설정
                        </h3>
                        <div className="flex items-center justify-between p-4 bg-slate-50 rounded-2xl border border-slate-100">
                            <div>
                                <p className="font-bold text-slate-700">마이데이터 연결 동의</p>
                                <p className="text-sm text-slate-500 mt-1">
                                    건강/금융 데이터를 연동하여 맞춤형 보험 분석을 받습니다.
                                </p>
                            </div>
                            <button
                                onClick={() => setMyDataAgreed(!myDataAgreed)}
                                className={`relative w-14 h-8 rounded-full transition-colors ${myDataAgreed ? 'bg-primary-500' : 'bg-slate-300'}`}
                            >
                                <div className={`absolute top-1 w-6 h-6 bg-white rounded-full transition-transform ${myDataAgreed ? 'left-7' : 'left-1'}`} />
                            </button>
                        </div>
                    </div>

                    {/* Menu List */}
                    <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden divide-y divide-slate-100">
                        <button className="w-full flex items-center justify-between p-6 hover:bg-slate-50 transition-colors">
                            <div className="flex items-center gap-4">
                                <div className="w-10 h-10 bg-blue-50 text-blue-500 rounded-xl flex items-center justify-center">
                                    <CreditCard className="w-5 h-5" />
                                </div>
                                <div className="text-left">
                                    <p className="font-bold text-slate-700">결제 수단 관리</p>
                                    <p className="text-xs text-slate-500">등록된 카드: 토스페이</p>
                                </div>
                            </div>
                            <ChevronRight className="w-5 h-5 text-slate-400" />
                        </button>

                        <button className="w-full flex items-center justify-between p-6 hover:bg-slate-50 transition-colors">
                            <div className="flex items-center gap-4">
                                <div className="w-10 h-10 bg-slate-100 text-slate-500 rounded-xl flex items-center justify-center">
                                    <ShieldAlert className="w-5 h-5" />
                                </div>
                                <div className="text-left">
                                    <p className="font-bold text-slate-700">앱 설정 및 알림</p>
                                    <p className="text-xs text-slate-500">푸쉬 알림 ON</p>
                                </div>
                            </div>
                            <ChevronRight className="w-5 h-5 text-slate-400" />
                        </button>
                    </div>

                    {/* Danger Zone */}
                    <div className="pt-8 flex justify-end">
                        <button className="text-sm font-medium text-slate-400 hover:text-red-500 transition-colors">
                            회원 탈퇴
                        </button>
                    </div>

                </div>
            </div>
        </div>
    );
};

export default Profile;
