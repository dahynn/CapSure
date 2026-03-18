import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import * as Icons from 'lucide-react';
import { getInsuranceDetail } from './api/searchInsurance.api';

const DynamicIcon = ({ name, className }) => {
    const IconComponent = Icons[name] || Icons.HelpCircle;
    return <IconComponent className={className} />;
};

const InsuranceDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [detail, setDetail] = useState(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchDetail = async () => {
            setIsLoading(true);
            try {
                const data = await getInsuranceDetail(id);
                setDetail(data);
            } catch (error) {
                console.error("Failed to fetch detail:", error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchDetail();
    }, [id]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[500px]">
                <Icons.Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
            </div>
        );
    }

    if (!detail) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[500px] gap-4">
                <span className="text-slate-500">보험 상세 정보를 불러올 수 없습니다.</span>
                <button onClick={() => navigate(-1)} className="px-4 py-2 bg-slate-100 text-slate-700 rounded-xl font-bold">뒤로 가기</button>
            </div>
        );
    }

    return (
        <div className="p-6 md:p-8 pb-24 min-h-screen bg-white">
            <button
                onClick={() => navigate(-1)}
                className="mb-8 flex items-center gap-2 text-slate-500 hover:text-slate-800 transition-colors"
            >
                <Icons.ArrowLeft className="w-5 h-5" /> 뒤로가기
            </button>

            <div className="max-w-5xl mx-auto flex flex-col lg:flex-row gap-12">
                {/* Left Title Area */}
                <div className="lg:w-1/3">
                    <h1 className="text-3xl lg:text-4xl font-black text-green-700 mb-2 leading-tight">
                        {detail.title}
                    </h1>
                    <h2 className="text-2xl lg:text-3xl font-bold text-slate-800 mb-4 leading-tight">
                        {detail.subtitle}
                    </h2>
                    <p className="text-lg text-slate-500">
                        {detail.subscript}
                    </p>
                </div>

                {/* Right Content Area */}
                <div className="lg:w-2/3 flex flex-col gap-8">
                    {/* Coverage Cards Outline Container */}
                    <div className="border border-slate-200 rounded-3xl p-8 bg-white shadow-sm flex flex-col md:flex-row gap-8 justify-between items-start">
                        {detail.coverages?.map((cov, idx) => (
                            <div key={idx} className="flex-1 flex flex-col items-center text-center">
                                {/* Icon Circle */}
                                <div className={`w-20 h-20 rounded-full flex items-center justify-center mb-4 ${cov.iconBg}`}>
                                    <DynamicIcon name={cov.icon} className={`w-10 h-10 ${cov.iconColor}`} />
                                </div>
                                <div className={`text-sm font-bold mb-4 px-3 py-1 rounded-full ${cov.iconColor} bg-slate-50/50`}>
                                    {cov.title}
                                </div>

                                <div className="font-bold text-slate-800 text-lg mb-2 whitespace-pre-line">
                                    {cov.mainText}
                                </div>

                                {cov.subText && (
                                    <div className="relative mb-2 w-full flex flex-col items-center">
                                        <div className="w-6 h-6 rounded-full bg-slate-800 text-white flex items-center justify-center -mt-3 mb-2 absolute top-0">
                                            <Icons.Plus className="w-4 h-4" />
                                        </div>
                                        <div className="pt-6 font-bold text-slate-700 text-sm whitespace-pre-line">
                                            {cov.subText}
                                        </div>
                                    </div>
                                )}

                                <div className="text-slate-500 text-sm font-medium mt-auto pt-2 border-t border-slate-100 w-full mt-4">
                                    {cov.detailText}
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* AI Summary List */}
                    <div className="pl-2">
                        <ul className="space-y-4 mb-4">
                            {detail.aiSummary?.map((item, idx) => (
                                <li key={idx} className="flex items-start gap-3">
                                    <span className="w-1.5 h-1.5 rounded-full bg-slate-400 mt-2 shrink-0" />
                                    <span className="text-slate-600 font-medium leading-relaxed">{item}</span>
                                </li>
                            ))}
                        </ul>
                        <p className="text-sm text-slate-400 font-medium bg-slate-50 p-4 rounded-xl inline-block">
                            {detail.aiNote}
                        </p>
                    </div>
                </div>
            </div>

            {/* Floating Action Buttons */}
            <div className="fixed bottom-8 right-8 flex flex-col gap-3">
                <button className="flex items-center gap-2 bg-slate-800 text-white px-6 py-3 rounded-full shadow-lg hover:bg-slate-900 transition-colors font-bold">
                    상품문의 <Icons.MessageSquare className="w-5 h-5 opacity-80" />
                </button>
                <button
                    onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
                    className="self-end w-12 h-12 bg-white border border-slate-200 text-slate-600 rounded-full shadow-md flex items-center justify-center hover:bg-slate-50 transition-colors"
                >
                    <Icons.ArrowUp className="w-5 h-5" />
                </button>
            </div>
        </div>
    );
};

export default InsuranceDetailPage;
