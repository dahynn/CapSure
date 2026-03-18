import React, { useState } from 'react';

const DigitalSealStep = ({ onNext }) => {
    const [name, setName] = useState('');
    const [isCreated, setIsCreated] = useState(false);

    const handleCreateSeal = () => {
        if (!name.trim()) {
            alert('이름을 입력해주세요.');
            return;
        }
        setIsCreated(true);
    };

    return (
        <div className="flex flex-col h-full animate-in fade-in duration-500 w-full pt-8 pb-4">
            
            {/* 상단 텍스트 */}
            <div className="text-center mb-12">
                <h1 className="text-3xl font-bold text-white mb-4 leading-tight">
                    간편한 서명을 위한<br/>디지털 인장을 만드세요
                </h1>
                <p className="text-sm" style={{ color: 'var(--color-brand-gray)' }}>
                    인증된 디지털 신원으로 문서에 즉시 서명하세요.
                </p>
            </div>

            {/* 메인 비주얼 (빛나는 원형 인장) */}
            <div className="flex-1 flex flex-col items-center justify-center mb-12">
                <div className="relative w-48 h-48 flex items-center justify-center">
                    {/* 외부 테두리 (점선 + 네온 효과) */}
                    <div className="absolute inset-0 rounded-full border-[3px] border-dashed opacity-50 transition-all duration-700" 
                         style={{ 
                             borderColor: isCreated ? 'var(--color-brand-blue)' : '#1E2A40',
                             boxShadow: isCreated ? '0 0 40px rgba(130, 216, 252, 0.2)' : 'none'
                         }}>
                    </div>
                    
                    {/* 내부 원 */}
                    <div className="absolute inset-4 rounded-full" 
                         style={{ 
                             background: 'radial-gradient(circle, rgba(30,42,64,0.3) 0%, rgba(19,27,46,0) 70%)'
                         }}>
                    </div>

                    {/* 아이콘 및 텍스트 */}
                    <div className="relative flex flex-col items-center justify-center z-10 transition-transform duration-500"
                         style={{ transform: isCreated ? 'scale(1.1)' : 'scale(1)' }}>
                        
                        <div className="mb-3 transition-colors duration-500" style={{ color: isCreated ? 'var(--color-brand-blue)' : 'var(--color-brand-purple)' }}>
                            <svg width="36" height="42" viewBox="0 0 24 28" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                                <polyline points="9 12 11 14 15 10"></polyline>
                            </svg>
                        </div>
                        
                        <span className="text-base text-white tracking-widest relative">
                            {name ? `${name}의 인장` : '나의 인장'}
                            <div className="absolute -bottom-2 left-0 right-0 h-px bg-slate-600 transition-all duration-500"
                                 style={{ backgroundColor: isCreated ? 'var(--color-brand-blue)' : '#475569' }}></div>
                        </span>
                    </div>
                </div>
            </div>

            {/* 하단 폼 및 버튼 영역 */}
            <div className="w-full space-y-4">
                {/* 이름 입력 */}
                <div>
                    <label className="block text-xs font-medium mb-3 pl-1" style={{ color: 'var(--color-brand-gray)' }}>
                        인장 생성을 위해 이름을 입력하세요
                    </label>
                    <input
                        type="text"
                        value={name}
                        onChange={(e) => {
                            setName(e.target.value);
                            setIsCreated(false); // 이름 변경 시 생성 상태 초기화
                        }}
                        placeholder="예: 홍길동"
                        className="w-full px-5 py-4 rounded-2xl text-white placeholder-slate-500 text-sm outline-none transition-all"
                        style={{
                            backgroundColor: '#0B1120',
                            border: '1px solid #1E2A40',
                        }}
                        onFocus={e => e.target.style.boxShadow = '0 0 0 2px var(--color-brand-blue)'}
                        onBlur={e => e.target.style.boxShadow = 'none'}
                    />
                </div>

                {/* 인장 생성하기 버튼 */}
                {!isCreated && (
                    <button
                        onClick={handleCreateSeal}
                        className="w-full py-4 rounded-2xl font-bold text-base transition-transform active:scale-95 text-slate-900 mt-2"
                        style={{
                            backgroundImage: 'linear-gradient(to right, var(--color-brand-blue), var(--color-brand-purple))',
                        }}
                    >
                        인장 생성하기
                    </button>
                )}

                {/* 다음 버튼 (생성 완료 후에만 표시되거나, 와이어프레임처럼 같이 보이거나.. 와이어프레임엔 진행을 위해 노출) */}
                {isCreated && (
                    <button
                        onClick={onNext}
                        className="w-full py-4 rounded-2xl font-bold text-base transition-transform active:scale-95 flex items-center justify-center gap-2 text-slate-900 mt-2"
                        style={{ backgroundColor: 'var(--color-brand-yellow)' }}
                    >
                        다음
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <polyline points="9 18 15 12 9 6"></polyline>
                        </svg>
                    </button>
                )}
            </div>
            
        </div>
    );
};

export default DigitalSealStep;
