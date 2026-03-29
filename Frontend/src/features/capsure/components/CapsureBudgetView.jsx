import React, { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import {
    Droplets,
    HeartPulse,
    Scissors,
    Shield,
    ShieldAlert,
    TriangleAlert,
    Zap,
} from 'lucide-react';
import AppButton from '@/common/components/ui/button/AppButton';

const GRID_SIZE = 3;
const CELL_SIZE = 90;
const CELL_GAP = 10;
const QUESTION_KEY = 'question';
const STEP1_MOVE_GAP_MS = 260;
const STEP1_QUESTION_SETTLE_MS = 340;
const STEP2_STAGGER_MS = 170;
const STEP3_MOVE_GAP_MS = 180;
const LOOP_PAUSE_MS = 1500;
const CTA_EXTRA_GAP_PX = 16;

const CARD_SPRING_TRANSITION = {
    type: 'spring',
    bounce: 0.14,
    damping: 34,
    stiffness: 118,
    mass: 1,
};

const TILE_MAP = {
    'brain-heart': { label: '뇌/심장', icon: HeartPulse, tone: 'purple' },
    injury: { label: '상해', icon: ShieldAlert, tone: 'blue' },
    cancer: { label: '암', icon: Zap, tone: 'yellow' },
    death: { label: '사망', icon: TriangleAlert, tone: 'blue' },
    'actual-loss': { label: '실손', icon: Droplets, tone: 'purple' },
    surgery: { label: '수술', icon: Scissors, tone: 'yellow' },
    [QUESTION_KEY]: { label: '?', icon: null, tone: 'question' },
};

const INITIAL_BOARD = [
    'brain-heart',
    'injury',
    null,
    'cancer',
    QUESTION_KEY,
    'death',
    'actual-loss',
    'surgery',
    null,
];

const STACK_TONE_STYLES = {
    blue: {
        card: 'border-[#3a86c6]/90 bg-[#0f2034]/72',
        iconWrap: 'bg-[#142740]',
        icon: 'text-[#82D8FC]',
    },
    purple: {
        card: 'border-[#7a66a8]/90 bg-[#201a35]/74',
        iconWrap: 'bg-[#2c2442]',
        icon: 'text-[#F2BEF7]',
    },
    yellow: {
        card: 'border-[#8f7a27]/90 bg-[#242014]/76',
        iconWrap: 'bg-[#272417]',
        icon: 'text-[#F6CD3C]',
    },
    question: {
        card: 'border-dashed border-slate-200/85 bg-[#0b1322] shadow-[0_8px_24px_rgba(120,185,255,0.22)]',
        iconWrap: 'bg-transparent',
        icon: 'text-white',
    },
};

const isOrthogonalMove = (from, to) => {
    if (from === to) {
        return false;
    }

    const fromRow = Math.floor(from / GRID_SIZE);
    const fromCol = from % GRID_SIZE;
    const toRow = Math.floor(to / GRID_SIZE);
    const toCol = to % GRID_SIZE;

    return Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol) === 1;
};

const moveTile = (board, from, to) => {
    if (!isOrthogonalMove(from, to)) {
        return board;
    }

    if (board[from] === null || board[to] !== null) {
        return board;
    }

    const nextBoard = [...board];
    nextBoard[to] = nextBoard[from];
    nextBoard[from] = null;
    return nextBoard;
};

const getCellPosition = (index) => {
    const row = Math.floor(index / GRID_SIZE);
    const col = index % GRID_SIZE;
    return {
        top: row * (CELL_SIZE + CELL_GAP),
        left: col * (CELL_SIZE + CELL_GAP),
    };
};

const PuzzleCard = ({ tileKey, index, highlightQuestion }) => {
    const tile = TILE_MAP[tileKey];
    const style = STACK_TONE_STYLES[tile.tone] ?? STACK_TONE_STYLES.blue;
    const Icon = tile.icon;
    const isQuestion = tileKey === QUESTION_KEY;
    const cellPosition = getCellPosition(index);

    return (
        <motion.article
            layout="position"
            transition={CARD_SPRING_TRANSITION}
            animate={
                isQuestion
                    ? {
                          opacity: highlightQuestion ? 0.8 : 1,
                          scale: highlightQuestion ? 1.05 : 1,
                      }
                    : { opacity: 1, scale: 1 }
            }
            className={`absolute z-20 w-[90px] h-[90px] rounded-[28px] border ${style.card} shadow-[0_8px_20px_rgba(0,0,0,0.28)] flex flex-col items-center justify-center`}
            style={cellPosition}
        >
            {isQuestion ? (
                <p className="text-[40px] leading-none font-black text-white/95">?</p>
            ) : (
                <>
                    <div className={`w-10 h-10 rounded-[11px] ${style.iconWrap} flex items-center justify-center`}>
                        <Icon className={`w-[18px] h-[18px] ${style.icon}`} />
                    </div>
                    <p className="mt-1 text-white text-[12px] font-bold">{tile.label}</p>
                </>
            )}
        </motion.article>
    );
};

const CapsureBudgetView = ({ onProceed }) => {
    const [budgetInput, setBudgetInput] = useState('10000');
    const [board, setBoard] = useState(INITIAL_BOARD);
    const [highlightQuestion, setHighlightQuestion] = useState(false);
    const [ctaHeight, setCtaHeight] = useState(0);

    const boardRef = useRef(INITIAL_BOARD);
    const timeoutsRef = useRef([]);
    const questionTimeoutRef = useRef(null);
    const ctaRef = useRef(null);

    const clearQueuedTimers = () => {
        timeoutsRef.current.forEach((timerId) => window.clearTimeout(timerId));
        timeoutsRef.current = [];
    };

    const sleep = (ms) =>
        new Promise((resolve) => {
            const timerId = window.setTimeout(resolve, ms);
            timeoutsRef.current.push(timerId);
        });

    const triggerQuestionHighlight = () => {
        setHighlightQuestion(true);

        if (questionTimeoutRef.current) {
            window.clearTimeout(questionTimeoutRef.current);
        }

        questionTimeoutRef.current = window.setTimeout(() => {
            setHighlightQuestion(false);
        }, 320);
    };

    const runMove = (from, to) => {
        const movingTile = boardRef.current[from];
        if (movingTile === QUESTION_KEY) {
            triggerQuestionHighlight();
        }

        setBoard((prev) => {
            const next = moveTile(prev, from, to);
            boardRef.current = next;
            return next;
        });
    };

    useEffect(() => {
        let cancelled = false;

        const runOrganicFlow = async () => {
            while (!cancelled) {
                setBoard(INITIAL_BOARD);
                boardRef.current = INITIAL_BOARD;
                setHighlightQuestion(false);
                await sleep(420);
                if (cancelled) {
                    break;
                }

                // Step 1: 길 터주기
                runMove(5, 8);
                await sleep(STEP1_MOVE_GAP_MS);
                if (cancelled) {
                    break;
                }
                runMove(4, 5);
                await sleep(STEP1_QUESTION_SETTLE_MS);
                if (cancelled) {
                    break;
                }
                runMove(5, 2);
                await sleep(520);
                if (cancelled) {
                    break;
                }

                // Step 2: 판 흔들기
                const step2Moves = [
                    [7, 4],
                    [8, 7],
                    [2, 5],
                    [1, 2],
                    [0, 1],
                ];
                for (const [from, to] of step2Moves) {
                    runMove(from, to);
                    await sleep(STEP2_STAGGER_MS);
                    if (cancelled) {
                        break;
                    }
                }
                if (cancelled) {
                    break;
                }

                await sleep(320);
                if (cancelled) {
                    break;
                }

                // Step 3: 중앙 안착
                runMove(4, 0);
                await sleep(STEP3_MOVE_GAP_MS);
                if (cancelled) {
                    break;
                }
                runMove(5, 4);
                await sleep(LOOP_PAUSE_MS);
            }
        };

        runOrganicFlow();

        return () => {
            cancelled = true;
            clearQueuedTimers();
            if (questionTimeoutRef.current) {
                window.clearTimeout(questionTimeoutRef.current);
            }
        };
    }, []);

    useEffect(() => {
        const ctaElement = ctaRef.current;
        if (!ctaElement) {
            return;
        }

        const updateCtaHeight = () => {
            setCtaHeight(ctaElement.offsetHeight || 0);
        };

        updateCtaHeight();
        window.addEventListener('resize', updateCtaHeight);

        let observer = null;
        if (typeof ResizeObserver !== 'undefined') {
            observer = new ResizeObserver(updateCtaHeight);
            observer.observe(ctaElement);
        }

        return () => {
            if (observer) {
                observer.disconnect();
            }
            window.removeEventListener('resize', updateCtaHeight);
        };
    }, []);

    const contentPaddingBottom = `calc(var(--app-bottom-nav-height) + env(safe-area-inset-bottom) + ${ctaHeight + CTA_EXTRA_GAP_PX}px)`;

    return (
        <div
            className="flex flex-col min-h-screen"
            style={{ paddingBottom: contentPaddingBottom }}
        >
            <div className="px-6 pt-6 animate-in slide-in-from-right-8 duration-300">
                <h2 className="text-2xl font-black text-white leading-snug mb-2">
                    이번 달 보험료로
                    <br />
                    얼마가 적당할까요?
                </h2>
                <p className="text-slate-400 text-sm mb-8">맞춤형 캡슐 설계를 위해 목표 예산을 알려주세요.</p>

                <p className="text-slate-400 text-capsure-sm mb-2">목표 예산</p>
                <div className="flex items-center justify-between border-2 rounded-xl px-5 py-4 transition-colors border-slate-700 bg-[#0A0D14] focus-within:border-brand-blue focus-within:bg-capsure-card">
                    <input
                        type="number"
                        value={budgetInput}
                        onChange={(e) => setBudgetInput(e.target.value)}
                        className="w-full bg-transparent text-3xl font-bold tracking-tight text-white outline-none no-spinner"
                        placeholder="0"
                    />
                    <span className="text-white font-bold ml-2">원</span>
                </div>

                <div className="flex gap-2.5 mt-4 overflow-x-auto hide-scrollbar">
                    {[10000, 30000, 50000, 70000].map((amt) => (
                        <button
                            key={amt}
                            onClick={() => setBudgetInput(amt.toString())}
                            className={`px-5 py-2 whitespace-nowrap rounded-full text-capsure-base font-bold border transition-colors outline-none ${budgetInput === amt.toString() ? 'border-brand-blue text-brand-blue bg-brand-blue/10' : 'border-slate-700 text-slate-400 bg-capsure-card hover:border-slate-500 hover:text-slate-300'}`}
                        >
                            {amt / 10000}만원
                        </button>
                    ))}
                </div>
            </div>

            <div className="flex flex-col items-center mt-7 mb-2 animate-in fade-in duration-500">
                <div className="relative w-[290px] h-[290px] mb-6">
                    {board.map((tileKey, index) =>
                        tileKey ? (
                            <PuzzleCard
                                key={tileKey}
                                tileKey={tileKey}
                                index={index}
                                highlightQuestion={highlightQuestion}
                            />
                        ) : null
                    )}
                </div>

                <p className="text-center text-slate-400 text-capsure-base leading-relaxed">
                    설정하신 예산 내에서
                    <br />
                    최적의 보장 항목을 캡슐에 담아드릴게요.
                </p>
            </div>

            <div
                ref={ctaRef}
                className="fixed left-0 right-0 max-w-[560px] mx-auto px-6 pb-4 pt-6 bg-gradient-to-t from-[#020715] via-[#020715] to-transparent z-40"
                style={{ bottom: 'calc(var(--app-bottom-nav-height) + env(safe-area-inset-bottom) + 2px)' }}
            >
                <div className="flex justify-center flex-wrap gap-x-6 gap-y-2 mb-3 text-slate-500 text-capsure-sm font-bold">
                    <span className="flex items-center gap-1.5">
                        <Shield className="w-3.5 h-3.5" /> 안전한 보안 진단
                    </span>
                    <span className="flex items-center gap-1.5">⚡ 3초 빠른 설계</span>
                </div>
                <AppButton
                    onClick={() => {
                        onProceed(Number(budgetInput) || 10000);
                    }}
                    className="shadow-[0_0_20px_rgba(130,216,252,0.2)]"
                >
                    캡슐 설계 시작하기
                </AppButton>
            </div>
        </div>
    );
};

export default CapsureBudgetView;
