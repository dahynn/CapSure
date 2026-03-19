import React, { useState, useEffect } from "react";
import {
    getCapsureItems,
    getMyCapsureInsurance,
} from "./api/capsureInsurance.api";
import { submitCapsureReservation } from "./api/capsureModify.api";
import { Plus, Info, ChevronRight, Loader2, CheckCircle } from "lucide-react";
import CustomModal from "@/common/components/ui/modal/CustomModal";
import TermsCheck from "./components/modify/TermsCheck";

const categories = [
    {
        id: "silson",
        name: "실손 보험",
        color: "bg-emerald-100 text-emerald-700 hover:bg-emerald-200",
    },
    {
        id: "disease",
        name: "질병 보험",
        color: "bg-rose-100 text-rose-700 hover:bg-rose-200",
    },
    {
        id: "liability",
        name: "생활 배상 보험",
        color: "bg-blue-100 text-blue-700 hover:bg-blue-200",
    },
    {
        id: "pet",
        name: "펫 보험",
        color: "bg-amber-100 text-amber-700 hover:bg-amber-200",
    },
    {
        id: "driver",
        name: "상시 운전자 보험",
        color: "bg-purple-100 text-purple-700 hover:bg-purple-200",
    },
];

const CapsureModify = () => {
    const [targetAmount, setTargetAmount] = useState(0);
    const [selectedCells, setSelectedCells] = useState([]);

    // view state: 'step-modify' -> 'step-terms'
    const [view, setView] = useState("step-modify");

    // Reservation state
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false);

    // Category items
    const [openCategory, setOpenCategory] = useState(null);
    const [categoryItems, setCategoryItems] = useState([]);
    const [isLoadingItems, setIsLoadingItems] = useState(false);

    const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);

    // Init fetch
    useEffect(() => {
        const fetchInit = async () => {
            try {
                const data = await getMyCapsureInsurance();
                setTargetAmount(data.targetAmount);
                setSelectedCells(
                    data.selectedCells ||
                        Array.from({ length: data.targetAmount }, () => null),
                );
            } catch (e) {
                console.error(e);
            }
        };
        fetchInit();
    }, []);

    // Effect to adjust grid size when targetAmount changes
    useEffect(() => {
        const amount = Number(targetAmount);
        if (amount > 0 && selectedCells.length !== amount) {
            let newCells = [...selectedCells];
            if (newCells.length < amount) {
                // Add empty cells
                newCells = [
                    ...newCells,
                    ...Array.from(
                        { length: amount - newCells.length },
                        () => null,
                    ),
                ];
            } else {
                newCells = newCells.slice(0, amount);
            }
            setSelectedCells(newCells);
        }
    }, [targetAmount]);

    const handleCategoryClick = async (categoryId) => {
        if (openCategory === categoryId) {
            setOpenCategory(null);
            return;
        }
        setOpenCategory(categoryId);
        setIsLoadingItems(true);
        try {
            const data = await getCapsureItems(categoryId, 10000); // 1만원 단위 가정
            setCategoryItems(data);
        } catch (error) {
            console.error("Failed to fetch items", error);
        } finally {
            setIsLoadingItems(false);
        }
    };

    const handleAddItem = (item, cat) => {
        const firstEmptyIndex = selectedCells.findIndex(
            (cell) => cell === null,
        );
        if (firstEmptyIndex !== -1) {
            const newCells = [...selectedCells];
            newCells[firstEmptyIndex] = {
                ...item,
                category: cat,
                groupId: `mod-${Date.now()}`,
            };
            setSelectedCells(newCells);
        } else {
            alert(
                "모든 캡슐이 채워졌습니다. 기존 캡슐을 제거한 후 다시 시도해주세요.",
            );
        }
    };

    const handleRemoveItem = (groupId) => {
        const newCells = selectedCells.map((cell) => {
            if (cell && cell.groupId === groupId) return null;
            return cell;
        });
        setSelectedCells(newCells);
    };

    const handleNextToTerms = () => {
        const filled = selectedCells.filter((c) => c !== null).length;
        if (filled < Number(targetAmount)) {
            alert("목표 금액만큼 칸을 모두 채워주세요!");
            return;
        }
        setView("step-terms");
    };

    const handleExecuteReservation = async () => {
        setIsSubmitting(true);
        try {
            await submitCapsureReservation({
                targetAmount,
                selectedCells,
            });
            setIsSuccessModalOpen(true);
        } catch (e) {
            console.error(e);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleSuccessClose = () => {
        setIsSuccessModalOpen(false);
        // Reset or navigate logic can go here (for now, simply returns to previous tab/state implicitly
        // by the parent component or simply resetting the view)
        setView("step-modify");
    };

    return (
        <div className="space-y-6 font-sans">
            {view === "step-modify" && (
                <div className="animate-in fade-in slide-in-from-right-4 duration-500">
                    <div className="flex justify-between items-end border-b border-slate-200 pb-4 mb-6">
                        <div>
                            <h2 className="text-2xl font-bold text-slate-800">
                                다음달 구독 변경 예약하기
                            </h2>
                            <p className="text-slate-500 mt-1">
                                원하는 캡슐을 추가/제거하여 다음달 보험을 새롭게
                                구성해보세요.
                            </p>
                        </div>
                        <div className="flex items-center gap-3 bg-white px-4 py-2 rounded-xl border border-slate-200">
                            <span className="text-sm font-bold text-slate-600">
                                구독 금액:
                            </span>
                            <input
                                type="number"
                                min="1"
                                value={targetAmount}
                                onChange={(e) =>
                                    setTargetAmount(e.target.value)
                                }
                                className="w-16 font-black text-primary-600 text-xl border-b-2 border-slate-200 focus:border-primary-500 outline-none text-center bg-transparent"
                            />
                            <span className="font-bold text-slate-600">
                                만원
                            </span>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                        {/* Left: Grid Table */}
                        <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 min-h-[500px] flex flex-col">
                            <div className="bg-slate-100 rounded-2xl flex-1 border-2 border-dashed border-slate-300 p-8 grid grid-cols-5 gap-2 content-start">
                                {selectedCells.map((cell, i) => (
                                    <div
                                        key={i}
                                        onClick={() =>
                                            cell &&
                                            handleRemoveItem(cell.groupId)
                                        }
                                        className={`aspect-square sm:aspect-auto sm:h-20 rounded-xl border flex flex-col items-center justify-center transition-all ${
                                            cell
                                                ? `${cell.category.color} cursor-pointer hover:opacity-80`
                                                : "bg-white/50 border-slate-200"
                                        }`}
                                    >
                                        {cell ? (
                                            <span className="font-bold text-[10px] sm:text-xs text-center px-1 leading-tight">
                                                {cell.name}
                                            </span>
                                        ) : (
                                            <Plus className="w-6 h-6 text-slate-300" />
                                        )}
                                    </div>
                                ))}
                            </div>

                            <div className="mt-6 flex justify-between items-center">
                                <div className="flex items-center gap-2 text-sm text-slate-500 font-medium">
                                    <Info className="w-4 h-4" /> 탭하여 항목을
                                    제거할 수 있습니다
                                </div>
                                <button
                                    onClick={handleNextToTerms}
                                    className="bg-slate-800 text-white px-8 py-3 rounded-xl font-bold hover:bg-slate-900 transition-colors shadow-lg"
                                >
                                    변경 완료 및 예약하기
                                </button>
                            </div>
                        </div>

                        {/* Right: Categories */}
                        <div className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 flex flex-col h-[600px]">
                            <h3 className="font-bold text-slate-800 text-lg mb-4">
                                보험 카테고리
                            </h3>

                            <div className="flex items-center gap-2 mb-4 overflow-x-auto pb-2 scrollbar-hide">
                                <button
                                    onClick={() => setIsFilterModalOpen(true)}
                                    className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-600 rounded-lg text-sm font-bold transition-colors whitespace-nowrap"
                                >
                                    필터 커스텀
                                </button>
                                <button className="px-3 py-1.5 bg-slate-50 border border-slate-200 hover:bg-slate-100 text-slate-600 rounded-lg text-sm transition-colors whitespace-nowrap">
                                    인기순
                                </button>
                                <button className="px-3 py-1.5 bg-slate-50 border border-slate-200 hover:bg-slate-100 text-slate-600 rounded-lg text-sm transition-colors whitespace-nowrap">
                                    가성비순
                                </button>
                            </div>

                            <div className="flex-1 overflow-y-auto pr-2 space-y-3 custom-scrollbar">
                                {categories.map((cat) => (
                                    <div
                                        key={cat.id}
                                        className="border border-slate-200 rounded-2xl overflow-hidden shadow-sm"
                                    >
                                        <button
                                            onClick={() =>
                                                handleCategoryClick(cat.id)
                                            }
                                            className={`w-full p-4 flex items-center justify-between transition-colors ${cat.color.split(" ")[0]} ${cat.color.split(" ")[1]}`}
                                        >
                                            <span className="font-bold text-lg tracking-tight">
                                                {cat.name}
                                            </span>
                                            {openCategory === cat.id ? (
                                                <ChevronRight className="w-5 h-5 rotate-90 transition-transform" />
                                            ) : (
                                                <ChevronRight className="w-5 h-5 transition-transform" />
                                            )}
                                        </button>

                                        {openCategory === cat.id && (
                                            <div className="bg-white p-4 border-t border-slate-100">
                                                {isLoadingItems ? (
                                                    <div className="flex justify-center py-4">
                                                        <Loader2 className="w-6 h-6 animate-spin text-slate-300" />
                                                    </div>
                                                ) : (
                                                    <div className="space-y-3">
                                                        {categoryItems.map(
                                                            (item) => (
                                                                <div
                                                                    key={
                                                                        item.id
                                                                    }
                                                                    className="group relative flex items-center justify-between p-3 bg-slate-50 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer border border-transparent hover:border-slate-200"
                                                                    onClick={() =>
                                                                        handleAddItem(
                                                                            item,
                                                                            cat,
                                                                        )
                                                                    }
                                                                >
                                                                    <div className="flex flex-col">
                                                                        <span className="font-bold text-slate-700">
                                                                            {
                                                                                item.name
                                                                            }
                                                                        </span>
                                                                        <span className="text-xs text-slate-500 mt-1">
                                                                            {
                                                                                item.description
                                                                            }
                                                                        </span>
                                                                    </div>
                                                                    <div className="flex items-center gap-3">
                                                                        <span className="text-xs font-black px-2 py-1 bg-white rounded-md shadow-sm border border-slate-100 group-hover:bg-primary-50 group-hover:text-primary-600 transition-colors">
                                                                            1만원
                                                                        </span>
                                                                        <div className="w-8 h-8 rounded-full bg-white flex items-center justify-center text-slate-400 group-hover:bg-primary-500 group-hover:text-white transition-colors shadow-sm">
                                                                            <Plus className="w-4 h-4" />
                                                                        </div>
                                                                    </div>
                                                                </div>
                                                            ),
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {view === "step-terms" && (
                <div className="bg-white p-8 rounded-3xl shadow-sm border border-slate-200 animate-in fade-in slide-in-from-right-4">
                    <div className="mb-6 flex justify-between items-center">
                        <div>
                            <h2 className="text-2xl font-bold text-slate-800">
                                예약 변경 약관 동의
                            </h2>
                            <p className="text-slate-500 mt-1">
                                새로 구성하신 보험의 약관을 확인해 주세요.
                            </p>
                        </div>
                    </div>
                    {isSubmitting ? (
                        <div className="flex flex-col items-center justify-center py-20 space-y-4">
                            <Loader2 className="w-10 h-10 animate-spin text-primary-500" />
                            <p className="text-slate-600 font-medium text-lg">
                                예약 정보를 저장 중입니다...
                            </p>
                        </div>
                    ) : (
                        <TermsCheck
                            selectedCells={selectedCells}
                            onPrev={() => setView("step-modify")}
                            onNext={handleExecuteReservation}
                            buttonText="예약 완료하기"
                        />
                    )}
                </div>
            )}

            <CustomModal
                isOpen={isSuccessModalOpen}
                onClose={handleSuccessClose}
            >
                <div className="text-center p-8 flex flex-col items-center">
                    <div className="w-20 h-20 bg-emerald-100 text-emerald-500 rounded-full flex items-center justify-center mb-6">
                        <CheckCircle className="w-10 h-10" />
                    </div>
                    <h3 className="text-2xl font-black text-slate-800 mb-2">
                        예약 완료
                    </h3>
                    <p className="text-slate-500 text-lg mb-8">
                        다음달 보험 구성 변경 예약이 완료되었습니다.
                        <br />
                        예약된 내역은 다음 달 결제일에 맞춰 적용됩니다.
                    </p>
                    <button
                        onClick={handleSuccessClose}
                        className="px-8 py-3 bg-slate-800 text-white font-bold rounded-xl hover:bg-slate-900 transition-colors shadow-lg w-full"
                    >
                        확인
                    </button>
                </div>
            </CustomModal>

            <CustomModal
                isOpen={isFilterModalOpen}
                onClose={() => setIsFilterModalOpen(false)}
            >
                <div className="text-center p-8">
                    <h3 className="text-xl font-bold text-slate-800 mb-2">
                        필터 커스텀 모달
                    </h3>
                    <p className="text-slate-500 text-sm">
                        원하는 조건으로 보험 상품을 자세하게 필터링할 수 있는
                        모달입니다.
                    </p>
                </div>
            </CustomModal>
        </div>
    );
};

export default CapsureModify;
