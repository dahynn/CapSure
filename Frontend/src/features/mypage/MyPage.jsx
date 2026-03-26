import React, { useState, useEffect } from 'react';
import { 
    Settings, 
    Pill, 
    CreditCard, 
    ReceiptText, 
    Megaphone, 
    Headphones, 
    LogOut, 
    ChevronRight,
    ChevronLeft,
    CheckCircle2
} from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { getPaymentHistory } from './api/mypage.api';

const MyPage = ({ initialView = 'main' }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const [view, setView] = useState(initialView);
    const [history, setHistory] = useState([]);
    const [showToast, setShowToast] = useState(false);
    
    const [user, setUser] = useState({
        name: '고객',
        email: 'capsure_user@email.com',
        subscriptionCount: 3
    });

    useEffect(() => {
        import('./api/mypage.api')
            .then(({ getUserProfile }) => getUserProfile())
            .then(data => {
                if (data) setUser(prev => ({ ...prev, name: data.name || '고객', email: data.email || prev.email }));
            })
            .catch(() => {});
    }, []);

    useEffect(() => {
        setView(initialView);
    }, [initialView]);

    useEffect(() => {
        if (location.state?.profileUpdated) {
            setShowToast(true);
            window.history.replaceState({}, document.title);
            const timer = setTimeout(() => setShowToast(false), 2500);
            return () => clearTimeout(timer);
        }
    }, [location]);

    useEffect(() => {
        if (view === 'history') {
            const fetchHistory = async () => {
                const data = await getPaymentHistory();
                setHistory(data);
            };
            fetchHistory();
        }
    }, [view]);

    const renderMain = () => (
        <div className="flex flex-col animate-in fade-in slide-in-from-bottom-4 duration-500 space-y-12">
            {/* Header */}
            <div className="flex justify-between items-center mb-0">
                <div></div> {/* Spacer */}
            </div>

            {/* Profile Section */}
            <div className="flex justify-between items-center mb-0">
                <div>
                    <h2 className="text-[28px] md:text-3xl font-bold text-white leading-tight tracking-tight mb-1">{user.name} 님</h2>
                    <p className="text-[#9D9DA4] text-sm">{user.email}</p>
                </div>
                <button 
                    onClick={() => navigate('/mypage/edit')}
                    className="px-4 py-2 bg-[#1C212E] hover:bg-[#2A3142] rounded-xl text-sm font-medium text-white transition-colors"
                >
                    프로필 수정
                </button>
            </div>

            {/* Subscription Info Section */}
            <div className="mb-10">
                <h3 className="text-[#9D9DA4] text-sm font-medium mb-4">나의 구독 정보</h3>
                <button 
                    onClick={() => navigate('/mypage/capsure')}
                    className="w-full flex items-center justify-between p-5 bg-[#141925] hover:bg-[#1E2535] rounded-[24px] transition-all group border border-slate-800/30"
                >
                    <div className="flex items-center gap-4">
                        <div className="w-10 h-10 bg-[#1C212E] rounded-full flex items-center justify-center text-[#82D8FC]">
                            <Pill className="w-6 h-6" />
                        </div>
                        <span className="font-bold text-lg text-white">나의 구독 캡슐</span>
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
                    {[
                        { icon: CreditCard, label: '결제 수단 관리', id: 'payment' },
                        { icon: ReceiptText, label: '구독 내역', id: 'history' },
                        { icon: Megaphone, label: '공지사항', id: 'notice' },
                        { icon: Headphones, label: '고객센터', id: 'support' },
                    ].map((item, idx) => {
                        const Icon = item.icon;
                        return (
                            <button 
                                key={idx}
                                onClick={() => item.id === 'history' ? navigate('/mypage/history') : null}
                                className="w-full flex items-center justify-between p-5 bg-[#141925] hover:bg-[#1E2535] rounded-[24px] transition-all group border border-slate-800/30"
                            >
                                <div className="flex items-center gap-4">
                                    <div className="flex items-center justify-center text-[#82D8FC]">
                                        <Icon className="w-6 h-6" />
                                    </div>
                                    <span className="font-bold text-lg text-white">{item.label}</span>
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

    const renderHistory = () => (
        <div className="flex flex-col animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="flex items-center gap-4 mb-8">
                <button onClick={() => navigate('/mypage')} className="p-2 hover:bg-slate-800 rounded-full transition-colors text-white">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-2xl font-bold text-white">구독 내역</h1>
            </div>

            <div className="space-y-4">
                {history.length > 0 ? history.map((item) => (
                    <div key={item.id} className="p-5 bg-[#141925] rounded-[24px] border border-slate-800/30">
                        <div className="flex justify-between items-start mb-2">
                            <span className="text-[#82D8FC] text-xs font-bold px-2 py-1 bg-[#82D8FC]/10 rounded-md">{item.status}</span>
                            <span className="text-[#9D9DA4] text-xs">{item.date}</span>
                        </div>
                        <h4 className="text-white font-bold text-lg mb-1">{item.items[0].name} {item.items.length > 1 && `외 ${item.items.length-1}건`}</h4>
                        <div className="flex justify-between items-end">
                            <p className="text-[#9D9DA4] text-sm">{item.items[0].company}</p>
                            <p className="text-white font-black text-xl">{item.totalAmount.toLocaleString()}원</p>
                        </div>
                    </div>
                )) : (
                    <div className="py-20 text-center text-[#9D9DA4]">내역이 없습니다.</div>
                )}
            </div>
        </div>
    );

    const renderCapsule = () => (
        <div className="flex flex-col animate-in fade-in slide-in-from-right-4 duration-300">
            <div className="flex items-center gap-4 mb-8">
                <button onClick={() => navigate('/mypage')} className="p-2 hover:bg-slate-800 rounded-full transition-colors text-white">
                    <ChevronLeft className="w-6 h-6" />
                </button>
                <h1 className="text-2xl font-bold text-white">나의 캡슐</h1>
            </div>
            
            <div className="p-8 bg-[#141925] rounded-[32px] border border-slate-800/30 text-center">
                <div className="w-20 h-20 bg-[#1C212E] rounded-full flex items-center justify-center text-[#82D8FC] mx-auto mb-6">
                    <Pill className="w-10 h-10" />
                </div>
                <h3 className="text-white text-xl font-bold mb-2">현재 3개의 캡슐 사용 중</h3>
                <p className="text-[#9D9DA4] text-sm mb-8">각 캡슐을 클릭하여 상세 보장 내용을 확인하세요.</p>
                
                <div className="grid grid-cols-3 gap-4">
                    {[1, 2, 3].map(i => (
                        <button 
                            key={i} 
                            onClick={() => navigate(`/mypage/capsule/${i}`)}
                            className="aspect-square bg-[#1C212E] hover:bg-slate-800 transition-colors rounded-2xl flex items-center justify-center border border-slate-700/50 cursor-pointer shadow-lg hover:shadow-cyan-900/20"
                        >
                            <div className="w-8 h-8 rounded-full bg-[#82D8FC]/20 text-[#82D8FC] flex items-center justify-center">
                                <Pill className="w-4 h-4" />
                            </div>
                        </button>
                    ))}
                    {[4, 5, 6, 7, 8, 9].map(i => (
                        <div key={i} className="aspect-square bg-[#141925]/50 rounded-2xl border border-slate-800/20"></div>
                    ))}
                </div>
            </div>
        </div>
    );

    return (
        <div className="px-8 py-8 md:px-12 md:py-10 space-y-12 max-w-[560px] mx-auto w-full transition-all min-h-screen relative">
            {/* Toast Notification */}
            {showToast && (
                <div className="fixed top-8 left-1/2 -translate-x-1/2 z-50 animate-in slide-in-from-top-4 fade-in duration-300">
                    <div className="bg-[#1C212E]/90 backdrop-blur-md text-white px-5 py-3 rounded-full shadow-2xl border border-slate-700 flex items-center gap-3">
                        <CheckCircle2 className="w-5 h-5 text-[#82D8FC]" />
                        <span className="text-sm font-bold whitespace-nowrap">프로필이 성공적으로 수정되었습니다</span>
                    </div>
                </div>
            )}
            
            {view === 'main' && renderMain()}
            {view === 'history' && renderHistory()}
            {view === 'capsule' && renderCapsule()}
        </div>
    );
};

export default MyPage;
