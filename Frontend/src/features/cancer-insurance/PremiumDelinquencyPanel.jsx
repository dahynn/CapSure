import React, { useCallback, useEffect, useRef, useState } from 'react';
import { getDelinquencyRuns, runPremiumDelinquency, resumePremiumDelinquency } from './api/operations.api';

const labels = { COMPLETED: '완료', RUNNING: '진행 중', FAILED: '실패 · 재개 가능' };

export default function PremiumDelinquencyPanel({ onUpdated }) {
  const [runs, setRuns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reason, setReason] = useState('');
  const [pending, setPending] = useState(false);
  const requestKey = useRef(null);
  const inFlight = useRef(false);

  const load = useCallback(async () => {
    try {
      setRuns(await getDelinquencyRuns());
      setError('');
    } catch (err) {
      setError(err.message || '미납 배치 이력을 불러오지 못했습니다.');
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
    if (!runId && !requestKey.current) requestKey.current = `MANUAL-DELINQUENCY-${crypto.randomUUID()}`;
    try {
      const result = runId
        ? await resumePremiumDelinquency(runId, reason.trim())
        : await runPremiumDelinquency(requestKey.current, reason.trim());
      requestKey.current = null;
      setNotice(`실행 #${result.runId} ${labels[result.status] || result.status} · 처리 ${result.processedCount}/${result.targetCount}건 · 독촉 실패 ${result.noticeFailedCount}건`);
      setReason('');
      await load();
      await onUpdated();
    } catch (err) {
      setError(`${err.message || '실행 결과를 확인하지 못했습니다.'} 새 실행을 만들기 전에 같은 요청을 재시도하거나 실행 이력을 확인하세요.`);
    } finally {
      inFlight.current = false;
      setPending(false);
    }
  };

  return (
    <section aria-labelledby="delinquency-title" className="rounded-2xl border border-amber-300/20 bg-[#09111F] p-5">
      <p className="text-xs font-bold text-amber-200">PREMIUM DELINQUENCY · SIMULATION</p>
      <h2 id="delinquency-title" className="mt-1 text-lg font-black text-white">미납 점검 · 독촉 · 계약 효력</h2>
      <p className="mt-2 text-xs leading-5 text-slate-400">한국시간 기준으로 미수액과 유예 종료일을 확인합니다. 독촉은 모의 처리이며 실제 메시지나 출금은 발생하지 않습니다. 실효 후 입금은 자동 부활하지 않습니다.</p>
      <label htmlFor="delinquency-reason" className="mt-4 block text-xs font-bold text-slate-300">미납 점검·재개 사유</label>
      <textarea id="delinquency-reason" maxLength={500} rows={2} value={reason} disabled={pending}
        onChange={(e) => setReason(e.target.value)} placeholder="예: 미납 계약과 독촉 이력의 일일 점검"
        className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white disabled:opacity-50" />
      <div className="mt-3 flex flex-wrap gap-2">
        <button type="button" disabled={pending || !reason.trim()} onClick={() => execute()}
          className="rounded-xl bg-amber-200 px-4 py-2 text-xs font-black text-slate-950 disabled:opacity-40">
          {pending ? '처리 중…' : requestKey.current ? '같은 요청 재시도' : '미납 배치 실행'}
        </button>
        <button type="button" disabled={pending} onClick={load} className="rounded-xl border border-slate-700 px-4 py-2 text-xs text-slate-300">실행 이력 갱신</button>
      </div>
      {error && <p role="alert" className="mt-3 text-xs leading-5 text-rose-300">{error}</p>}
      {notice && <p role="status" className="mt-3 text-xs text-emerald-200">{notice}</p>}
      <div className="mt-4 space-y-3">
        {loading ? <p className="text-xs text-slate-500">실행 이력을 불러오는 중입니다.</p>
          : runs.length === 0 ? <p className="text-xs text-slate-500">아직 미납 배치 실행 이력이 없습니다.</p> : runs.map((run) => (
            <article key={run.runId} className="rounded-xl border border-slate-800 p-3 text-xs">
              <p className="font-bold text-white">실행 #{run.runId} · {run.businessDate} · {labels[run.status] || run.status}</p>
              <p className="mt-2 text-slate-400">대상 {run.targetCount} · 처리 {run.processedCount} · 변경 {run.changedCount} · 유지 {run.unchangedCount} · 남음 {run.remainingCount}</p>
              <p className={`mt-1 ${run.noticeFailedCount || !run.controlTotalMatched ? 'text-rose-300' : 'text-emerald-300'}`}>독촉 실패 {run.noticeFailedCount} · 처리 합계 {run.controlTotalMatched ? '일치' : '불일치'}</p>
              {run.noticeFailedCount > 0 && <p className="mt-1 text-amber-200">독촉 실패 건은 새 점검 실행에서 재시도합니다.</p>}
              {run.errorReason && <p className="mt-1 text-rose-300">{run.errorReason}</p>}
              {['FAILED', 'RUNNING'].includes(run.status) && <button type="button" disabled={pending || !reason.trim()}
                onClick={() => execute(run.runId)} className="mt-3 rounded-lg border border-amber-200/40 px-3 py-2 text-amber-200 disabled:opacity-40">실행 #{run.runId} 재개</button>}
            </article>
          ))}
      </div>
    </section>
  );
}
