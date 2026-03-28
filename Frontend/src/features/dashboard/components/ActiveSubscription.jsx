import React from 'react';
import { ChevronRight, Receipt } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const formatCurrency = (value) => {
    const amount = Number(value || 0);
    return new Intl.NumberFormat('ko-KR').format(amount);
};

const ActiveSubscription = ({ data }) => {
    const navigate = useNavigate();

    return (
        <div className="group relative mb-8 overflow-hidden rounded-3xl border border-slate-800 bg-[#161B26] p-6 shadow-xl">
            <div className="pointer-events-none absolute right-0 top-0 h-64 w-64 -translate-y-1/3 translate-x-1/3 rounded-full bg-blue-500/5 blur-3xl transition-opacity duration-700 group-hover:opacity-100" />

            <div className="relative z-10">
                <div className="mb-5 flex items-start justify-between gap-4">
                    <div>
                        <p className="mb-2 text-[13px] font-medium text-[#9D9DA4]">월간 구독료</p>
                        <div className="flex items-end gap-2">
                            <span className="text-[32px] font-black leading-none tracking-tight text-white md:text-[36px]">
                                {formatCurrency(data?.totalMonthlyBilling)}
                            </span>
                            <span className="pb-1 text-[16px] font-bold text-white">원</span>
                        </div>
                    </div>
                    <div className="rounded-2xl border border-slate-700 bg-[#1B2230] p-3 text-[#82D8FC]">
                        <Receipt className="h-5 w-5" />
                    </div>
                </div>

                <div className="mb-5 rounded-2xl border border-slate-800 bg-[#10141D] px-4 py-3">
                    <p className="text-[12px] text-[#9D9DA4]">다음 결제일</p>
                    <p className="mt-1 text-[15px] font-semibold text-white">
                        {data?.nextBillingAt || '정보 없음'}
                    </p>
                </div>

                <div className="mb-6 flex flex-wrap gap-2">
                    {(data?.items || []).slice(0, 4).map((item) => (
                        <span
                            key={item.subscriptionItemId}
                            className="rounded-full border border-slate-700 bg-[#1B2230] px-3 py-1.5 text-[12px] text-slate-200"
                        >
                            {item.productName}
                        </span>
                    ))}
                    {!data?.items?.length ? (
                        <span className="rounded-full border border-slate-700 bg-[#1B2230] px-3 py-1.5 text-[12px] text-slate-400">
                            구독 중인 상품이 없습니다.
                        </span>
                    ) : null}
                </div>

                <button
                    className="flex w-full items-center justify-between rounded-xl bg-[#82D8FC] px-4 py-3.5 text-[15px] font-bold text-[#020715] transition-colors hover:bg-[#6CCDF2]"
                    onClick={() => navigate('/mypage/capsure')}
                    type="button"
                >
                    <span>캡슐 상세 보러가기</span>
                    <ChevronRight className="h-5 w-5" />
                </button>
            </div>
        </div>
    );
};

export default ActiveSubscription;
