import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
    Settings, 
    Pill, 
    CreditCard, 
    ReceiptText, 
    Megaphone, 
    Headphones, 
    LogOut, 
    ChevronRight 
} from 'lucide-react';

const Profile = () => {
    const navigate = useNavigate();

    // Mock user data
    const user = {
        name: '김캡슐님',
        email: 'capsure_user@email.com',
        subscriptionCount: 3
    };

    const menuItems = [
        { icon: CreditCard, label: '결제 수단 관리', path: '/payment-methods' },
        { icon: ReceiptText, label: '구독 내역', path: '/my-history' },
        { icon: Megaphone, label: '공지사항', path: '/notices' },
        { icon: Headphones, label: '고객센터', path: '/support' },
    ];

    return (
        <div className="flex flex-col min-h-full text-white px-6 py-4 animate-in fade-in duration-500">
            {/* Header */}
            <div className="flex justify-between items-center mb-10">
                <h1 className="text-2xl font-bold">마이페이지</h1>
                <button className="p-2 hover:bg-slate-800 rounded-full transition-colors">
                    <Settings className="w-6 h-6" />
                </button>
            </div>

            {/* Profile Section */}
            <div className="flex justify-between items-center mb-12">
                <div>
                    <h2 className="text-2xl font-bold mb-1">{user.name}</h2>
                    <p className="text-[#9D9DA4] text-sm">{user.email}</p>
                </div>
                <button className="px-4 py-2 bg-[#1C212E] hover:bg-[#2A3142] rounded-xl text-sm font-medium transition-colors">
                    프로필 수정
                </button>
            </div>

            {/* Subscription Info Section */}
            <div className="mb-10">
                <h3 className="text-[#9D9DA4] text-sm font-medium mb-4">나의 구독 정보</h3>
                <button 
                    onClick={() => navigate('/my-capsure')}
                    className="w-full flex items-center justify-between p-5 bg-[#141925] hover:bg-[#1E2535] rounded-[24px] transition-all group border border-slate-800/30"
                >
                    <div className="flex items-center gap-4">
                        <div className="w-10 h-10 bg-[#1C212E] rounded-full flex items-center justify-center text-[#82D8FC]">
                            <Pill className="w-6 h-6" />
                        </div>
                        <span className="font-bold text-lg">나의 구독 캡슐</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <span className="text-[#82D8FC] font-medium">{user.subscriptionCount}개 사용 중</span>
                        <ChevronRight className="w-5 h-5 text-[#4E5669] group-hover:translate-x-1 transition-transform" />
                    </div>
                </button>
            </div>

            {/* Account Settings Section */}
            <div className="flex-1">
                <h3 className="text-[#9D9DA4] text-sm font-medium mb-4">계정 설정</h3>
                <div className="space-y-3">
                    {menuItems.map((item, idx) => {
                        const Icon = item.icon;
                        return (
                            <button 
                                key={idx}
                                onClick={() => navigate(item.path)}
                                className="w-full flex items-center justify-between p-5 bg-[#141925] hover:bg-[#1E2535] rounded-[24px] transition-all group border border-slate-800/30"
                            >
                                <div className="flex items-center gap-4">
                                    <div className="flex items-center justify-center text-[#82D8FC]">
                                        <Icon className="w-6 h-6" />
                                    </div>
                                    <span className="font-bold text-lg">{item.label}</span>
                                </div>
                                <ChevronRight className="w-5 h-5 text-[#4E5669] group-hover:translate-x-1 transition-transform" />
                            </button>
                        );
                    })}
                </div>
            </div>

            {/* Logout Button */}
            <div className="mt-12 mb-8 flex justify-center">
                <button className="flex items-center gap-2 text-[#9D9DA4] hover:text-white transition-colors py-2 px-4 rounded-lg">
                    <LogOut className="w-5 h-5" />
                    <span className="font-medium">로그아웃</span>
                </button>
            </div>
        </div>
    );
};

export default Profile;
