import React from "react";
import WelcomeHeader from "./components/WelcomeHeader";
import AiRecommendations from "./components/AiRecommendations";
import SubscribedCapsures from "./components/SubscribedCapsures";
import ActiveInsurances from "./components/ActiveInsurances";

const HomePage = () => {
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

    // Mock AI Recommendations
    const [aiRecommendations] = React.useState([
        {
            id: 1,
            badgeColor: "#82D8FC",
            title: "운전자 보험\n업그레이드 제안",
            desc: "현재 보장 대비 24% 효율 증가",
            btnColor: "#82D8FC",
            btnText: "분석하기"
        },
        {
            id: 2,
            badgeColor: "#F6CD3C",
            title: "암 보험\n맞춤형 플랜",
            desc: "내 가족력 기반 최적의 보장",
            btnColor: "#F6CD3C",
            btnText: "알아보기"
        }
    ]);

    // Mock subscribed capsures
    const [subscribedCapsures] = React.useState([
        {
            id: 1,
            title: "나의 든든한 일상",
            date: "2023.10.12",
            themeColor: "#82D8FC", // 브랜드 블루
            coverages: [
                { name: "실손", isActive: true },
                { name: "상해", isActive: true },
                { name: "배상", isActive: true },
                { name: "사망", isActive: false },
                { name: "암", isActive: true },
                { name: "수술", isActive: true },
                { name: "뇌/심장", isActive: false }
            ]
        },
        {
            id: 2,
            title: "우리 가족 건강 지키미",
            date: "2024.01.05",
            themeColor: "#F6CD3C", // 옐로우
            coverages: [
                { name: "사망", isActive: true },
                { name: "암", isActive: true },
                { name: "뇌/심장", isActive: true },
                { name: "실손", isActive: true },
                { name: "수술", isActive: true },
                { name: "상해", isActive: false },
                { name: "배상", isActive: false }
            ]
        }
    ]);

    // Mock active insurances
    const [activeInsurances] = React.useState([
        {
            id: 1,
            status: "정상 유지",
            statusColor: "#82D8FC", // Blue
            productName: "카카오 정기 보험",
            paymentDay: 15,
            monthlyPremium: 45000
        },
        {
            id: 2,
            status: "납입 대기",
            statusColor: "#F6CD3C", // Yellow
            productName: "현대해상 실손의료비",
            paymentDay: 25,
            monthlyPremium: 12000
        }
    ]);

    return (
        <div className="px-8 py-8 md:px-12 md:py-10 space-y-12 max-w-[560px] mx-auto w-full transition-all">
            <WelcomeHeader user={user} />
            <AiRecommendations recommendations={aiRecommendations} />
            <SubscribedCapsures subscribedCapsures={subscribedCapsures} />
            <ActiveInsurances activeInsurances={activeInsurances} />
        </div>
    );
};

export default HomePage;
