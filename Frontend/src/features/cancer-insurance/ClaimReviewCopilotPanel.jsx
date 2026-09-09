import React, { useCallback, useEffect, useState } from 'react';
import { CheckCircle2, FileSearch, Loader2, RefreshCw, ShieldCheck, XCircle } from 'lucide-react';
import { getClaimCopilotDraft, getClaimCopilotReviewQueue, updateClaimCopilotReview } from './api/operations.api';

const formatDateTime = (value) => {
  if (!value) return '-';
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value));
};

const ClaimReviewCopilotPanel = ({ onUpdated }) => {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [savingKey, setSavingKey] = useState('');
  const [detailLoadingKey, setDetailLoadingKey] = useState('');
  const [selectedKey, setSelectedKey] = useState('');
  const [drafts, setDrafts] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async (silent = false) => {
    if (silent) setRefreshing(true);
    else setLoading(true);
    setError('');
    try {
      setReviews(await getClaimCopilotReviewQueue('DRAFT', 20));
    } catch (requestError) {
      setError(requestError.message || '심사 보조 대기열을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const review = async (item, status) => {
    const key = `${item.claimId}:${item.requestId}`;
    setSavingKey(key);
    setError('');
    setNotice('');
    try {
      await updateClaimCopilotReview(item.claimId, item.requestId, status);
      setNotice(status === 'CONFIRMED'
        ? '초안 검토 확인을 기록했습니다. 보험금 지급·거절은 별도 심사 절차에서 처리해야 합니다.'
        : '초안을 반려로 기록했습니다. 보험금 청구 상태는 변경되지 않습니다.');
      await load(true);
      onUpdated?.();
    } catch (requestError) {
      setError(requestError.message || '심사 보조 검토 상태를 저장하지 못했습니다.');
    } finally {
      setSavingKey('');
    }
  };

  const toggleDraft = async (item) => {
    const key = `${item.claimId}:${item.requestId}`;
    if (selectedKey === key) {
      setSelectedKey('');
      return;
    }
    setSelectedKey(key);
    if (drafts[key]) return;
    setDetailLoadingKey(key);
    setError('');
    try {
      const draft = await getClaimCopilotDraft(item.claimId, item.requestId);
      setDrafts((current) => ({ ...current, [key]: draft }));
    } catch (requestError) {
      setSelectedKey('');
      setError(requestError.message || '심사 보조 초안을 불러오지 못했습니다.');
    } finally {
      setDetailLoadingKey('');
    }
  };

  return (
    <section className="rounded-[26px] border border-violet-300/20 bg-violet-300/5 p-5">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-violet-300/10 text-violet-200">
            <FileSearch className="h-5 w-5" />
          </span>
          <div>
            <p className="text-xs font-bold text-violet-200">CLAIM REVIEW COPILOT</p>
            <h2 className="mt-1 text-lg font-black text-white">심사 보조 검토 대기열</h2>
            <p className="mt-2 text-xs leading-5 text-slate-400">
              약관·증빙 확인 초안만 검토합니다. 이 화면에서는 보험금 지급·거절을 결정할 수 없습니다.
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => load(true)}
          disabled={refreshing || loading}
          className="rounded-xl border border-violet-200/15 bg-black/10 p-2.5 text-violet-100 disabled:opacity-50"
          aria-label="심사 보조 대기열 새로고침"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {notice && (
        <p className="mt-4 rounded-xl border border-emerald-400/20 bg-emerald-400/10 px-3 py-2 text-xs leading-5 text-emerald-100">
          {notice}
        </p>
      )}
      {error && (
        <p className="mt-4 rounded-xl border border-rose-400/20 bg-rose-400/10 px-3 py-2 text-xs leading-5 text-rose-100">
          {error}
        </p>
      )}

      <div className="mt-4 overflow-hidden rounded-2xl border border-slate-800 bg-[#09111F]">
        {loading ? (
          <div className="flex items-center justify-center gap-2 px-4 py-8 text-xs text-slate-400">
            <Loader2 className="h-4 w-4 animate-spin" /> 대기열을 불러오는 중입니다.
          </div>
        ) : reviews.length === 0 ? (
          <div className="px-4 py-8 text-center text-xs leading-5 text-slate-500">
            현재 담당자 확인이 필요한 심사 보조 초안이 없습니다.
          </div>
        ) : reviews.map((item, index) => {
          const key = `${item.claimId}:${item.requestId}`;
          const saving = savingKey === key;
          const selected = selectedKey === key;
          const draft = drafts[key];
          return (
            <article key={key} className={`p-4 ${index ? 'border-t border-slate-800' : ''}`}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-xs font-black text-white">청구 #{item.claimId}</p>
                  <p className="mt-1 truncate text-[10px] text-slate-500">검토 요청 {item.requestId}</p>
                  <p className="mt-2 text-[11px] text-slate-400">초안 생성 {formatDateTime(item.updatedAt)}</p>
                </div>
                <span className="shrink-0 rounded-full bg-amber-300/10 px-2.5 py-1 text-[10px] font-black text-amber-100">
                  검토 대기
                </span>
              </div>
              <button
                type="button"
                onClick={() => toggleDraft(item)}
                disabled={saving || detailLoadingKey === key}
                className="mt-4 flex w-full items-center justify-center gap-1.5 rounded-xl border border-violet-200/20 bg-violet-300/10 px-3 py-2.5 text-xs font-black text-violet-100 disabled:opacity-50"
              >
                {detailLoadingKey === key ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <FileSearch className="h-3.5 w-3.5" />}
                {selected ? '초안 상세 닫기' : '초안 상세 확인'}
              </button>
              {selected && draft && (
                <div className="mt-3 rounded-xl border border-slate-800 bg-slate-950/40 p-3 text-[11px] leading-5 text-slate-300">
                  <p className="font-black text-violet-100">확인할 약관 근거</p>
                  <ul className="mt-1 space-y-1 text-slate-400">
                    {draft.termsToCheck.length === 0 ? <li>연결된 약관 조항을 먼저 확인해야 합니다.</li> : draft.termsToCheck.map((source) => (
                      <li key={`${source.sourceType}:${source.sourceId}`}>{source.sourceType} · {source.sourceId}</li>
                    ))}
                  </ul>
                  {draft.possibleMissingEvidence.length > 0 && (
                    <p className="mt-3 text-amber-100">누락 확인: {draft.possibleMissingEvidence.join(', ')}</p>
                  )}
                  {draft.additionalQuestions.length > 0 && (
                    <ul className="mt-3 space-y-1 text-slate-400">
                      {draft.additionalQuestions.map((question) => <li key={question}>• {question}</li>)}
                    </ul>
                  )}
                  <p className="mt-3 text-[10px] text-slate-500">이 초안은 보험금 지급·거절 결정을 대신하지 않습니다.</p>
                </div>
              )}
              {selected && draft && (
              <div className="mt-3 grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => review(item, 'CONFIRMED')}
                  disabled={saving}
                  className="flex items-center justify-center gap-1.5 rounded-xl bg-emerald-400/15 px-3 py-2.5 text-xs font-black text-emerald-100 disabled:opacity-50"
                >
                  {saving ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <CheckCircle2 className="h-3.5 w-3.5" />}
                  검토 확인
                </button>
                <button
                  type="button"
                  onClick={() => review(item, 'REJECTED')}
                  disabled={saving}
                  className="flex items-center justify-center gap-1.5 rounded-xl bg-rose-400/10 px-3 py-2.5 text-xs font-black text-rose-100 disabled:opacity-50"
                >
                  <XCircle className="h-3.5 w-3.5" /> 초안 반려
                </button>
              </div>
              )}
            </article>
          );
        })}
      </div>
      <p className="mt-3 flex items-start gap-2 text-[10px] leading-4 text-slate-500">
        <ShieldCheck className="mt-0.5 h-3.5 w-3.5 shrink-0 text-violet-200" />
        대기열에는 청구 ID, 요청 ID, 검토 상태와 시각만 표시됩니다.
      </p>
    </section>
  );
};

export default ClaimReviewCopilotPanel;
