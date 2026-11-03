import React, { useEffect, useState } from 'react';
import { getPendingCoverageChanges, decideCoverageChange } from './api/operations.api';

const won = (value) => `${new Intl.NumberFormat('ko-KR').format(Number(value || 0))}원`;
const date = (value) => new Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul', dateStyle: 'medium' }).format(new Date(value));

export default function CoverageChangeReviewPanel() {
  const [requests, setRequests] = useState([]);
  const [reasons, setReasons] = useState({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    getPendingCoverageChanges().then((rows) => { if (active) setRequests(rows); })
      .catch((e) => { if (active) setError(e.message); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  const refresh = async () => {
    setBusy(true); setError('');
    try { setRequests(await getPendingCoverageChanges()); } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  };
  const decide = async (request, decision) => {
    setBusy(true); setError(''); setNotice('');
    try {
      const result = await decideCoverageChange(request.changeRequestId, decision, reasons[request.changeRequestId]?.trim());
      setNotice(result.status === 'SCHEDULED' ? '승인된 보장이 적용 예약됐습니다.' : result.status === 'REJECTED' ? '반려 사유가 기록됐습니다.' : '신청 기한이 만료되어 적용되지 않았습니다.');
      setRequests(await getPendingCoverageChanges());
    } catch (e) { setError(e.message); } finally { setBusy(false); }
  };
  return (
    <section className="mt-6 rounded-2xl border border-slate-700 bg-slate-900 p-5 text-white">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">보장 변경 심사</h2>
        <button type="button" disabled={busy} onClick={refresh} className="rounded-lg border border-slate-600 px-3 py-2 text-sm disabled:opacity-40">목록 새로고침</button>
      </div>
      <p className="mt-2 text-sm text-slate-400">모의 상품의 중도 변경 조건을 검토한 뒤 판단합니다. 자기 신청은 심사할 수 없으며, 승인 전에는 계약과 보험료가 유지됩니다.</p>
      {loading && <p role="status" className="mt-3 text-sm">심사 목록을 불러오고 있습니다.</p>}
      {error && <p role="alert" className="mt-3 text-sm text-rose-300">{error}</p>}
      {notice && <p role="status" className="mt-3 text-sm text-sky-200">{notice}</p>}
      {!loading && !requests.length && !error && <p className="mt-4 text-sm text-slate-400">심사 대기 중인 신청이 없습니다.</p>}
      <div className="mt-4 space-y-4">
        {requests.map((request) => (
          <article key={request.changeRequestId} className="rounded-xl border border-slate-700 p-4">
            <h3 className="font-semibold">변경 신청 #{request.changeRequestId} · 계약 #{request.policyId}</h3>
            <p className="mt-2 text-sm">희망 적용일 {date(request.effectiveAt)} · 변경 후 월 {won(request.quote.monthlyPremium)}</p>
            <p className="mt-2 text-sm text-slate-300">{request.quote.coverages.map((coverage) => coverage.coverageName).join(', ')}</p>
            <label className="mt-3 block text-sm" htmlFor={`change-reason-${request.changeRequestId}`}>판단 사유</label>
            <textarea id={`change-reason-${request.changeRequestId}`} maxLength={500} disabled={busy}
              value={reasons[request.changeRequestId] || ''} onChange={(e) => setReasons((old) => ({ ...old, [request.changeRequestId]: e.target.value }))}
              placeholder="변경 가능 조건과 검토 결과를 입력하세요. 개인·건강정보 원문은 입력하지 마세요."
              className="mt-2 min-h-20 w-full rounded-lg border border-slate-600 bg-slate-950 p-3 text-sm" />
            <div className="mt-3 flex gap-3">
              <button type="button" disabled={busy || !reasons[request.changeRequestId]?.trim()} onClick={() => decide(request, 'APPROVE')}
                className="rounded-lg bg-sky-300 px-4 py-2 font-semibold text-slate-950 disabled:opacity-40">승인</button>
              <button type="button" disabled={busy || !reasons[request.changeRequestId]?.trim()} onClick={() => decide(request, 'REJECT')}
                className="rounded-lg border border-rose-300 px-4 py-2 text-rose-200 disabled:opacity-40">반려</button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
