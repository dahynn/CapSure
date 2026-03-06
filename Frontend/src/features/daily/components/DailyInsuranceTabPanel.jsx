import React from 'react';

const DailyInsuranceTabPanel = ({ tabName }) => {
    return (
        <div className="bg-white p-6 animate-in fade-in">
            <h3 className="text-lg font-bold text-slate-800 mb-4">{tabName} 보험 탭 패널입니다</h3>
            <p className="text-slate-500 text-sm">{tabName}에 해당하는 다양한 추천 보험 상품들이 이곳에 나열될 예정입니다. 레이아웃은 공통 컴포넌트를 사용하여 동일하게 구성됩니다.</p>
        </div>
    );
};

export default DailyInsuranceTabPanel;
