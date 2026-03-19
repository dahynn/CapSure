import React, { useEffect, useState } from 'react';
import { getInsuranceTerms } from '@/features/capsure/api/capsureInsurance.api';
import { Loader2, ArrowRight, CheckCircle2, ChevronDown, ChevronUp, CheckSquare, Square } from 'lucide-react';

const TermsCheck = ({ selectedCells, onNext, onPrev, buttonText = "다음" }) => {
    const [termsData, setTermsData] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [openAccordion, setOpenAccordion] = useState(null);

    // Track checked state: { "s1-t1": true, "s1-t2": false, ... }
    const [checkedTerms, setCheckedTerms] = useState({});

    useEffect(() => {
        const fetchTerms = async () => {
            setIsLoading(true);
            try {
                // Collect dummy IDs for API from selectedCells
                const selectedItemsDetails = selectedCells.filter(c => c !== null).reduce((acc, current) => {
                    const x = acc.find(item => item.groupId === current.groupId);
                    if (!x) return acc.concat([current]);
                    return acc;
                }, []);

                // For mock, we map them directly to ids recognized by getInsuranceTerms or pass our mapped ids
                const mappedIds = selectedItemsDetails.map((item, idx) => {
                    // Match to dummy items (approximate logic for mock demo)
                    if (item.category.id === 'disease') return 'd1';
                    if (item.category.id === 'liability') return 'l2';
                    if (item.category.id === 'pet') return 'p1';
                    if (item.category.id === 'shilson') return 's1';
                    return `mock-${idx}`;
                });

                // Fetch the mock terms
                const fetchedTerms = await getInsuranceTerms(mappedIds.length ? mappedIds : ['s1', 'd1']);

                // Merge real titles from our selectedCells to match user intent visually
                const mergedTerms = fetchedTerms.map((t, idx) => ({
                    ...t,
                    id: selectedItemsDetails[idx] ? selectedItemsDetails[idx].groupId : t.id,
                    title: selectedItemsDetails[idx] ? `${selectedItemsDetails[idx].name.replace(/\s*\(\d+만\)/, '')} - ${selectedItemsDetails[idx].company}` : t.title
                }));

                setTermsData(mergedTerms);

                // Initialize check states
                const initialChecks = {};
                mergedTerms.forEach(insurance => {
                    insurance.termsList.forEach(term => {
                        initialChecks[term.id] = false;
                    });
                });
                setCheckedTerms(initialChecks);

            } catch (error) {
                console.error("Failed to fetch terms", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchTerms();
    }, [selectedCells]);

    // Check if a specific insurance's all terms are checked
    const isInsuranceAllChecked = (insurance) => {
        if (!insurance.termsList || insurance.termsList.length === 0) return false;
        return insurance.termsList.every(term => checkedTerms[term.id]);
    };

    // Check if ALL insurances' ALL terms are checked
    const isAllChecked = () => {
        if (termsData.length === 0) return false;
        return termsData.every(insurance => isInsuranceAllChecked(insurance));
    };

    const toggleTerm = (termId) => {
        setCheckedTerms(prev => ({
            ...prev,
            [termId]: !prev[termId]
        }));
    };

    if (isLoading) {
        return (
            <div className="flex flex-col items-center justify-center h-64 space-y-4">
                <Loader2 className="w-8 h-8 text-primary-500 animate-spin" />
                <p className="text-slate-500 font-medium">약관 데이터를 불러오는 중입니다...</p>
            </div>
        );
    }

    return (
        <div className="max-w-2xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4">
            <div className="text-center py-6">
                <h2 className="text-2xl font-bold text-slate-800">약관 확인</h2>
                <p className="text-slate-500 mt-2">가입하시는 보험별로 모든 필수 약관에 동의해주세요.</p>
            </div>

            <div className="space-y-4">
                {termsData.map((insurance) => {
                    const isOpen = openAccordion === insurance.id;
                    const allChecked = isInsuranceAllChecked(insurance);

                    return (
                        <div key={insurance.id} className="bg-white border text-left border-slate-200 rounded-2xl overflow-hidden shadow-sm">
                            <button
                                onClick={() => setOpenAccordion(isOpen ? null : insurance.id)}
                                className={`w-full flex items-center justify-between p-5 transition-colors ${isOpen ? 'bg-slate-50' : 'hover:bg-slate-50'}`}
                            >
                                <div className="flex items-center gap-3">
                                    {isOpen ? (
                                        <ChevronUp className="w-5 h-5 text-slate-400" />
                                    ) : (
                                        <ChevronDown className="w-5 h-5 text-slate-400" />
                                    )}
                                    <span className="font-bold text-slate-800 text-lg">{insurance.title}</span>
                                </div>
                                <div className={`flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold border ${allChecked ? 'bg-emerald-50 text-emerald-600 border-emerald-200' : 'bg-slate-100 text-slate-500 border-slate-200'}`}>
                                    {allChecked && <CheckCircle2 className="w-3.5 h-3.5" />}
                                    {allChecked ? '확인 완료' : '확인 필요'}
                                </div>
                            </button>

                            {isOpen && (
                                <div className="p-5 border-t border-slate-100 bg-slate-50/50 space-y-6 animate-in slide-in-from-top-2">
                                    {insurance.termsList.map((term) => (
                                        <div key={term.id} className="space-y-3">
                                            <div
                                                onClick={() => toggleTerm(term.id)}
                                                className="flex items-center gap-2 cursor-pointer group"
                                            >
                                                {checkedTerms[term.id] ? (
                                                    <CheckSquare className="w-5 h-5 text-primary-500" />
                                                ) : (
                                                    <Square className="w-5 h-5 text-slate-400 group-hover:text-primary-400 transition-colors" />
                                                )}
                                                <span className="font-bold text-slate-700 select-none">[v] {term.title} 동의</span>
                                            </div>
                                            <div className="bg-white border p-4 text-left border-slate-200 rounded-xl h-32 overflow-y-auto text-sm text-slate-600 leading-relaxed custom-scrollbar">
                                                {term.content.split('\n').map((line, i) => (
                                                    <React.Fragment key={i}>
                                                        {line}
                                                        <br />
                                                    </React.Fragment>
                                                ))}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>

            <div className="flex justify-between items-center pt-8 border-t border-slate-200 mt-8">
                <button
                    onClick={onPrev}
                    className="px-6 py-3 text-slate-500 hover:text-slate-700 font-medium transition-colors"
                >
                    이전으로
                </button>
                <button
                    onClick={onNext}
                    disabled={!isAllChecked()}
                    className={`px-8 py-3 font-bold rounded-xl shadow-lg transition-colors flex items-center gap-2 group ${isAllChecked()
                        ? 'bg-primary-600 text-white hover:bg-primary-700'
                        : 'bg-slate-200 text-slate-400 cursor-not-allowed shadow-none'
                        }`}
                >
                    {buttonText}
                    <ArrowRight className={`w-5 h-5 ${isAllChecked() ? 'group-hover:translate-x-1 transition-transform' : ''}`} />
                </button>
            </div>
        </div>
    );
};

export default TermsCheck;
