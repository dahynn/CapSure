import React from 'react';
import { Check, ChevronRight } from 'lucide-react';

const PageTransitionLoading = ({
    message = '페이지로 이동했어요',
    backgroundClassName = 'bg-[#020715]',
    openDelayMs = 100,
    textDelayMs = 920,
    doneDelayMs = 1220,
}) => {
    const textRef = React.useRef(null);
    const [targetWidth, setTargetWidth] = React.useState(280);
    const [expanded, setExpanded] = React.useState(false);
    const [textVisible, setTextVisible] = React.useState(false);
    const [done, setDone] = React.useState(false);

    React.useEffect(() => {
        const measure = () => {
            const textWidth = textRef.current?.scrollWidth ?? 0;
            const rawWidth = 12 + 36 + 12 + textWidth + 14;
            const maxWidth = Math.max(240, window.innerWidth - 48);
            const clamped = Math.min(rawWidth, maxWidth);
            setTargetWidth(Math.ceil(clamped));
        };

        measure();
        window.addEventListener('resize', measure);

        return () => {
            window.removeEventListener('resize', measure);
        };
    }, [message]);

    React.useEffect(() => {
        const expandTimer = window.setTimeout(() => {
            setExpanded(true);
        }, openDelayMs);

        const textTimer = window.setTimeout(() => {
            setTextVisible(true);
        }, textDelayMs);

        const doneTimer = window.setTimeout(() => {
            setDone(true);
        }, doneDelayMs);

        return () => {
            window.clearTimeout(expandTimer);
            window.clearTimeout(textTimer);
            window.clearTimeout(doneTimer);
        };
    }, [openDelayMs, textDelayMs, doneDelayMs]);

    return (
        <div className={`fixed inset-0 z-[200] ${backgroundClassName} flex items-center justify-center px-6`}>
            <div className="flex flex-col items-center justify-center w-full max-w-[420px]">
                <div className="transition-loader-circle" aria-hidden="true" />

                <div
                    className={`transition-loader-snackbar transition-loader-snackbar-fixed ${expanded ? 'is-expanded' : ''} ${textVisible ? 'is-text-visible' : ''} ${done ? 'is-done' : ''}`}
                    role="status"
                    aria-live="polite"
                    style={{ width: expanded ? `${targetWidth}px` : '60px' }}
                >
                    <div className="transition-loader-icon-wrap" aria-hidden="true">
                        <div className="transition-loader-icon">
                            <ChevronRight className="transition-loader-chevron w-[18px] h-[18px] text-white" strokeWidth={3} />
                            <Check className="transition-loader-check w-[17px] h-[17px] text-white" strokeWidth={3} />
                        </div>
                    </div>
                    <p ref={textRef} className="transition-loader-text">
                        {message}
                    </p>
                </div>
            </div>
        </div>
    );
};

export default PageTransitionLoading;
