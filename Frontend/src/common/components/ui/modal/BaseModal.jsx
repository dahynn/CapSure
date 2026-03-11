import React, { useEffect } from 'react';

/**
 * BaseModal
 * 모든 커스텀 모달의 기본이 되는 레이아웃 컴포넌트
 */
const BaseModal = ({
    isOpen,
    onClose,
    onConfirm,
    children,
    hideCancel = false,
    hideConfirm = false,
    confirmText = '확인',
    cancelText = '취소'
}) => {
    // ESC 키로 모달 닫기
    useEffect(() => {
        const handleEsc = (e) => {
            if (e.key === 'Escape' && onClose) {
                onClose();
            }
        };
        window.addEventListener('keydown', handleEsc);
        return () => window.removeEventListener('keydown', handleEsc);
    }, [onClose]);

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200"
                onClick={onClose}
            />

            {/* Modal Box */}
            <div className="relative bg-white rounded-3xl w-full max-w-sm shadow-2xl animate-in zoom-in-95 duration-200 overflow-hidden flex flex-col">
                {/* Content Area */}
                <div className="p-6">
                    {children}
                </div>

                {/* Footer / Actions */}
                {(!hideCancel || !hideConfirm) && (
                    <div className="flex border-t border-slate-100">
                        {!hideCancel && (
                            <button
                                onClick={onClose}
                                className={`flex-1 py-4 text-sm font-medium text-slate-500 hover:bg-slate-50 hover:text-slate-700 transition-colors ${!hideConfirm ? 'border-r border-slate-100' : ''}`}
                            >
                                {cancelText}
                            </button>
                        )}
                        {!hideConfirm && (
                            <button
                                onClick={onConfirm}
                                className="flex-1 py-4 text-sm font-bold text-primary-600 hover:bg-primary-50 transition-colors"
                            >
                                {confirmText}
                            </button>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default BaseModal;
