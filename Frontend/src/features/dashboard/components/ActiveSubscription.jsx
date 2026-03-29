import React from 'react';
import { CalendarDays, Wallet } from 'lucide-react';

const formatCurrency = (value) => {
    const amount = Number(value || 0);
    return new Intl.NumberFormat('ko-KR').format(amount);
};

const ActiveSubscription = ({ data }) => {
    return (
        <section className="mb-7">
            <div className="mb-4 flex items-center gap-2 text-slate-100">
                <Wallet className="h-5 w-5 text-slate-300" />
                <h3 className="text-[17px] font-semibold">월간 구독료</h3>
            </div>

            <div className="rounded-[30px] border border-slate-800 bg-[#161B26] px-6 py-7 shadow-xl">
                <div>
                    <p className="text-[14px] font-medium text-slate-300">예상 월 납입 총액</p>
                    <p className="mt-2 text-[42px] font-semibold leading-none tracking-tight text-[#DDE4F2]">
                        {formatCurrency(data?.totalMonthlyBilling)}
                        <span className="ml-1 text-[16px] font-medium text-[#DDE4F2]">원</span>
                    </p>
                </div>

                <div className="mt-5 inline-flex items-center gap-2 rounded-2xl bg-[#1F2736] px-4 py-3 text-[13px] font-normal text-slate-300">
                    <CalendarDays className="h-4 w-4 text-[#82D8FC]" />
                    가장 가까운 결제일: {data?.nextBillingAt || '정보 없음'}
                </div>
            </div>
        </section>
    );
};

export default ActiveSubscription;
