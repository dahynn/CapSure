import React from 'react';

const WelcomeHeader = ({ user }) => {
    return (
        <section className="animate-in slide-in-from-top-4 duration-500">
            <h1 className="text-h1 text-white mb-1">
                {user.name} 님,
            </h1>
            <p className="text-body-lg mt-1" style={{ color: 'var(--color-brand-gray)' }}>
                오늘도 당신의 일상을 안전하게 보관 중입니다.
            </p>
        </section>
    );
};

export default WelcomeHeader;
