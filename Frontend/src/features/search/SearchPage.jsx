import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Search, X, History, TrendingUp } from 'lucide-react';

const SearchPage = () => {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');
    const inputRef = useRef(null);

    // Auto focus on load
    useEffect(() => {
        if (inputRef.current) {
            inputRef.current.focus();
        }
    }, []);

    const recentSearches = ['암보험', '카카오페이 캡슐', '여행자보험', '가족 지킴이'];
    const popularSearches = ['해외여행', '실손의료비', '자동차보험', '건강검진', '펫보험'];

    return (
        <div className="min-h-screen bg-black flex flex-col items-center">
            <div className="w-full max-w-[560px] flex flex-col min-h-screen relative shadow-2xl animate-in slide-in-from-right-full fade-in duration-300 ease-out" style={{ backgroundColor: '#020715' }}>
                {/* Search Header */}
                <header className="sticky top-0 z-50 w-full flex items-center px-4 py-3 border-b border-slate-800/50 bg-[#020715]">
                    <button 
                        onClick={() => navigate(-1)} 
                        className="p-2 mr-2 text-white hover:bg-slate-800/50 rounded-full transition-colors"
                    >
                        <ChevronLeft className="w-6 h-6" />
                    </button>
                    
                    <div className="flex-1 relative flex items-center">
                        <Search className="absolute left-3 w-4 h-4 text-slate-400" />
                        <input 
                            ref={inputRef}
                            type="text" 
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            placeholder="찾으시는 캡슐이나 보험이 있나요?" 
                            className="w-full bg-[#161B26] border border-slate-700/50 text-white rounded-xl py-2.5 pl-10 pr-10 focus:outline-none focus:border-[#82D8FC]/50 transition-colors text-[14px]"
                        />
                        {query && (
                            <button 
                                onClick={() => setQuery('')}
                                className="absolute right-3 p-1 text-slate-400 hover:text-white rounded-full bg-slate-800/50"
                            >
                                <X className="w-3.5 h-3.5" />
                            </button>
                        )}
                    </div>
                    
                    <button 
                        onClick={() => navigate(-1)}
                        className="p-2 ml-2 text-[#82D8FC] font-semibold text-[14px] hover:text-white transition-colors"
                    >
                        취소
                    </button>
                </header>

                <main className="flex-1 p-6 space-y-10">
                    {!query ? (
                        <>
                            {/* 최근 검색어 */}
                            <section className="animate-in fade-in duration-300">
                                <div className="flex justify-between items-center mb-4">
                                    <h2 className="text-[15px] font-bold text-white flex items-center gap-2">
                                        <History className="w-4 h-4 text-slate-400" />
                                        최근 검색어
                                    </h2>
                                    <button className="text-[12px] text-slate-500 hover:text-slate-400">전체삭제</button>
                                </div>
                                <div className="flex flex-wrap gap-2.5">
                                    {recentSearches.map(term => (
                                        <span key={term} className="px-3 py-1.5 rounded-full bg-[#161B26] border border-slate-800 text-slate-300 text-[13px] flex items-center gap-1.5 hover:bg-slate-800/50 transition-colors cursor-pointer">
                                            {term}
                                            <X className="w-3 h-3 text-slate-500 ml-1 hover:text-white" />
                                        </span>
                                    ))}
                                </div>
                            </section>

                            <hr className="border-slate-800/50" />

                            {/* 인기 검색어 */}
                            <section className="animate-in fade-in duration-300 delay-75 fill-mode-both">
                                <h2 className="text-[15px] font-bold text-white flex items-center gap-2 mb-4">
                                    <TrendingUp className="w-4 h-4 text-[#F6CD3C]" />
                                    급상승 검색어
                                </h2>
                                <div className="flex flex-col gap-1">
                                    {popularSearches.map((term, idx) => (
                                        <div key={term} className="flex items-center gap-4 py-3 px-2 hover:bg-slate-800/30 rounded-xl cursor-pointer transition-colors">
                                            <span className={`text-[15px] font-bold w-4 text-center ${idx < 3 ? 'text-[#82D8FC]' : 'text-slate-500'}`}>
                                                {idx + 1}
                                            </span>
                                            <span className="text-[15px] text-slate-200 font-medium">
                                                {term}
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            </section>
                        </>
                    ) : (
                        /* 검색 결과 영역 */
                        <div className="flex flex-col items-center justify-center py-24 text-center animate-in fade-in duration-200">
                            <Search className="w-12 h-12 text-slate-700 mb-4" />
                            <p className="text-slate-400 text-[15px] leading-relaxed">
                                <span className="text-white font-bold">'{query}'</span> 에 대한<br/>검색 결과가 없습니다.
                            </p>
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
};

export default SearchPage;
