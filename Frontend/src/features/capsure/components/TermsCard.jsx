import React from 'react';
import { Info, Check, FileText } from 'lucide-react';

const TermsCard = ({ product, termsList, isChecked, onToggle }) => {
    const companyDisplayName = product.companyName || product.company || '보험사';
    return (
        <div className="bg-[#192235] border border-slate-800/80 rounded-2xl p-5 mb-4 shadow-sm relative">
            {/* Top Info Icon */}
            <div className="absolute top-5 right-5 text-slate-400 cursor-pointer hover:text-white transition-colors">
                <Info className="w-5 h-5" />
            </div>

            {/* Header: Icon, Name, Badge */}
            <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-xl bg-[#2A3142] flex items-center justify-center flex-shrink-0">
                    <div className="grid grid-cols-2 gap-[2px]">
                        <div className="w-1.5 h-1.5 bg-brand-blue rounded-sm" />
                        <div className="w-1.5 h-1.5 bg-brand-blue rounded-sm" />
                        <div className="w-1.5 h-1.5 bg-brand-blue rounded-sm" />
                        <div className="w-1.5 h-1.5 bg-brand-blue rounded-sm" />
                    </div>
                </div>
                <div className="flex flex-col items-start gap-1">
                    <h3 className="text-white font-bold text-base leading-none">{companyDisplayName}</h3>
                    <div className="flex items-center gap-1 bg-[#2C213B] px-2 py-0.5 rounded-full">
                        <span className="text-[10px]">✨</span>
                        <span className="text-[#A78BFA] text-[10px] font-bold">AI 보장 요약</span>
                    </div>
                </div>
            </div>

            {/* Terms List */}
            <ul className="flex flex-col gap-4 mb-6">
                {termsList.map((term, idx) => (
                    <li key={idx} className="flex items-start gap-3">
                        <div className="mt-0.5 rounded-full bg-brand-blue text-[#020715] flex-shrink-0 p-[2px]">
                            <Check className="w-3 h-3" strokeWidth={3} />
                        </div>
                        <p className="text-slate-300 text-sm leading-snug font-medium break-keep">
                            {term}
                        </p>
                    </li>
                ))}
            </ul>

            {/* Bottom Actions */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-700/50 mt-1">
                <button className="flex items-center gap-1.5 text-brand-blue hover:text-[#6BC1E6] transition-colors">
                    <FileText className="w-4 h-4" />
                    <span className="text-sm font-bold">상세 약관 보기</span>
                </button>

                <div className="flex items-center gap-2 cursor-pointer" onClick={onToggle}>
                    <span className="text-slate-400 text-sm">약관 동의</span>
                    <div className={`w-5 h-5 rounded-[6px] border flex items-center justify-center transition-all ${isChecked ? 'bg-slate-700 border-slate-600' : 'border-slate-600'}`}>
                        {isChecked && <Check className="w-3.5 h-3.5 text-white" strokeWidth={3} />}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TermsCard;
