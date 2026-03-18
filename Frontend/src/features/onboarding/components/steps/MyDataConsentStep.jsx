import React, { useState } from 'react';

const MyDataConsentStep = ({ onNext }) => {
    // 항목별 동의 상태
    const [consents, setConsents] = useState({
        insurance: true,
        health: true,
        asset: true,
    });

    const isAllChecked = Object.values(consents).every(Boolean);

    // 전체 동의 토글
    const handleToggleAll = () => {
        const nextState = !isAllChecked;
        setConsents({
            insurance: nextState,
            health: nextState,
            asset: nextState,
        });
    };

    // 개별 동의 토글
    const handleToggleSingle = (key) => {
        setConsents(prev => ({ ...prev, [key]: !prev[key] }));
    };

    return (
        <div className="flex flex-col h-full animate-in fade-in duration-500 pt-2 pb-4 w-full">
            
            {/* 상단 텍스트 */}
            <div className="mb-8">
                <h1 className="text-2xl font-bold text-white mb-4 leading-snug">
                    맞춤형 보험 추천을 위해<br/>데이터를 연결해 주세요
                </h1>
                <p className="text-sm leading-relaxed" style={{ color: 'var(--color-brand-gray)' }}>
                    데이터를 연결하면 고객님의 프로필을 분석하여 최저가로 최적의 보장을 찾아드립니다. 
                    데이터는 암호화되어 은행 수준의 보안으로 안전하게 관리됩니다.
                </p>
            </div>

            {/* 보안 뱃지 */}
            <div className="mb-6">
                <div className="inline-flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium" 
                     style={{ backgroundColor: '#131B2E', border: '1px solid #1E2A40', color: 'var(--color-brand-blue)' }}>
                    <svg width="14" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                    </svg>
                    보안 마이데이터 동기화
                </div>
            </div>

            {/* 동의 항목 리스트 */}
            <div className="space-y-3 mb-8">
                {/* 1. 보험 가입 내역 */}
                <div 
                    onClick={() => handleToggleSingle('insurance')}
                    className="flex justify-between items-center p-4 rounded-2xl cursor-pointer transition-colors"
                    style={{ backgroundColor: '#0B1120', border: '1px solid #1E2A40' }}
                >
                    <div className="flex items-center gap-4">
                        <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ backgroundColor: '#131B2E' }}>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-brand-blue)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="16" y1="13" x2="8" y2="13"></line>
                                <line x1="16" y1="17" x2="8" y2="17"></line>
                                <polyline points="10 9 9 9 8 9"></polyline>
                            </svg>
                        </div>
                        <div>
                            <p className="text-white font-medium text-base mb-0.5">보험 가입 내역</p>
                            <p className="text-xs" style={{ color: 'var(--color-brand-gray)' }}>청구 이력 및 현재 유지 중인 보험</p>
                        </div>
                    </div>
                    {/* 체크박스 디자인 */}
                    <div className={`w-6 h-6 rounded flex items-center justify-center transition-all ${consents.insurance ? 'bg-[#82D8FC]' : 'bg-[#1E2A40]'}`}>
                        {consents.insurance && (
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#020715" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        )}
                    </div>
                </div>

                {/* 2. 건강 기록 */}
                <div 
                    onClick={() => handleToggleSingle('health')}
                    className="flex justify-between items-center p-4 rounded-2xl cursor-pointer transition-colors"
                    style={{ backgroundColor: '#0B1120', border: '1px solid #1E2A40' }}
                >
                    <div className="flex items-center gap-4">
                        <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ backgroundColor: '#131B2E' }}>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-brand-blue)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                            </svg>
                        </div>
                        <div>
                            <p className="text-white font-medium text-base mb-0.5">건강 기록</p>
                            <p className="text-xs" style={{ color: 'var(--color-brand-gray)' }}>건강검진 및 진료 이력</p>
                        </div>
                    </div>
                    {/* 체크박스 디자인 */}
                    <div className={`w-6 h-6 rounded flex items-center justify-center transition-all ${consents.health ? 'bg-[#82D8FC]' : 'bg-[#1E2A40]'}`}>
                        {consents.health && (
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#020715" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        )}
                    </div>
                </div>

                {/* 3. 자산 현황 */}
                <div 
                    onClick={() => handleToggleSingle('asset')}
                    className="flex justify-between items-center p-4 rounded-2xl cursor-pointer transition-colors"
                    style={{ backgroundColor: '#0B1120', border: '1px solid #1E2A40' }}
                >
                    <div className="flex items-center gap-4">
                        <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ backgroundColor: '#131B2E' }}>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-brand-blue)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="4" y="2" width="16" height="20" rx="2" ry="2"></rect>
                                <line x1="12" y1="18" x2="12.01" y2="18"></line>
                                <path d="M8 13h0"></path><path d="M16 13h0"></path><path d="M12 13h0"></path><path d="M8 9h0"></path><path d="M16 9h0"></path><path d="M12 9h0"></path>
                            </svg>
                        </div>
                        <div>
                            <p className="text-white font-medium text-base mb-0.5">자산 현황</p>
                            <p className="text-xs" style={{ color: 'var(--color-brand-gray)' }}>부동산 및 자동차 소유 정보</p>
                        </div>
                    </div>
                    {/* 체크박스 디자인 */}
                    <div className={`w-6 h-6 rounded flex items-center justify-center transition-all ${consents.asset ? 'bg-[#82D8FC]' : 'bg-[#1E2A40]'}`}>
                        {consents.asset && (
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#020715" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        )}
                    </div>
                </div>
            </div>

            {/* 모두 동의 토글 */}
            <div className="flex items-center justify-between mt-4 mb-10 px-2 cursor-pointer" onClick={handleToggleAll}>
                <span className="text-white font-bold">모든 권한에 동의합니다</span>
                <div className={`w-12 h-6 rounded-full transition-colors flex items-center px-1 ${isAllChecked ? 'bg-[#82D8FC]' : 'bg-[#1E2A40]'}`}>
                    <div className={`w-4 h-4 bg-white rounded-full transition-transform ${isAllChecked ? 'translate-x-6' : 'translate-x-0'}`} />
                </div>
            </div>

            {/* 하단 버튼들 */}
            <div className="mt-auto flex flex-col items-center pb-2">
                <button
                    onClick={onNext}
                    className="w-full py-4 rounded-2xl font-bold text-base transition-transform active:scale-95 text-slate-900 mb-6"
                    style={{ backgroundColor: 'var(--color-brand-blue)' }}
                >
                    마이데이터 연결하기
                </button>
                
                <button 
                    onClick={onNext} // 스킵 기능 대용
                    className="text-sm font-medium transition-opacity hover:opacity-70 mb-8" 
                    style={{ color: 'var(--color-brand-gray)' }}
                >
                    다음에 할게요
                </button>

                <p className="text-center text-[10px] sm:text-xs leading-relaxed max-w-xs" style={{ color: '#475569' }}>
                    데이터를 연결함으로써 귀하는 당사의 <span className="underline">개인정보 처리방침</span>에 동의하게 됩니다.
                    앱 설정에서 언제든지 권한을 철회할 수 있습니다.
                </p>
            </div>
            
        </div>
    );
};

export default MyDataConsentStep;
