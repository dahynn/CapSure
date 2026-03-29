import React from 'react';
import { CalendarClock, ChevronDown, Clock3 } from 'lucide-react';

const formatCurrency = (value) => {
    const amount = Number(value || 0);
    return new Intl.NumberFormat('ko-KR').format(amount);
};

const getDaysLeft = (dateText) => {
    if (!dateText) {
        return null;
    }
    const today = new Date();
    const targetDate = new Date(dateText);
    today.setHours(0, 0, 0, 0);
    targetDate.setHours(0, 0, 0, 0);
    return Math.ceil((targetDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
};

const ScheduleSummary = ({ data }) => {
    const upcomingBillings = data?.upcomingBillings || [];
    const currentItems = data?.currentItems || [];
    const [openedBillingId, setOpenedBillingId] = React.useState(null);
    const daysLeft = getDaysLeft(data?.nextBillingAt);

    const toggleBilling = (subscriptionId) => {
        setOpenedBillingId((prev) => (prev === subscriptionId ? null : subscriptionId));
    };

    return (
        <section className="mb-10">
            <div className="mb-4 flex items-center gap-2 text-slate-100">
                <Clock3 className="h-5 w-5 text-slate-300" />
                <h3 className="text-[17px] font-semibold">결제 예정일 일정 요약</h3>
            </div>

            <div className="mb-4 rounded-3xl border border-slate-800 bg-[#161B26] px-5 py-4 shadow-xl">
                <div className="flex items-start justify-between gap-3">
                    <div>
                        <p className="text-[12px] font-medium text-slate-400">가장 가까운 결제 예정 금액</p>
                        <p className="mt-1 text-[34px] font-semibold tracking-tight text-[#DDE4F2]">
                            {formatCurrency(data?.expectedNextAmount)}원
                        </p>
                        <p className="mt-1 text-[12px] text-slate-400">
                            결제일 {data?.nextBillingAt || '정보 없음'}
                            {data?.billingAnchorDay ? ` · 정기 결제일 ${data.billingAnchorDay}일` : ''}
                        </p>
                    </div>
                    <div className="inline-flex items-center gap-1.5 rounded-xl bg-[#1F2736] px-3 py-2 text-[12px] font-medium text-[#82D8FC]">
                        <CalendarClock className="h-4 w-4" />
                        {daysLeft === null ? '-' : daysLeft >= 0 ? `D-${daysLeft}` : '지남'}
                    </div>
                </div>
            </div>

            <div className="space-y-3">
                {upcomingBillings.slice(0, 2).map((item, index) => {
                    const isOpen = openedBillingId === item.subscriptionId;
                    const relatedItems = currentItems.filter((row) => row.subscriptionId === item.subscriptionId);
                    return (
                        <div
                            key={item.subscriptionId}
                            className={`overflow-hidden rounded-3xl bg-[#161B26] shadow-xl transition-all ${
                                isOpen
                                    ? 'border border-slate-700'
                                    : 'border border-slate-700/70'
                            }`}
                        >
                            <button
                                type="button"
                                onClick={() => toggleBilling(item.subscriptionId)}
                                className="flex w-full items-center justify-between px-6 py-5 text-left focus:outline-none"
                            >
                                <div className="min-w-0">
                                    <div className="mb-1">
                                        <p className="truncate text-[16px] font-semibold text-[#DDE4F2]">{item.capsuleName}</p>
                                    </div>
                                    <p className="text-[13px] text-slate-300">결제일 {item.nextBillingAt || '정보 없음'}</p>
                                </div>

                                <div className="ml-4 flex items-center gap-4">
                                    <p className="text-[16px] font-semibold tracking-tight text-[#DDE4F2]">
                                        {formatCurrency(item.expectedAmount)}원
                                    </p>
                                    <ChevronDown
                                        className={`h-5 w-5 text-slate-300 transition-transform ${isOpen ? 'rotate-180' : ''}`}
                                    />
                                </div>
                            </button>

                            {isOpen ? (
                                <div className="border-t border-slate-700/50 bg-[#1F2736]/70 px-4 py-3">
                                    <p className="mb-2 px-2 text-[12px] font-medium text-slate-400">캡슐 항목별 결제 요약</p>
                                    <div className="space-y-2">
                                        {relatedItems.length ? (
                                            relatedItems.map((row) => (
                                                <div
                                                    key={`${row.subscriptionId}-${row.subscriptionItemId}`}
                                                    className="flex items-center justify-between rounded-xl bg-[#1A2332] px-3 py-2.5"
                                                >
                                                    <div className="min-w-0 pr-3">
                                                        <p className="truncate text-[13px] font-medium text-[#DDE4F2]">{row.productName}</p>
                                                        <p className="mt-0.5 text-[11px] text-slate-400">{row.capsuleName}</p>
                                                    </div>
                                                    <p className="shrink-0 text-[13px] font-semibold text-[#DDE4F2]">
                                                        {formatCurrency(row.monthlyPrice)}원
                                                    </p>
                                                </div>
                                            ))
                                        ) : (
                                            <div className="rounded-xl bg-[#1A2332] px-3 py-3 text-[12px] text-slate-400">
                                                세부 항목 정보가 없습니다.
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ) : null}
                        </div>
                    );
                })}
            </div>

        </section>
    );
};

export default ScheduleSummary;
