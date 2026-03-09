import React from 'react';

const DailyInsuranceQuickJoinItem = ({ item, onClick }) => {
    const Icon = item.icon;
    return (
        <div className="flex items-center justify-between group py-3 px-4 hover:bg-slate-50 transition-colors rounded-xl">
            <div className="flex items-center gap-4">
                <div className={`p-2.5 rounded-xl ${item.bg || 'bg-slate-100'}`}>
                    {Icon && <Icon className="w-5 h-5 text-slate-700" />}
                </div>
                <div>
                    <h4 className="font-bold text-slate-800 text-sm md:text-base">{item.title}</h4>
                    <p className="text-xs md:text-sm text-slate-500">{item.type}</p>
                </div>
            </div>
            <button
                onClick={onClick}
                className="px-4 py-2 bg-slate-900 text-white text-sm font-bold rounded-lg hover:bg-slate-800 transition-colors"
            >
                퀵 가입
            </button>
        </div>
    );
};

export default DailyInsuranceQuickJoinItem;
