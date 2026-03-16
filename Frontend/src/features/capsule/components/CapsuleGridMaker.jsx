import React from 'react';
import { Plus, Info } from 'lucide-react';

const CapsuleGridMaker = ({ selectedCells, handleRemoveItem, handleSubscribeConfirm, targetAmount }) => {
    return (
        <div className="bg-white p-6 md:p-8 rounded-3xl shadow-sm border border-slate-200 min-h-[500px] flex flex-col h-full">
            <div className="flex justify-between items-end mb-6">
                <div>
                    <h2 className="text-2xl font-bold text-slate-800">캡슐 조합하기</h2>
                    <p className="text-slate-500 mt-1">원하는 카테고리의 캡슐을 선택하여 채워주세요.</p>
                </div>
                <div className="flex items-center gap-2 bg-slate-50 px-4 py-2 rounded-xl border border-slate-200">
                    <span className="text-sm font-bold text-slate-600">목표:</span>
                    <span className="text-xl font-black text-primary-600">{targetAmount}만원</span>
                </div>
            </div>

            <div className="bg-slate-100 rounded-2xl flex-1 border-2 border-dashed border-slate-300 p-6 md:p-8 grid grid-cols-5 gap-2 content-start">
                {/* Dynamic Grid Cells based on targetAmount and selection */}
                {selectedCells.map((cell, i) => (
                    <div
                        key={i}
                        onClick={() => cell && handleRemoveItem(cell.groupId)}
                        className={`aspect-square rounded-xl border flex flex-col items-center justify-center transition-all ${cell ? `${cell.category.color} cursor-pointer hover:opacity-80` : 'bg-white/50 border-slate-200'
                            }`}
                    >
                        {cell ? (
                            <span className="font-bold text-[10px] sm:text-xs text-center px-1 leading-tight">{cell.name}</span>
                        ) : (
                            <Plus className="w-6 h-6 text-slate-300" />
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default CapsuleGridMaker;
