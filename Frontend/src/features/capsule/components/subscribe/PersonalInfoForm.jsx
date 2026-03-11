import React, { useState } from 'react';
import { ArrowRight } from 'lucide-react';

const PersonalInfoForm = ({ onNext, onPrev }) => {
    const [formData, setFormData] = useState({
        name: '',
        rrnFront: '',
        rrnBack: '',
        phone: ''
    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        // Here you would normally validate the form
        onNext();
    };

    return (
        <div className="max-w-xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4">
            <div className="text-center py-6">
                <h2 className="text-2xl font-bold text-slate-800">개인정보 입력</h2>
                <p className="text-slate-500 mt-2">보험 가입을 위해 정확한 정보를 입력해주세요.</p>
            </div>

            <form onSubmit={handleSubmit} className="bg-white p-8 rounded-3xl shadow-sm border border-slate-200 flex flex-col gap-6">

                {/* Name */}
                <div className="space-y-2">
                    <label className="text-sm font-bold text-slate-700">이름</label>
                    <input
                        type="text"
                        name="name"
                        value={formData.name}
                        onChange={handleChange}
                        required
                        placeholder="홍길동"
                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500 transition-colors"
                    />
                </div>

                {/* Resident Registration Number */}
                <div className="space-y-2">
                    <label className="text-sm font-bold text-slate-700">주민등록번호</label>
                    <div className="flex items-center gap-3">
                        <input
                            type="text"
                            name="rrnFront"
                            maxLength="6"
                            value={formData.rrnFront}
                            onChange={(e) => setFormData({ ...formData, rrnFront: e.target.value.replace(/[^0-9]/g, '') })}
                            required
                            placeholder="YYMMDD"
                            className="flex-1 px-4 py-3 text-center tracking-widest rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500 transition-colors"
                        />
                        <span className="text-slate-400 font-bold">-</span>
                        <input
                            type="password"
                            name="rrnBack"
                            maxLength="7"
                            value={formData.rrnBack}
                            onChange={(e) => setFormData({ ...formData, rrnBack: e.target.value.replace(/[^0-9]/g, '') })}
                            required
                            placeholder="●●●●●●●"
                            className="flex-1 px-4 py-3 text-center tracking-widest rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500 transition-colors"
                        />
                    </div>
                </div>

                {/* Phone */}
                <div className="space-y-2">
                    <label className="text-sm font-bold text-slate-700">휴대폰 번호</label>
                    <input
                        type="tel"
                        name="phone"
                        maxLength="11"
                        value={formData.phone}
                        onChange={(e) => setFormData({ ...formData, phone: e.target.value.replace(/[^0-9]/g, '') })}
                        required
                        placeholder="- 없이 숫자만 입력"
                        className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:border-primary-500 focus:ring-1 focus:ring-primary-500 transition-colors"
                    />
                </div>

                <div className="flex justify-between items-center pt-8 mt-4">
                    <button
                        type="button"
                        onClick={onPrev}
                        className="px-6 py-3 text-slate-500 hover:text-slate-700 font-medium transition-colors"
                    >
                        이전으로
                    </button>
                    <button
                        type="submit"
                        className="px-8 py-3 bg-primary-600 text-white font-bold rounded-xl shadow-lg hover:bg-primary-700 transition-colors flex items-center gap-2 group"
                    >
                        다음
                        <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                    </button>
                </div>
            </form>
        </div>
    );
};

export default PersonalInfoForm;
