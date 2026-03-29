import React from 'react';
import { CalendarClock, CheckCircle2, Clock3 } from 'lucide-react';

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

const ItemRow = ({ item, tone = 'default' }) => {
    const isNext = tone === 'next';

    return (
        <div className="flex items-center justify-between gap-4 rounded-2xl border border-slate-800 bg-[#10141D] px-4 py-4">
            <div className="min-w-0">
                <p className="truncate text-[14px] font-semibold text-white">{item.productName}</p>
                <p className="mt-1 text-[12px] text-slate-400">{item.capsuleName || '캡슐 정보 없음'}</p>
                <p className="mt-1 text-[12px] text-[#9D9DA4]">
                    {formatCurrency(item.monthlyPrice)}원 · {item.itemStatus}
                </p>
            </div>
            <div
                className={`rounded-full px-3 py-1 text-[11px] font-bold ${
                    isNext ? 'bg-[#1E2D24] text-[#8AE7A3]' : 'bg-[#1B2230] text-[#82D8FC]'
                }`}
            >
                {isNext ? '다음 회차' : '현재'}
            </div>
        </div>
    );
};

const UpcomingBillingRow = ({ item }) => {
    return (
        <div className="flex items-center justify-between gap-4 rounded-2xl border border-slate-800 bg-[#10141D] px-4 py-4">
            <div className="min-w-0">
                <p className="truncate text-[14px] font-semibold text-white">{item.capsuleName}</p>
                <p className="mt-1 text-[12px] text-slate-400">
                    결제일 {item.nextBillingAt || '정보 없음'}
                    {item.billingAnchorDay ? ` · 정기 결제일 ${item.billingAnchorDay}일` : ''}
                </p>
            </div>
            <div className="text-right">
                <p className="text-[14px] font-bold text-white">{formatCurrency(item.expectedAmount)}원</p>
            </div>
        </div>
    );
};

const ScheduleSummary = ({ data }) => {
    const daysLeft = getDaysLeft(data?.nextBillingAt);

    return (
        <div className="mb-10">
            <div className="mb-6 px-1">
                <h2 className="text-[20px] font-bold tracking-tight text-white">결제 예정일 일정 요약</h2>
            </div>

            <div className="mb-5 rounded-3xl border border-slate-800 bg-[#10141D] p-5 shadow-sm">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <p className="text-[13px] text-[#9D9DA4]">가장 가까운 결제 예정 금액</p>
                        <p className="mt-2 text-[26px] font-black tracking-tight text-white">
                            {formatCurrency(data?.expectedNextAmount)}원
                        </p>
                        <p className="mt-2 text-[12px] text-slate-400">
                            결제일 {data?.nextBillingAt || '정보 없음'}
                            {data?.billingAnchorDay ? ` · 정기 결제일 ${data.billingAnchorDay}일` : ''}
                        </p>
                    </div>
                    <div className="flex items-center gap-2 rounded-2xl bg-[#1B2230] px-3 py-2 text-[#82D8FC]">
                        <CalendarClock className="h-4 w-4" />
                        <span className="text-[12px] font-bold">
                            {daysLeft === null ? '-' : daysLeft >= 0 ? `D-${daysLeft}` : '지남'}
                        </span>
                    </div>
                </div>
            </div>

            <div className="space-y-5">
                <div>
                    <div className="mb-3 flex items-center gap-2 px-1">
                        <CalendarClock className="h-4 w-4 text-[#82D8FC]" />
                        <h3 className="text-[14px] font-semibold text-slate-200">캡슐별 결제 일정</h3>
                    </div>
                    <div className="space-y-3">
                        {(data?.upcomingBillings || []).map((item) => (
                            <UpcomingBillingRow key={`billing-${item.subscriptionId}`} item={item} />
                        ))}
                        {!data?.upcomingBillings?.length ? (
                            <div className="rounded-2xl border border-dashed border-slate-700 px-4 py-4 text-[13px] text-slate-400">
                                예정된 결제 일정이 없습니다.
                            </div>
                        ) : null}
                    </div>
                </div>

                <div>
                    <div className="mb-3 flex items-center gap-2 px-1">
                        <CheckCircle2 className="h-4 w-4 text-[#82D8FC]" />
                        <h3 className="text-[14px] font-semibold text-slate-200">현재 적용 중</h3>
                    </div>
                    <div className="space-y-3">
                        {(data?.currentItems || []).map((item) => (
                            <ItemRow
                                key={`current-${item.subscriptionId}-${item.subscriptionItemId}`}
                                item={item}
                            />
                        ))}
                        {!data?.currentItems?.length ? (
                            <div className="rounded-2xl border border-dashed border-slate-700 px-4 py-4 text-[13px] text-slate-400">
                                현재 적용 중인 상품이 없습니다.
                            </div>
                        ) : null}
                    </div>
                </div>

                <div>
                    <div className="mb-3 flex items-center gap-2 px-1">
                        <Clock3 className="h-4 w-4 text-[#8AE7A3]" />
                        <h3 className="text-[14px] font-semibold text-slate-200">다음 회차 예정</h3>
                    </div>
                    <div className="space-y-3">
                        {(data?.nextItems || []).map((item) => (
                            <ItemRow
                                key={`next-${item.subscriptionId}-${item.subscriptionItemId}`}
                                item={item}
                                tone="next"
                            />
                        ))}
                        {!data?.nextItems?.length ? (
                            <div className="rounded-2xl border border-dashed border-slate-700 px-4 py-4 text-[13px] text-slate-400">
                                예정된 변경 상품이 없습니다.
                            </div>
                        ) : null}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ScheduleSummary;
