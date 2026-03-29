import React from 'react';
import { ChevronLeft, MoreVertical } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getHomeDashboard } from './api/home.api';

const CATEGORY_COLOR_MAP = {
    사망: '#F2BEF7',
    암: '#F2BEF7',
    '뇌/심장': '#F6CD3C',
    실손: '#82D8FC',
    수술: '#82D8FC',
    상해: '#FFB4C8',
    배상: '#8B9DC4',
    일상배상책임: '#8B9DC4',
    기타: '#8B9DC4',
};

const ActiveInsurancesPage = () => {
    const navigate = useNavigate();
    const [items, setItems] = React.useState([]);
    const [isLoading, setIsLoading] = React.useState(true);

    React.useEffect(() => {
        let mounted = true;

        const load = async () => {
            setIsLoading(true);
            try {
                const dashboard = await getHomeDashboard();
                if (!mounted) return;

                const mapped = (dashboard?.activeInsurances ?? []).map((insurance) => ({
                    id: `${insurance.subscriptionId}-${insurance.productSourceId}`,
                    productSourceId: insurance.productSourceId,
                    status: insurance.category || '기타',
                    statusColor: CATEGORY_COLOR_MAP[insurance.category] || CATEGORY_COLOR_MAP.기타,
                    productName: insurance.productName,
                    paymentDay: insurance.billingAnchorDay ?? '-',
                    monthlyPremium: Number(insurance.monthlyPremium ?? 0),
                }));
                setItems(mapped);
            } catch (error) {
                setItems([]);
            } finally {
                if (mounted) {
                    setIsLoading(false);
                }
            }
        };

        load();
        return () => {
            mounted = false;
        };
    }, []);

    return (
        <div className="mx-auto min-h-screen w-full max-w-[560px] px-8 pt-8 pb-10 md:px-12 md:py-10">
            <header className="mb-6 flex items-center gap-3">
                <button
                    onClick={() => navigate(-1)}
                    className="rounded-full p-2 text-white hover:bg-slate-800 transition-colors"
                    aria-label="뒤로가기"
                >
                    <ChevronLeft className="h-6 w-6" />
                </button>
                <h1 className="text-[24px] font-black tracking-tight text-white">현재 진행 중인 보험</h1>
            </header>

            {isLoading ? (
                <div className="rounded-3xl border border-slate-800 bg-[#161B26] px-6 py-10 text-center text-slate-400">
                    보험 목록을 불러오는 중이에요...
                </div>
            ) : null}

            {!isLoading && items.length === 0 ? (
                <div className="rounded-3xl border border-slate-800 bg-[#161B26] px-6 py-10 text-center text-slate-400">
                    현재 진행 중인 보험이 없습니다.
                </div>
            ) : null}

            {!isLoading ? (
                <div className="space-y-4">
                    {items.map((ins) => (
                        <div
                            key={ins.id}
                            className="w-full bg-[#161B26] rounded-3xl p-6 relative overflow-hidden shadow-xl border border-slate-800 flex flex-col hover:border-slate-700 transition-colors"
                        >
                            <div className="flex justify-between items-start mb-6">
                                <span
                                    className="px-3 py-1.5 rounded-lg text-xs font-bold tracking-wide"
                                    style={{
                                        color: ins.statusColor,
                                        backgroundColor: `${ins.statusColor}1A`,
                                    }}
                                >
                                    {ins.status}
                                </span>
                                <button
                                    onClick={() => navigate(`/capsure-insurance/detail/${ins.productSourceId}`)}
                                    className="text-slate-500 hover:text-white transition-colors"
                                    aria-label="보험 상세 보기"
                                >
                                    <MoreVertical className="w-5 h-5" />
                                </button>
                            </div>

                            <div className="mb-8">
                                <h3 className="text-[17px] font-bold text-white mb-2">{ins.productName}</h3>
                                <p className="text-[13px] text-[#9D9DA4]">월 납입일: {ins.paymentDay}일</p>
                            </div>

                            <div className="flex justify-between items-end mt-auto pt-4">
                                <span className="text-[13px] text-[#9D9DA4] mb-1">월 보험료</span>
                                <span className="text-[24px] font-bold text-white">
                                    {ins.monthlyPremium.toLocaleString()}원
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            ) : null}
        </div>
    );
};

export default ActiveInsurancesPage;
