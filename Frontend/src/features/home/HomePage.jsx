import React from "react";
import { useNavigate } from "react-router-dom";
import WelcomeHeader from "./components/WelcomeHeader";
import CategoryRecommend from "./components/CategoryRecommend";
import SubscribedCapsures from "./components/SubscribedCapsures";
import ActiveInsurances from "./components/ActiveInsurances";
import { getLatestCapsureSubscription } from '@/features/capsure/utils/capsuleStorage';
import { getCategoryRecommendations, getHomeDashboard } from './api/home.api';

const CATEGORY_LABEL_MAP = {
    DEATH: '사망',
    CANCER: '암',
    BRAIN_HEART: '뇌/심장',
    ACTUAL_LOSS: '실손',
    SURGERY: '수술',
    ACCIDENT: '상해',
    LIABILITY: '일상배상책임',
    ETC: '기타',
};
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

const RECOMMEND_TONES = ['blue', 'yellow', 'purple', 'gray'];
const ALL_CATEGORY_CODES = ['DEATH', 'CANCER', 'BRAIN_HEART', 'ACTUAL_LOSS', 'SURGERY', 'ACCIDENT', 'LIABILITY', 'ETC'];

const buildCoverageBadges = (activeCategories = []) => {
    const activeSet = new Set((activeCategories ?? []).map((category) => String(category).trim()));

    return ALL_CATEGORY_CODES.map((code) => {
        const label = CATEGORY_LABEL_MAP[code];
        const isLiabilityAlias = code === 'LIABILITY' && activeSet.has('배상');
        const isActive = activeSet.has(code) || activeSet.has(label) || isLiabilityAlias;
        return { name: label, isActive };
    });
};

const HomePage = () => {
    const navigate = useNavigate();
    // User data state
    const [user, setUser] = React.useState({
        name: '고객',
    });

    React.useEffect(() => {
        import('@/features/mypage/api/mypage.api')
            .then(({ getUserProfile }) => getUserProfile())
            .then(data => {
                if (data && data.name) setUser({ name: data.name });
            })
            .catch(() => console.error("프로필 조회를 실패했습니다."));
    }, []);

    const [subscribedCapsures, setSubscribedCapsures] = React.useState([]);
    const [activeInsurances, setActiveInsurances] = React.useState([]);
    const [categoryRecommendations, setCategoryRecommendations] = React.useState([]);

    React.useEffect(() => {
        getCategoryRecommendations()
            .then((recommendations) => {
                const mapped = (recommendations ?? []).map((item, index) => {
                    const tone = RECOMMEND_TONES[index % RECOMMEND_TONES.length];
                    const categoryLabel =
                        CATEGORY_LABEL_MAP[item.coverageCategoryCode] ??
                        item.coverageCategoryCode ??
                        item.category ??
                        item.categoryLabel ??
                        '';
                    const monthlyPrice = Number(item.monthlyPrice ?? 0);
                    return {
                        id: item.productSourceId,
                        productSourceId: item.productSourceId,
                        categoryLabel,
                        title: item.productName,
                        desc: `${item.companyName} · 월 ${monthlyPrice.toLocaleString()}원`,
                        tone,
                        btnText: '자세히 보기',
                    };
                });
                setCategoryRecommendations(mapped);
            })
            .catch(() => setCategoryRecommendations([]));
    }, []);

    React.useEffect(() => {
        const toPaymentDay = (nextBillingAt) => {
            if (!nextBillingAt) {
                return '-';
            }
            const split = nextBillingAt.split('.');
            return split.length === 3 ? Number(split[2]) : '-';
        };

        getHomeDashboard()
            .then((dashboard) => {
                const mappedCapsules = (dashboard?.subscribedCapsules ?? []).map((capsule, index) => ({
                    id: capsule.capsuleSnapshotId ?? `${capsule.subscriptionId}-${index}`,
                    subscriptionId: capsule.subscriptionId,
                    title: capsule.capsuleName,
                    date: capsule.subscribedDate,
                    themeColor: index % 2 === 0 ? '#82D8FC' : '#F6CD3C',
                    coverages: buildCoverageBadges(capsule.categories ?? []),
                }));

                const mappedInsurances = (dashboard?.activeInsurances ?? []).map((insurance) => ({
                    id: `${insurance.subscriptionId}-${insurance.productSourceId}`,
                    productSourceId: insurance.productSourceId,
                    status: insurance.category || '기타',
                    statusColor: CATEGORY_COLOR_MAP[insurance.category] || CATEGORY_COLOR_MAP.기타,
                    productName: insurance.productName,
                    companyName: insurance.companyName,
                    paymentDay: toPaymentDay(insurance.nextBillingAt),
                    monthlyPremium: Number(insurance.monthlyPremium ?? 0),
                }));

                setSubscribedCapsures(mappedCapsules);
                setActiveInsurances(mappedInsurances);
            })
            .catch(() => {
                setSubscribedCapsures([]);
                setActiveInsurances([]);
            });
    }, []);

    React.useEffect(() => {
        const latestCapsule = getLatestCapsureSubscription();
        if (!latestCapsule) {
            return;
        }

        const mappedCoverages = buildCoverageBadges(
            [...new Set(latestCapsule.products.map((product) => product.categoryLabel))]
        );

        setSubscribedCapsures((previousCapsures) => {
            const filteredCapsures = previousCapsures.filter(
                (capsule) => String(capsule.id) !== String(latestCapsule.subscriptionId)
            );

            return [
                {
                    id: latestCapsule.subscriptionId,
                    title: latestCapsule.capsuleName,
                    date: new Date().toISOString().slice(0, 10).replace(/-/g, '.'),
                    themeColor: '#82D8FC',
                    coverages: mappedCoverages,
                },
                ...filteredCapsures,
            ].slice(0, 2);
        });
    }, []);

    return (
        <div className="px-8 pt-8 pb-4 md:px-12 md:py-10 space-y-8 max-w-[560px] mx-auto w-full transition-all">
            <WelcomeHeader user={user} />
            <CategoryRecommend
                recommendations={categoryRecommendations}
                onViewDetail={(productSourceId) => navigate(`/capsure-insurance/detail/${productSourceId}`)}
            />
            <SubscribedCapsures subscribedCapsures={subscribedCapsures} />
            <ActiveInsurances activeInsurances={activeInsurances} />
        </div>
    );
};

export default HomePage;
