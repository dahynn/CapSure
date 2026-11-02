import React, { useEffect, useState } from 'react';
import { getCancerProduct, getCoverageChanges, issueCancerQuote, scheduleCoverageChange, createNextPremium } from './api/cancerInsurance.api';

const won = (value) => `${new Intl.NumberFormat('ko-KR').format(Number(value || 0))}원`;
const date = (value) => new Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul', year: 'numeric', month: 'long', day: 'numeric' }).format(new Date(value));

export default function CoverageChangePanel({ policy }) {
    const [product, setProduct] = useState(null);
    const [selected, setSelected] = useState([]);
    const [changes, setChanges] = useState([]);
    const [preview, setPreview] = useState(null);
    const [agreed, setAgreed] = useState(false);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [premium, setPremium] = useState(null);
    const [loading, setLoading] = useState(true);
    const version = policy.policyVersion;
    useEffect(() => {
        let active = true;
        setLoading(true);
        setError('');
        Promise.all([getCancerProduct(version.productVersionId), getCoverageChanges(policy.policyId)])
            .then(([detail, history]) => {
                if (!active) return;
                setProduct(detail); setChanges(history);
                setSelected(version.coverages.map((coverage) => coverage.productCoverageId));
            }).catch((e) => { if (active) setError(e.message); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [policy.policyId, version.policyVersionId, version.productVersionId]);
    const pending = changes.find((change) => change.status === 'SCHEDULED');
    const unchanged = selected.length === version.coverages.length && version.coverages.every((coverage) => selected.includes(coverage.productCoverageId));
    const total = (product?.coverages || []).filter((coverage) => selected.includes(coverage.productCoverageId))
        .reduce((sum, coverage) => sum + Number(coverage.monthlyPremium || 0), 0);
    const run = async (action) => {
        setBusy(true); setError('');
        try { await action(); } catch (e) { setError(e.message || '변경 요청을 처리하지 못했습니다.'); }
        finally { setBusy(false); }
    };
    const toggle = (id) => {
        setSelected((old) => old.includes(id) ? old.filter((value) => value !== id) : [...old, id]);
        setPreview(null); setAgreed(false);
    };
    return (
        <section className="mt-6 rounded-2xl border border-slate-800 bg-[#09111F] p-5 text-white">
            <h2 className="text-lg font-bold">다음 달 보장 변경</h2>
            <p className="mt-2 text-sm text-slate-400">변경은 다음 달 1일 0시부터 적용됩니다. 그전 사고는 기존 보장으로 확인하며, 새로 추가한 보장에는 면책기간이 적용됩니다. 실제 보험 상품과 구분한 모의 보험료입니다.</p>
            {loading && <p className="mt-4 text-sm" role="status">변경 가능한 보장을 불러오고 있습니다.</p>}
            {error && <p role="alert" className="mt-3 text-sm text-rose-300">{error}</p>}
            {pending ? (
                <div className="mt-4 rounded-xl bg-slate-800 p-4" role="status">
                    <p>{date(pending.effectiveAt)} 변경 예정 · 월 {won(pending.quote.monthlyPremium)}</p>
                    <p className="mt-2 text-sm text-slate-300">{pending.quote.coverages.map((coverage) => coverage.coverageName).join(', ')}</p>
                </div>
            ) : !loading && product && policy.status === 'ACTIVE' && !premium && (
                <fieldset disabled={busy} className="mt-4 space-y-3">
                    <legend className="sr-only">다음 달에 유지할 보장</legend>
                    {product.coverages.map((coverage) => (
                        <label key={coverage.productCoverageId} className="flex items-center gap-3 rounded-xl bg-slate-900 p-3 text-sm">
                            <input type="checkbox" checked={selected.includes(coverage.productCoverageId)} onChange={() => toggle(coverage.productCoverageId)} />
                            <span className="flex-1">{coverage.coverageName}</span><span>월 {won(coverage.monthlyPremium)}</span>
                        </label>
                    ))}
                    <p className="text-sm">선택 보장 예상 월 보험료: {won(total)}</p>
                    <button type="button" disabled={!selected.length || unchanged || busy} className="rounded-xl bg-sky-300 px-4 py-3 font-bold text-slate-950 disabled:opacity-40"
                        onClick={() => run(async () => {
                            const quote = await issueCancerQuote(version.productVersionId, selected);
                            setPreview({ quote, key: crypto.randomUUID() }); setAgreed(false);
                        })}>변경 견적 확인</button>
                    {preview && (
                        <div className="space-y-3 rounded-xl border border-sky-300/30 p-4">
                            <p>확정 견적 월 {won(preview.quote.monthlyPremium)}</p>
                            <label className="flex items-start gap-2 text-sm"><input type="checkbox" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />보장 내용과 약관, 다음 달 적용일 및 추가 보장의 면책기간을 확인했습니다.</label>
                            <button type="button" disabled={!agreed || busy} className="rounded-xl bg-sky-300 px-4 py-3 font-bold text-slate-950 disabled:opacity-40"
                                onClick={() => run(async () => {
                                    await scheduleCoverageChange(policy.policyId, preview.quote, preview.key);
                                    setChanges(await getCoverageChanges(policy.policyId)); setPreview(null); setAgreed(false);
                                })}>다음 달부터 변경 예약</button>
                        </div>
                    )}
                </fieldset>
            )}
            {!loading && policy.status === 'ACTIVE' && (
                <div className="mt-5 border-t border-slate-800 pt-4">
                    <p className="mb-3 text-xs text-slate-400">보장 선택을 마친 뒤 다음 달 납부 내역을 생성할 수 있습니다. 생성 후에는 해당 월의 보장을 변경할 수 없습니다.</p>
                    <button type="button" disabled={busy || !!premium} className="rounded-xl border border-slate-600 px-4 py-2 text-sm disabled:opacity-40"
                        onClick={() => run(async () => setPremium(await createNextPremium(policy.policyId)))}>다음 달 납부 내역 생성</button>
                    {premium && <p className="mt-3 text-sm" role="status">다음 달 청구 보험료 {won(premium.amountDue)} · 보장 변경 시점의 견적을 반영했습니다.</p>}
                </div>
            )}
        </section>
    );
}
