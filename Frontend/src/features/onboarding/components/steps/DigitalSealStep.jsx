import React from 'react';
import { Fingerprint } from 'lucide-react';

const DigitalSealStep = ({ onNext }) => {
    return (
        <div className="flex flex-col h-full animate-in fade-in slide-in-from-right-8 duration-300">
            <div className="flex-1 flex flex-col items-center justify-center text-center px-4">
                <div className="w-24 h-24 bg-primary-50 rounded-full flex items-center justify-center mb-6">
                    <Fingerprint className="w-12 h-12 text-primary-600" />
                </div>
                
                <h2 className="text-2xl font-bold text-slate-800 mb-2">
                    전자 서명 만들기
                </h2>
                <p className="text-slate-500 mb-8 max-w-[280px]">
                    안전하고 간편한 보험 가입을 위해<br/>
                    나만의 전자 서명이 필요해요
                </p>
                
                {/* 
                  현재 기획이 완벽하지 않으므로 버튼 클릭 시 
                  생성 완료되었다고 가정하고 다음으로 넘어감 
                */}
                <button
                    onClick={onNext}
                    className="w-full max-w-xs py-4 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 shadow-lg shadow-primary-200 transition-all flex justify-center items-center"
                >
                    서명 생성하기
                </button>
            </div>
        </div>
    );
};

export default DigitalSealStep;
