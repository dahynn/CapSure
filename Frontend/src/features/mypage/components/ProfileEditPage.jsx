import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, User, Phone, Calendar, Loader2, ChevronDown, CheckCircle2 } from 'lucide-react';
import { getUserProfile, updateUserProfile } from '../api/mypage.api';

const CustomSelect = ({ value, options, onChange }) => {
    const [isOpen, setIsOpen] = useState(false);
    const ref = useRef(null);

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (ref.current && !ref.current.contains(e.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div ref={ref} className="relative w-full">
            <button 
                type="button" 
                onClick={() => setIsOpen(!isOpen)}
                className="w-full bg-transparent border border-slate-700 hover:border-[#82D8FC] rounded-xl py-3 px-4 text-white outline-none transition-colors flex items-center justify-between font-medium"
            >
                <span>{options.find(o => String(o.value) === String(value))?.label}</span>
                <ChevronDown className={`w-4 h-4 text-[#4E5669] transition-transform ${isOpen ? 'rotate-180' : ''}`} />
            </button>
            
            {isOpen && (
                <div className="absolute z-10 w-full mt-2 bg-[#1C212E] border border-slate-700 rounded-xl shadow-2xl max-h-56 overflow-y-auto scrollbar-hide py-1">
                    {options.map((opt) => (
                        <button
                            key={opt.value}
                            type="button"
                            onClick={() => {
                                onChange(opt.value);
                                setIsOpen(false);
                            }}
                            className={`w-full text-left px-4 py-2.5 text-sm transition-colors hover:bg-slate-800 ${String(opt.value) === String(value) ? 'text-[#82D8FC] font-medium' : 'text-[#9D9DA4]'}`}
                        >
                            {opt.label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
};

const ProfileEditPage = () => {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [showSuccess, setShowSuccess] = useState(false);
    const [formData, setFormData] = useState({
        name: '',
        phone: '',
        birthDate: '',
        gender: 'M'
    });

    const currentYear = new Date().getFullYear();
    const yearOptions = Array.from({length: 100}, (_, i) => {
        const y = currentYear - i;
        return { label: `${y}년`, value: String(y) };
    });
    const monthOptions = Array.from({length: 12}, (_, i) => {
        const m = String(i + 1).padStart(2, '0');
        return { label: `${i + 1}월`, value: m };
    });
    const dayOptions = Array.from({length: 31}, (_, i) => {
        const d = String(i + 1).padStart(2, '0');
        return { label: `${i + 1}일`, value: d };
    });

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const data = await getUserProfile().catch(err => {
                    console.warn(err);
                    return {
                        name: '정정교',
                        phone: '010-1234-5678',
                        birthDate: '1990-01-01',
                        gender: 'M'
                    };
                });
                setFormData({
                    name: data.name || '',
                    phone: data.phone || '',
                    birthDate: data.birthDate || '',
                    gender: data.gender || 'M'
                });
            } finally {
                setIsLoading(false);
            }
        };
        fetchProfile();
    }, []);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleDateChange = (type, val) => {
        const parts = (formData.birthDate || '1990-01-01').split('-');
        let [y, m, d] = parts;
        if (type === 'year') y = val;
        if (type === 'month') m = val;
        if (type === 'day') d = val;
        setFormData(prev => ({ ...prev, birthDate: `${y}-${m}-${d}` }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSaving(true);
        try {
            await updateUserProfile(formData).catch(err => {
                console.warn("Backend update failed, using mock log.", err);
            });
            navigate('/mypage', { state: { profileUpdated: true } });
        } catch (error) {
            alert('프로필 수정 중 오류가 발생했습니다.');
            setIsSaving(false);
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-screen text-[#82D8FC]">
                <Loader2 className="w-8 h-8 animate-spin" />
            </div>
        );
    }

    return (
        <div className="px-8 py-8 md:px-12 md:py-10 space-y-8 max-w-[560px] mx-auto w-full min-h-screen animate-in fade-in pb-32">
            {/* Header */}
            <div className="flex items-center gap-4 mb-2">
                <button 
                    onClick={() => navigate('/mypage')} 
                    className="p-2 hover:bg-[#1E2535] rounded-full transition-colors text-white"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-2xl font-bold text-white">프로필 수정</h1>
            </div>
            
            <p className="text-[#9D9DA4] text-sm mb-8 px-2">개인정보를 최신 상태로 유지해주세요.</p>

            {/* Form */}
            <form onSubmit={handleSubmit} className="space-y-6">
                
                {/* Name */}
                <div className="bg-[#141925] p-5 rounded-[24px] border border-slate-800/30">
                    <label className="text-[#9D9DA4] text-xs font-bold mb-2 block flex items-center gap-2">
                        <User className="w-4 h-4" /> 이름 (닉네임)
                    </label>
                    <input 
                        type="text"
                        name="name"
                        value={formData.name}
                        onChange={handleChange}
                        placeholder="이름을 입력하세요"
                        className="w-full bg-transparent border-b border-slate-700 focus:border-[#82D8FC] py-2 text-white outline-none transition-colors"
                        required
                    />
                </div>

                {/* Phone */}
                <div className="bg-[#141925] p-5 rounded-[24px] border border-slate-800/30">
                    <label className="text-[#9D9DA4] text-xs font-bold mb-2 block flex items-center gap-2">
                        <Phone className="w-4 h-4" /> 전화번호
                    </label>
                    <input 
                        type="tel"
                        name="phone"
                        value={formData.phone}
                        onChange={handleChange}
                        placeholder="010-0000-0000"
                        className="w-full bg-transparent border-b border-slate-700 focus:border-[#82D8FC] py-2 text-white outline-none transition-colors"
                        required
                    />
                </div>

                {/* Birth Date (Dropdowns) */}
                <div className="bg-[#141925] p-5 rounded-[24px] border border-slate-800/30">
                    <label className="text-[#9D9DA4] text-xs font-bold mb-4 block flex items-center gap-2">
                        <Calendar className="w-4 h-4" /> 생년월일
                    </label>
                    <div className="flex gap-3">
                        <div className="relative flex-[1.2]">
                            <CustomSelect 
                                options={yearOptions}
                                value={(formData.birthDate || '1990-01-01').split('-')[0]}
                                onChange={(val) => handleDateChange('year', val)}
                            />
                        </div>
                        <div className="relative flex-[0.9]">
                            <CustomSelect 
                                options={monthOptions}
                                value={(formData.birthDate || '1990-01-01').split('-')[1]}
                                onChange={(val) => handleDateChange('month', val)}
                            />
                        </div>
                        <div className="relative flex-[0.9]">
                            <CustomSelect 
                                options={dayOptions}
                                value={(formData.birthDate || '1990-01-01').split('-')[2]}
                                onChange={(val) => handleDateChange('day', val)}
                            />
                        </div>
                    </div>
                </div>

                {/* Gender */}
                <div className="bg-[#141925] p-5 rounded-[24px] border border-slate-800/30">
                    <label className="text-[#9D9DA4] text-xs font-bold mb-4 block">
                        성별
                    </label>
                    <div className="flex gap-4">
                        <label className="flex-1 cursor-pointer">
                            <input 
                                type="radio" 
                                name="gender" 
                                value="M" 
                                checked={formData.gender === 'M'}
                                onChange={handleChange}
                                className="peer sr-only" 
                            />
                            <div className="w-full text-center py-3 rounded-xl border border-slate-700 text-[#9D9DA4] peer-checked:bg-[#82D8FC]/10 peer-checked:text-[#82D8FC] peer-checked:border-[#82D8FC] transition-colors font-medium">
                                남성
                            </div>
                        </label>
                        <label className="flex-1 cursor-pointer">
                            <input 
                                type="radio" 
                                name="gender" 
                                value="F" 
                                checked={formData.gender === 'F'}
                                onChange={handleChange}
                                className="peer sr-only" 
                            />
                            <div className="w-full text-center py-3 rounded-xl border border-slate-700 text-[#9D9DA4] peer-checked:bg-[#82D8FC]/10 peer-checked:text-[#82D8FC] peer-checked:border-[#82D8FC] transition-colors font-medium">
                                여성
                            </div>
                        </label>
                    </div>
                </div>

                {/* Submit Button */}
                <div className="pt-6 pb-10">
                    <button 
                        type="submit" 
                        disabled={isSaving}
                        className="w-full py-4 bg-[#82D8FC] hover:bg-[#6BC4E8] text-[#141925] font-bold rounded-2xl transition-colors disabled:opacity-50 flex items-center justify-center"
                    >
                        {isSaving ? <Loader2 className="w-5 h-5 animate-spin" /> : '변경사항 저장'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default ProfileEditPage;
