import React, { useCallback, useEffect, useRef, useState } from 'react';
import { getPremiumBillingRuns, resumePremiumBilling, runPremiumBilling } from './api/operations.api';

const labels = { COMPLETED: '완료', RUNNING: '진행 중', FAILED: '실패 · 재개 가능' };
const currentMonth = () => new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Seoul' }).slice(0, 7);

export default function PremiumBillingPanel({ onUpdated }) {
  const [runs, setRuns] = useState([]);
  const [billingMonth, setBillingMonth] = useState(currentMonth);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const requestKey = useRef(null);
  const inFlight = useRef(false);

  const load = useCallback(async () => {
    try {
      setRuns(await getPremiumBillingRuns());
      setError('');
    } catch (requestError) {
      setError(requestError.message || '정기 채권 실행 이력을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const timer = setInterval(load, 30000);
    return () => clearInterval(timer);
  }, [load]);

  const execute = async (runId = null) => {
    if (!reason.trim() || inFlight.current) return;
    inFlight.current = true;
    setPending(true);
    setError('');
    setNotice('');
    if (!runId && !requestKey.current) requestKey.current = `MANUAL-PREMIUM-BILLING-${crypto.randomUUID()}`;
    try {
      const result = runId
        ? await resumePremiumBilling(runId, reason.trim())
        : await runPremiumBilling(requestKey.current, `${billingMonth}-01`, reason.trim());
      requestKey.current = null;
      setReason('');
      setNotice(`실행 #${result.runId} ${labels[result.status] || result.status} · 생성 ${result.createdCount}건 · 기존 ${result.existingCount}건`);
      await load();
      await onUpdated?.();
    } catch (requestError) {
      setError(`${requestError.message || '실행 결과를 확인하지 못했습니다.'} 같은 요청 키로 재시도하거나 실행 이력을 확인하세요.`);
    } finally {
      inFlight.current = false;
      setPending(false);
    }
  };

  return (
    <section aria-labelledby="premium-billing-title" className="rounded-2xl border border-sky-300/20 bg-[#09111F] p-5">
      <p className="text-xs font-bold text-sky-200">RECURRING PREMIUM · SIMULATION</p>
      <h2 id="premium-billing-title" className="mt-1 text-lg font-black text-white">월 정기 보험료 채권 생성</h2>
      <p className="mt-2 text-xs leading-5 text-slate-400">
        전월 이전에 활성화된 유효 계약의 월 보험료를 증권 스냅샷에서 읽어 채권과 모의 자동출금 지시를 만듭니다. 동일 계약·청구월은 한 건만 유지됩니다.
      </p>
      <div className="mt-4 grid gap-3 sm:grid-cols-[150px_1fr]">
        <label className="text-xs font-bold text-slate-300">
          청구월
          <input type="month" max={currentMonth()} value={billingMonth} disabled={pending}
            onChange={(event) => setBillingMonth(event.target.value)}
            className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white disabled:opacity-50" />
        </label>
        <label className="text-xs font-bold text-slate-300">
          실행·재개 사유
          <textarea maxLength={500} rows={2} value={reason} disabled={pending}
            onChange={(event) => setReason(event.target.value)} placeholder="예: 9월 정기 보험료 채권 생성"
            className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white disabled:opacity-50" />
        </label>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        <button type="button" disabled={pending || !reason.trim() || !billingMonth} onClick={() => execute()}
          className="rounded-xl bg-sky-200 px-4 py-2 text-xs font-black text-slate-950 disabled:opacity-40">
          {pending ? '처리 중…' : requestKey.current ? '같은 요청 재시도' : '정기 채권 생성'}
        </button>
        <button type="button" disabled={pending} onClick={load}
          className="rounded-xl border border-slate-700 px-4 py-2 text-xs text-slate-300">실행 이력 갱신</button>
      </div>
      {error && <p role="alert" className="mt-3 text-xs leading-5 text-rose-300">{error}</p>}
      {notice && <p role="status" className="mt-3 text-xs text-emerald-200">{notice}</p>}
      <div className="mt-4 space-y-3">
        {loading ? <p className="text-xs text-slate-500">실행 이력을 불러오는 중입니다.</p>
          : runs.length === 0 ? <p className="text-xs text-slate-500">아직 정기 채권 실행 이력이 없습니다.</p>
            : runs.map((run) => (
              <article key={run.runId} className="rounded-xl border border-slate-800 p-3 text-xs">
                <p className="font-bold text-white">실행 #{run.runId} · {run.billingCycle} · {labels[run.status] || run.status}</p>
                <p className="mt-2 text-slate-400">대상 {run.targetCount} · 생성 {run.createdCount} · 기존 {run.existingCount} · 제외 {run.ineligibleCount} · 남음 {run.remainingCount}</p>
                <p className={`mt-1 ${run.controlTotalMatched ? 'text-emerald-300' : 'text-rose-300'}`}>
                  처리 합계 {run.controlTotalMatched ? '일치' : '불일치'}
                </p>
                {run.errorReason && <p className="mt-1 text-rose-300">{run.errorReason}</p>}
                {['FAILED', 'RUNNING'].includes(run.status) && (
                  <button type="button" disabled={pending || !reason.trim()} onClick={() => execute(run.runId)}
                    className="mt-3 rounded-lg border border-sky-200/40 px-3 py-2 text-sky-200 disabled:opacity-40">
                    실행 #{run.runId} 재개
                  </button>
                )}
              </article>
            ))}
      </div>
    </section>
  );
}
