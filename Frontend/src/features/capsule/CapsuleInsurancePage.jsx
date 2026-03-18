import React, { useState, useEffect } from 'react';
import { ShieldCheck, Plus, Check, ArrowRight, Info, GripHorizontal, SlidersHorizontal, Loader2 } from 'lucide-react';
import TextModal from '@/common/components/ui/modal/TextModal';
import CustomModal from '@/common/components/ui/modal/CustomModal';
import { getCapsuleItems } from './api/capsuleInsurance.api';
import CapsuleGridMaker from './components/CapsuleGridMaker';
import CapsuleItemExplorer from './components/CapsuleItemExplorer';

// Subscribe Flow Components
import InsuranceInfoConfirm from './components/subscribe/InsuranceInfoConfirm';
import TermsCheck from './components/subscribe/TermsCheck';
import Payment from './components/subscribe/Payment';
import SubscribeComplete from './components/subscribe/SubscribeComplete';
import MyCapsuleInsurance from '@/features/mypage/MyCapsuleInsurance';

const CapsuleInsurancePage = () => {
    const [hasSubscription, setHasSubscription] = useState(false);
    const [view, setView] = useState('prologue');
    const [targetAmount, setTargetAmount] = useState('');
    const [selectedCells, setSelectedCells] = useState([]);
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

    // Filter & Category State
    const [activeCategory, setActiveCategory] = useState('shilson');
    const [activeFilter, setActiveFilter] = useState(null); // null(All), 1, 3, 5
    const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
    const [categoryItems, setCategoryItems] = useState([]);
    const [isItemsLoading, setIsItemsLoading] = useState(false);

    // Accordion & Terms state
    const [expandedItemId, setExpandedItemId] = useState(null);
    const [isTermsModalOpen, setIsTermsModalOpen] = useState(false);
    const [selectedTermsText, setSelectedTermsText] = useState("");

    // Updated Capsule Categories
    const categories = [
        { id: 'shilson', name: '실손 보험', color: 'bg-teal-100 border-teal-300 text-teal-700' },
        { id: 'disease', name: '질병 보험', color: 'bg-rose-100 border-rose-300 text-rose-700' },
        { id: 'liability', name: '생활 배상 보험', color: 'bg-blue-100 border-blue-300 text-blue-700' },
        { id: 'pet', name: '펫 보험', color: 'bg-amber-100 border-amber-300 text-amber-700' },
        { id: 'driver', name: '상시 운전자 보험', color: 'bg-indigo-100 border-indigo-300 text-indigo-700' },
    ];

    // Effect to fetch items when category or filter changes
    useEffect(() => {
        if (!activeCategory) return;

        const fetchItems = async () => {
            setIsItemsLoading(true);
            try {
                const items = await getCapsuleItems(activeCategory, activeFilter);
                setCategoryItems(items);
            } catch (error) {
                console.error("Failed to fetch capsule items:", error);
            } finally {
                setIsItemsLoading(false);
            }
        };

        fetchItems();
    }, [activeCategory, activeFilter]);

    const handleCreateCapsule = (e) => {
        e.preventDefault();
        const amount = Number(targetAmount);
        if (amount > 0) {
            setSelectedCells(Array(amount).fill(null)); // Initialize N cells with null
            setView('grid-maker');
        }
    };

    const handleAddItem = (category, item) => {
        const price = item.price;
        const emptyCellsCount = selectedCells.filter(cell => cell === null).length;

        if (emptyCellsCount < price) {
            alert("선택한 금액보다 많은 보험을 담았어요.");
            return;
        }

        // Fill empty cells with the selected category item
        let filledCount = 0;
        const groupId = Date.now() + Math.random(); // Unique ID for this block group
        const newCells = selectedCells.map(cell => {
            if (cell === null && filledCount < price) {
                filledCount++;
                return {
                    category,
                    name: `${item.name} (${price}만)`,
                    groupId,
                    company: item.company
                };
            }
            return cell;
        });

        setSelectedCells(newCells);
    };

    const handleRemoveItem = (groupId) => {
        if (!groupId) return;
        const newCells = selectedCells.map(cell => {
            if (cell && cell.groupId === groupId) {
                return null; // Empty the cell
            }
            return cell;
        });
        setSelectedCells(newCells);
    };

    const handleSubscribeConfirm = () => {
        const filledCellsCount = selectedCells.filter(cell => cell !== null).length;
        const amount = Number(targetAmount);

        if (filledCellsCount < amount) {
            setIsConfirmModalOpen(true);
        } else {
            setView('step-info'); // Go to first step of subscription
        }
    };

    const performSubscription = () => {
        setIsConfirmModalOpen(false);
        // Navigate to the first step of the subscription flow instead of directly finishing
        setView('step-info');
    };

    return (
        <div className="p-6 md:p-8 pb-24 min-h-screen bg-slate-50">

            {/* ----------------- PROLOGUE VIEW ----------------- */}
            {!hasSubscription && view === 'prologue' && (
                <div className="max-w-2xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4">
                    <div className="text-center space-y-4 py-8">
                        <div className="w-20 h-20 bg-primary-100 text-primary-600 rounded-full flex items-center justify-center mx-auto mb-6">
                            <ShieldCheck className="w-10 h-10" />
                        </div>
                        <h1 className="text-3xl font-black text-slate-900">구독형 캡슐 보험이란?</h1>
                        <p className="text-lg text-slate-600 leading-relaxed max-w-lg mx-auto">
                            레고 캡슐을 조립하듯, 나에게 필요한 보장만 골라 담아
                            매월 결제하는 신개념 맞춤형 보험 서비스입니다.
                        </p>
                    </div>

                    <div className="bg-white p-8 rounded-3xl shadow-xl glass-panel space-y-6">
                        <div className="flex items-start gap-4">
                            <div className="mt-1 p-2 bg-emerald-100 text-emerald-600 rounded-lg"><Check className="w-5 h-5" /></div>
                            <div>
                                <h3 className="font-bold text-slate-800 text-lg">필요한 보장만 선택</h3>
                                <p className="text-slate-500 mt-1">불필요한 특약 없이 딱 필요한 것만 가입하세요.</p>
                            </div>
                        </div>
                        <div className="flex items-start gap-4">
                            <div className="mt-1 p-2 bg-blue-100 text-blue-600 rounded-lg"><Check className="w-5 h-5" /></div>
                            <div>
                                <h3 className="font-bold text-slate-800 text-lg">매월 자유로운 변경</h3>
                                <p className="text-slate-500 mt-1">상황에 따라 다음 달 보장 내역을 쉽게 바꿀 수 있어요.</p>
                            </div>
                        </div>

                        <button
                            onClick={() => setView('create')}
                            className="w-full mt-8 py-4 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 shadow-lg shadow-primary-200 transition-all flex justify-center items-center gap-2 group"
                        >
                            "캡슐 보험" 만들기
                            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                        </button>
                    </div>
                </div>
            )}

            {/* ----------------- CREATE VIEW ----------------- */}
            {!hasSubscription && view === 'create' && (
                <div className="max-w-xl mx-auto pt-12 animate-in zoom-in-95 duration-300">
                    <div className="bg-white p-8 rounded-3xl shadow-2xl glass-panel text-center">
                        <h2 className="text-2xl font-bold text-slate-800 mb-2">얼마만큼 혜택을 받고 싶으신가요?</h2>
                        <p className="text-slate-500 mb-8">목표 금액을 설정하면 그에 맞는 캡슐 판이 생성됩니다.</p>

                        <form onSubmit={handleCreateCapsule} className="space-y-6">
                            <div className="flex items-center justify-center gap-4 text-3xl font-black text-slate-800">
                                <span>저는 보험을</span>
                                <div className="relative w-32 border-b-4 border-slate-200 focus-within:border-primary-500 transition-colors">
                                    <input
                                        type="number"
                                        required
                                        value={targetAmount}
                                        onChange={(e) => setTargetAmount(e.target.value)}
                                        className="w-full text-center bg-transparent outline-none pb-1 text-primary-600"
                                        placeholder="0"
                                    />
                                </div>
                                <span>만원만큼</span>
                            </div>
                            <p className="text-xl font-bold text-slate-800">혜택 받고 싶어요.</p>

                            <button
                                type="submit"
                                className="w-full mt-8 py-4 rounded-xl font-bold text-white bg-primary-600 hover:bg-primary-700 transition-all"
                            >
                                확인
                            </button>

                            <button
                                type="button"
                                onClick={() => setView('prologue')}
                                className="text-slate-400 hover:text-slate-600 text-sm font-medium"
                            >
                                취소
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* ----------------- GRID MAKER VIEW ----------------- */}
            {!hasSubscription && view === 'grid-maker' && (
                <div className="space-y-6 animate-in fade-in duration-500 h-full flex flex-col">
                    <div className="flex justify-between items-end mb-6">
                        <div>
                            <h2 className="text-2xl font-bold text-slate-800">캡슐 구성하기</h2>
                            <p className="text-slate-500 mt-1">왼쪽에서 목표 금액을 확인하고, 오른쪽에서 보험을 탐색하여 채워주세요.</p>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 xl:grid-cols-5 gap-6 h-full min-h-[600px]">
                        {/* Left Side: Grid Area (60%) */}
                        <div className="h-full xl:col-span-3">
                            <CapsuleGridMaker
                                selectedCells={selectedCells}
                                handleRemoveItem={handleRemoveItem}
                                targetAmount={targetAmount}
                            />
                        </div>

                        {/* Right Side: Selection Area (40%) */}
                        <div className="h-full xl:col-span-2">
                            <CapsuleItemExplorer
                                categories={categories}
                                activeCategory={activeCategory}
                                setActiveCategory={setActiveCategory}
                                activeFilter={activeFilter}
                                setActiveFilter={setActiveFilter}
                                setIsFilterModalOpen={setIsFilterModalOpen}
                                isItemsLoading={isItemsLoading}
                                categoryItems={categoryItems}
                                expandedItemId={expandedItemId}
                                setExpandedItemId={setExpandedItemId}
                                setSelectedTermsText={setSelectedTermsText}
                                setIsTermsModalOpen={setIsTermsModalOpen}
                                handleAddItem={handleAddItem}
                            />
                        </div>
                    </div>

                    {/* Subscribe Button Area */}
                    <div className="mt-8 flex justify-end items-center bg-white p-6 rounded-3xl shadow-sm border border-slate-200">
                        <div className="flex items-center gap-3 text-sm text-slate-500 font-medium mr-6">
                            <Info className="w-5 h-5 text-primary-500" />
                            남은 칸을 모두 채워야만 구독을 진행할 수 있어요.
                        </div>
                        <button
                            onClick={handleSubscribeConfirm}
                            className="px-10 py-4 bg-primary-600 text-white text-lg font-bold rounded-xl shadow-lg hover:bg-primary-700 transition-colors"
                        >
                            캡슐 보험 구독하기
                        </button>
                    </div>
                </div>
            )}

            {/* ----------------- SUBSCRIBE FLOW VIEWS ----------------- */}
            {view === 'step-info' && (
                <InsuranceInfoConfirm
                    selectedCells={selectedCells}
                    onNext={() => setView('step-terms')}
                    onPrev={() => setView('grid-maker')}
                />
            )}

            {view === 'step-terms' && (
                <TermsCheck
                    selectedCells={selectedCells}
                    onNext={() => setView('step-pay')}
                    onPrev={() => setView('step-info')}
                    buttonText="다음"
                />
            )}

            {view === 'step-pay' && (
                <Payment
                    onNext={() => {
                        setHasSubscription(true); // User effectively paid and subscribed
                        setView('step-complete');
                    }}
                    onPrev={() => setView('step-terms')}
                />
            )}

            {view === 'step-complete' && (
                <SubscribeComplete
                    selectedCells={selectedCells}
                    onNext={() => setView('my-capsule')}
                />
            )}

            {/* ----------------- MY CAPSULE VIEW ----------------- */}
            {view === 'my-capsule' && (
                <MyCapsuleInsurance />
            )}

            {/* ----------------- SUBSCRIBED VIEW (Legacy - kept if needed for toggle) ----------------- */}
            {hasSubscription && ['subscribed-this', 'subscribed-next'].includes(view) && (
                <div className="space-y-6 animate-in slide-in-from-bottom-4">
                    <div className="flex justify-between items-end mb-8">
                        <div>
                            <h2 className="text-2xl font-bold text-slate-800">나의 캡슐 보험</h2>
                            <p className="text-slate-500 text-sm mt-1">현재 적용 중인 맞춤형 보장 내역입니다.</p>
                        </div>
                        {/* Tabs */}
                        <div className="flex bg-slate-200/60 p-1.5 rounded-xl">
                            <button
                                onClick={() => setView('subscribed-this')}
                                className={`px-4 py-2 text-sm font-bold rounded-lg transition-all ${view !== 'subscribed-next' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                이번달 적용 캡슐
                            </button>
                            <button
                                onClick={() => setView('subscribed-next')}
                                className={`px-4 py-2 text-sm font-bold rounded-lg transition-all ${view === 'subscribed-next' ? 'bg-white text-primary-600 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}
                            >
                                다음달 예약 변경
                            </button>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                        {/* Left Box - Capsules Display */}
                        <div className="bg-white p-6 rounded-3xl shadow-md border border-slate-100 min-h-[400px]">
                            <h3 className="font-bold text-slate-800 mb-6 flex items-center justify-between">
                                <span>내 캡슐 판</span>
                                <span className="text-sm font-normal text-slate-400">총 5만원</span>
                            </h3>

                            {/* Custom Grid Layout Simulation */}
                            <div className="grid grid-cols-5 grid-rows-2 gap-3 h-48">
                                <div className="col-span-3 bg-rose-100 border border-rose-300 rounded-xl p-3 flex flex-col justify-between text-rose-700">
                                    <span className="font-bold text-sm">질병 (3만)</span>
                                    <ShieldCheck className="w-5 h-5 opacity-50" />
                                </div>
                                <div className="col-span-2 bg-blue-100 border border-blue-300 rounded-xl p-3 flex flex-col justify-between text-blue-700">
                                    <span className="font-bold text-sm">배상 (2만)</span>
                                    <ShieldCheck className="w-5 h-5 opacity-50" />
                                </div>
                            </div>
                        </div>

                        {/* Right Box - Details / Actions */}
                        <div className="bg-white p-6 rounded-3xl shadow-md border border-slate-100">
                            {view !== 'subscribed-next' ? (
                                // This Month View
                                <div>
                                    <h3 className="font-bold text-slate-800 mb-6">보장 상세 내역</h3>
                                    <ul className="space-y-4">
                                        <li className="flex justify-between items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                                            <div className="flex items-center gap-3">
                                                <div className="w-2 h-2 rounded-full bg-rose-500" />
                                                <span className="font-bold text-slate-700">질병 입원 일당 (3만)</span>
                                            </div>
                                            <span className="text-slate-500 font-medium text-sm">최대 3,000만원</span>
                                        </li>
                                        <li className="flex justify-between items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                                            <div className="flex items-center gap-3">
                                                <div className="w-2 h-2 rounded-full bg-blue-500" />
                                                <span className="font-bold text-slate-700">생활 배상 책임 (2만)</span>
                                            </div>
                                            <span className="text-slate-500 font-medium text-sm">최대 1억원</span>
                                        </li>
                                    </ul>
                                </div>
                            ) : (
                                // Next Month Modify View
                                <div className="flex flex-col h-full">
                                    <h3 className="font-bold text-slate-800 mb-4">다음달 보험 수정하기</h3>
                                    <p className="text-sm text-slate-500 mb-6 leading-relaxed">
                                        오른쪽의 카테고리에서 원하는 캡슐을 드래그하거나 클릭하여 추가하세요.
                                        왼쪽의 적용된 캡슐을 클릭하면 제거할 수 있습니다.
                                    </p>

                                    {/* Category Selection Mini */}
                                    <div className="flex-1 space-y-3 overflow-y-auto pr-2">
                                        {categories.map((cat) => (
                                            <div key={cat.id} className={`p-3 border rounded-xl flex items-center justify-between cursor-pointer hover:opacity-80 transition-opacity ${cat.color}`}>
                                                <span className="font-bold text-sm">{cat.name}</span>
                                                <Plus className="w-4 h-4" />
                                            </div>
                                        ))}
                                    </div>

                                    <div className="pt-6 mt-4 border-t border-slate-100">
                                        <button
                                            onClick={() => alert("다음달 약관 동의 페이지로 이동합니다.")}
                                            className="w-full py-4 bg-slate-800 text-white font-bold rounded-xl shadow-md hover:bg-slate-900 transition-colors"
                                        >
                                            수정 완료 및 약관 확인
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            <TextModal
                isOpen={isConfirmModalOpen}
                onClose={() => setIsConfirmModalOpen(false)}
                onConfirm={performSubscription}
                contents="선택한 금액보다 적은 보험을 담았어요. 진행할까요?"
                confirmText="확인"
                cancelText="취소"
            />

            <CustomModal
                isOpen={isFilterModalOpen}
                onClose={() => setIsFilterModalOpen(false)}
                onConfirm={() => setIsFilterModalOpen(false)}
                hideCancel={true}
            >
                <div className="text-center py-6">
                    <h2 className="text-xl font-bold text-slate-800">필터 커스텀 모달</h2>
                    <p className="text-sm text-slate-500 mt-2">이곳에 복잡한 필터 폼이 들어갑니다.</p>
                </div>
            </CustomModal>

            <CustomModal
                isOpen={isTermsModalOpen}
                onClose={() => setIsTermsModalOpen(false)}
                onConfirm={() => setIsTermsModalOpen(false)}
                hideCancel={true}
                confirmText="확인"
            >
                <div className="py-4">
                    <h2 className="text-lg font-bold text-slate-800 mb-4">약관 상세</h2>
                    <div className="text-sm text-slate-600 whitespace-pre-line max-h-60 overflow-y-auto pr-2 bg-slate-50 p-4 rounded-xl border border-slate-200">
                        {selectedTermsText}
                    </div>
                </div>
            </CustomModal>
        </div>
    );
};

export default CapsuleInsurancePage;
