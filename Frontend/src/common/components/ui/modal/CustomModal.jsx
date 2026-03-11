import React from 'react';
import BaseModal from './BaseModal';

/**
 * CustomModal
 * 내부에 테이블, 폼 등 복잡한 내용을 전달할 때 BaseModal을 감싸기 위한 컴포넌트
 */
const CustomModal = ({ children, ...baseProps }) => {
    return (
        <BaseModal {...baseProps}>
            {children}
        </BaseModal>
    );
};

export default CustomModal;
