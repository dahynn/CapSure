import React from 'react';
import BaseModal from './BaseModal';

/**
 * TextModal
 * 타이틀과 텍스트 내용만을 보여주는 범용 모달
 */
const TextModal = ({ title, contents, ...baseProps }) => {
    return (
        <BaseModal {...baseProps}>
            <div className="flex flex-col gap-3 text-center py-2">
                {title && (
                    <h3 className="text-xl font-bold text-slate-800">
                        {title}
                    </h3>
                )}
                {contents && (
                    <p className="text-slate-600 leading-relaxed font-medium">
                        {contents}
                    </p>
                )}
            </div>
        </BaseModal>
    );
};

export default TextModal;
