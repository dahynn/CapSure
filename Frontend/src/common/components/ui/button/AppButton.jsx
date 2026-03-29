import React from 'react';

const SIZE_STYLES = {
    md: 'py-3.5 text-[15px] rounded-xl',
    lg: 'py-4 text-base rounded-2xl',
};

const TONE_STYLES = {
    primary: 'bg-brand-blue text-[#020715] hover:bg-[#6BC1E6] active:scale-[0.98]',
    subtle: 'bg-slate-800 text-slate-300 hover:bg-slate-700',
    disabled: 'bg-slate-800 text-slate-500 cursor-not-allowed',
};

const AppButton = ({
    children,
    className = '',
    type = 'button',
    size = 'lg',
    tone = 'primary',
    fullWidth = true,
    disabled = false,
    ...props
}) => {
    const resolvedTone = disabled ? 'disabled' : tone;
    const widthClass = fullWidth ? 'w-full' : '';
    const sizeClass = SIZE_STYLES[size] ?? SIZE_STYLES.lg;
    const toneClass = TONE_STYLES[resolvedTone] ?? TONE_STYLES.primary;

    return (
        <button
            type={type}
            disabled={disabled}
            className={`${widthClass} ${sizeClass} font-bold transition-all flex items-center justify-center gap-2 ${toneClass} ${className}`.trim()}
            {...props}
        >
            {children}
        </button>
    );
};

export default AppButton;
