import React, { useState } from 'react';
import { Check, CheckCircle2 } from 'lucide-react';
import TextModal from '@/common/components/ui/modal/TextModal';

const dummyTerms = `
제1조 (목적)
본 약관은 회사가 제공하는 마이데이터 서비스와 관련하여 회사와 회원 간의 권리와 의무, 책임사항을 규정함을 목적으로 합니다.
본 서비스는 고객의 편리한 보험 상품 추천을 위해 다양한 금융 기관에 흩어져 있는 고객의 개인신용정보를 하나의 화면에서 조회하고 관리할 수 있도록 지원합니다.

제2조 (용어의 정의)
① "마이데이터 서비스"란 회원이 가입한 금융기관 등에 흩어져 있는 개인신용정보를 일정한 조건하에 통합하여 조회하고 관리할 수 있도록 하는 서비스를 말합니다.
② "회원"이란 본 약관에 동의하고 회사가 제공하는 마이데이터 서비스를 이용하는 개인을 말합니다.
③ "정보제공자"란 회원의 요구에 따라 마이데이터 사업자에게 신용정보를 제공하는 금융회사 등을 말합니다.

제3조 (서비스의 내용)
회사는 회원에게 다음 각 호의 서비스를 제공합니다.
1. 분산된 개인신용정보의 통합 조회 및 관리 서비스
2. 수집된 정보를 기반으로 한 맞춤형 보험 상품 추천 서비스
3. 기타 회사가 추가 개발하거나 제휴 계약 등을 통해 회원에게 제공하는 서비스

제4조 (개인신용정보 전송 요구 및 철회)
회원은 관련 법령에 따라 정보제공자에게 본인의 개인신용정보를 마이데이터 사업자에게 전송할 것을 요구할 수 있으며, 언제든지 전송 요구를 철회할 수 있습니다.
 
제5조 (정보의 보호 및 보안)
회사는 회원의 정보를 안전하게 보호하기 위해 관련 법령을 준수하며 기술적, 관리적 보안 조치를 취합니다.
`;

const MyDataConsentStep = ({ onNext }) => {
    const [isConsentChecked, setIsConsentChecked] = useState(false);
    const [isModalOpen, setIsModalOpen] = useState(false);

    // 체크박스 클릭 핸들러 (모달 오픈)
    const handleCheckboxClick = () => {
        // 이미 체크되어 있으면 해제 (모달 없이)
        if (isConsentChecked) {
            setIsConsentChecked(false);
            return;
        }
        // 체크 안되어 있으면 모달 오픈
        setIsModalOpen(true);
    };

    // 모달 확인 핸들러
    const handleModalConfirm = () => {
        setIsConsentChecked(true);
        setIsModalOpen(false);
    };

    // 모달 닫기 핸들러
    const handleModalClose = () => {
        setIsModalOpen(false);
    };

    // 폼 제출 (다음 단계)
    const handleSubmit = (e) => {
        e.preventDefault();
        if (!isConsentChecked) {
            alert('마이데이터 활용에 동의하셔야 다음 단계로 진행할 수 있습니다.');
            return;
        }
        onNext();
    };

    return (
        <div className="flex flex-col h-full animate-in fade-in slide-in-from-right-8 duration-300">
            <h2 className="text-2xl font-bold text-slate-800 mb-6">
                마이데이터 연동
            </h2>

            <form onSubmit={handleSubmit} className="flex-1 flex flex-col">
                <div className="flex-1 overflow-y-auto">
                    {/* 체크박스 영역 */}
                    <div 
                        className="flex items-center gap-3 p-4 mb-3 border border-slate-200 rounded-xl cursor-pointer hover:bg-slate-50 transition-colors"
                        onClick={handleCheckboxClick}
                    >
                        <div className={`w-6 h-6 rounded flex items-center justify-center border transition-colors ${
                            isConsentChecked 
                            ? 'bg-primary-500 border-primary-500 text-white' 
                            : 'border-slate-300 bg-white'
                        }`}>
                            {isConsentChecked && <Check className="w-4 h-4" />}
                        </div>
                        <span className="font-bold text-slate-800 text-lg">
                            마이데이터 동의
                        </span>
                    </div>

                    <p className="text-xs text-slate-500 mb-4 px-2">
                        서비스 사용을 위해서는 마이데이터 활용 동의가 필요해요!
                    </p>

                    {/* 약관 영역 */}
                    <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 h-64 overflow-y-auto text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">
                        {dummyTerms}
                    </div>
                </div>

                <div className="pt-6 mt-auto">
                    <button
                        type="submit"
                        disabled={!isConsentChecked}
                        className={`w-full py-4 rounded-xl font-bold text-white shadow-lg transition-all flex justify-center items-center ${
                            isConsentChecked
                            ? 'bg-primary-600 hover:bg-primary-700 shadow-primary-200'
                            : 'bg-slate-300 cursor-not-allowed shadow-none'
                        }`}
                    >
                        다음으로
                    </button>
                </div>
            </form>

            {/* 마이데이터 동의 확인 모달 */}
            <TextModal
                isOpen={isModalOpen}
                onClose={handleModalClose}
                onConfirm={handleModalConfirm}
                contents="마이데이터 활용에 동의하시나요?"
                confirmText="확인"
                cancelText="취소"
            />
        </div>
    );
};

export default MyDataConsentStep;
